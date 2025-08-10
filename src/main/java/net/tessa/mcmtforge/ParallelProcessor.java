package net.tessa.mcmtforge;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.*;
import net.tessa.mcmtforge.config.GeneralConfig;
import net.tessa.mcmtforge.debug.WorldTickStats;
import net.tessa.mcmtforge.serdes.pools.PostExecutePool;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.tessa.mcmtforge.parallelised.BotRegionManager;
import net.tessa.mcmtforge.parallelised.threads.GlobalAffinityThreadPool;
import net.tessa.mcmtforge.parallelised.threads.ThreadedChunksRegion;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class ParallelProcessor {

    private static final Logger LOGGER = LogManager.getLogger();

    static Phaser worldPhaser;

    static ExecutorService worldPool;
    static MinecraftServer mcs;
    static AtomicBoolean isTicking = new AtomicBoolean();

    private static final ConcurrentHashMap<ServerLevel, List<Runnable>> delayedChunkTasks = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<ServerLevel, List<Runnable>> delayedEntityTasks = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<ServerLevel, List<Runnable>> delayedBlockEntityTasks = new ConcurrentHashMap<>();

    static Map<String, Set<Thread>> mcThreadTracker = new ConcurrentHashMap<String, Set<Thread>>();

    // List of ThreadedChunksRegion
    public static final List<ThreadedChunksRegion> threadedChunksRegions = new ArrayList<>();

    private static final Set<ThreadedChunksRegion> pendingRegionsToAdd = ConcurrentHashMap.newKeySet();

    private static final Set<ThreadedChunksRegion> pendingRegionsToRemove = ConcurrentHashMap.newKeySet();


    public static final ConcurrentHashMap<ServerLevel, WorldTickStats> worldTickStats = new ConcurrentHashMap<>();

    private static final Cache<ChunkPos, ThreadedChunksRegion> chunkRegionCache = Caffeine.newBuilder()
            .initialCapacity(10000)
            .maximumSize(10000)
            .build();

    public static void resetThreadedChunksRegions() {
        synchronized (threadedChunksRegions) {
            for (ThreadedChunksRegion region : threadedChunksRegions) {
                region.shutdownExecutors();
            }
            threadedChunksRegions.clear();
            chunkRegionCache.invalidateAll(); // Invalidate cache when regions are reset
        }
    }

    public static void addThreadedChunksRegion(ThreadedChunksRegion region) {
        synchronized (pendingRegionsToAdd) {
            pendingRegionsToAdd.add(region);
            region.getSingleThreadExecutor(); // Force load executor once to avoid blocking when there is no chunk tick
        }
    }


    public static void removeThreadedChunksRegion(ThreadedChunksRegion region) {
        synchronized (threadedChunksRegions) {
            pendingRegionsToRemove.add(region);
        }
    }

    public static void removeThreadedChunksRegionByName(String name) {
        synchronized (threadedChunksRegions) {
            ThreadedChunksRegion region = null;
            for (ThreadedChunksRegion r : threadedChunksRegions) {
                if (r.getName().equals(name)) {
                    region = r;
                    break;
                }
            }
            if (region != null) {
                removeThreadedChunksRegion(region);
            }
        }
    }

    private static ThreadedChunksRegion findMatchingRegion(int chunkX, int chunkZ, Level world) {
        ChunkPos pos = new ChunkPos(chunkX, chunkZ);
        return chunkRegionCache.get(pos, key -> {
            synchronized (threadedChunksRegions) {
                String worldId = world.dimension().location().toString();

                // Single stream operation to find the smallest matching region
                // with preference for non-bot regions
                return threadedChunksRegions.stream()
                        .filter(region -> region.contains(worldId, chunkX, chunkZ))
                        .min((r1, r2) -> {
                            // First, compare based on bot region prefix
                            boolean isBot1 = r1.getName().startsWith("bot_region_");
                            boolean isBot2 = r2.getName().startsWith("bot_region_");

                            if (isBot1 != isBot2) {
                                // If one is a bot region and the other isn't,
                                // prefer the non-bot region
                                return isBot1 ? 1 : -1;
                            }

                            // If both are bot regions or both are not,
                            // compare by area as before
                            long area1 = r1.getArea();
                            long area2 = r2.getArea();
                            return Long.compare(area1, area2);
                        })
                        .orElse(null);
            }
        });
    }

    public static void setupThreadPool(int parallelism) {
        GlobalAffinityThreadPool.getAffinitySharedPool();

        worldPool = GlobalAffinityThreadPool.getAffinityWorldAndRegionPool();
        for (int i = 0; i < 2; i++)
            GlobalAffinityThreadPool.increaseWorldAndRegionPoolSize();
    }

    // Statistics
    public static AtomicInteger currentWorlds = new AtomicInteger();
    public static AtomicInteger currentEnts = new AtomicInteger();
    public static AtomicInteger currentTEs = new AtomicInteger();
    public static AtomicInteger currentEnvs = new AtomicInteger();

    //Operation logging
    public static Set<String> currentTasks = ConcurrentHashMap.newKeySet();

    public static void regThread(String poolName, Thread thread) {
        mcThreadTracker.computeIfAbsent(poolName, s -> ConcurrentHashMap.newKeySet()).add(thread);
    }

    public static boolean isThreadPooled(String poolName, Thread t) {
        return mcThreadTracker.containsKey(poolName) && mcThreadTracker.get(poolName).contains(t);
    }

    public static boolean serverExecutionThreadPatch(MinecraftServer ms) {
        return isThreadPooled("MCMT-World", Thread.currentThread()) || isThreadPooled("MCMT-Tick", Thread.currentThread());
    }

    static long tickStart = 0;
    static GeneralConfig config;

    public static void preTick(int size, MinecraftServer server) {
        config = MCMT.config;
        if (!config.disabled && !config.disableWorld) {
            if (worldPhaser != null) {
                LOGGER.warn("Multiple servers?");
                return;
            } else {
                tickStart = System.nanoTime();
                isTicking.set(true);
                worldPhaser = new Phaser(size + 1);
                mcs = server;
            }
        }
        // Process pending regions
        synchronized (pendingRegionsToAdd) {
            if (!pendingRegionsToAdd.isEmpty()) {
                synchronized (threadedChunksRegions) {
                    for (ThreadedChunksRegion region : pendingRegionsToAdd) {
                        threadedChunksRegions.add(region);
                        GlobalAffinityThreadPool.increaseWorldAndRegionPoolSize();
                    }
                    chunkRegionCache.invalidateAll();
                    pendingRegionsToAdd.clear();
                }
            }
        }
        synchronized (pendingRegionsToRemove) {
            if (!pendingRegionsToRemove.isEmpty()) {
                synchronized (pendingRegionsToRemove) {
                    for (ThreadedChunksRegion region : pendingRegionsToRemove) {
                        threadedChunksRegions.remove(region);
                        region.shutdownExecutors();
                        GlobalAffinityThreadPool.decreaseWorldAndRegionPoolSize();
                    }
                    chunkRegionCache.invalidateAll();
                    pendingRegionsToRemove.clear();
                }
            }
        }
    }

    public static void callTick(ServerLevel serverworld, BooleanSupplier hasTimeLeft, MinecraftServer server) {
        if (config.disabled || config.disableWorld) {
            try {
                serverworld.tick(hasTimeLeft);
            } catch (Exception e) {
                throw e;
            }
            return;
        }
        if (mcs != server) {
            LOGGER.warn("Multiple servers?");
            config.disabled = true;
            serverworld.tick(hasTimeLeft);
            return;
        } else {
            String taskName = null;
            if (config.opsTracing) {
                taskName = "WorldTick: " + serverworld.toString() + "@" + serverworld.hashCode();
                currentTasks.add(taskName);
            }
            String finalTaskName = taskName;
            worldPool.execute(() -> {
                try {
                    currentWorlds.incrementAndGet();
                    serverworld.tick(hasTimeLeft);
                } finally {
                    worldPhaser.arriveAndDeregister();
                    currentWorlds.decrementAndGet();
                    if (config.opsTracing) currentTasks.remove(finalTaskName);
                }
            });
        }
    }

    public static void postTick(MinecraftServer server) {
        if (!config.disabled && !config.disableWorld) {
            if (mcs != server) {
                LOGGER.warn("Multiple servers?");
                return;
            } else {
                worldPhaser.arriveAndAwaitAdvance();

                isTicking.set(false);
                worldPhaser = null;
                // PostExecute logic
                Deque<Runnable> queue = PostExecutePool.POOL.getQueue();
                Iterator<Runnable> qi = queue.iterator();
                while (qi.hasNext()) {
                    Runnable r = qi.next();
                    r.run();
                    qi.remove();
                }
            }
        }

        synchronized (threadedChunksRegions) {
            for (ThreadedChunksRegion region : threadedChunksRegions) {
                region.swapExecutionTimeBuffers();
            }
        }

        synchronized (worldTickStats) {
            for (WorldTickStats stats : worldTickStats.values()) {
                stats.swapExecutionTimeBuffers();
            }
        }
    }


    public static void preChunkTick(ServerLevel world) {
        if (config.disabled || config.disableEnvironment) {
            return;
        }
        synchronized (threadedChunksRegions) {
            for (ThreadedChunksRegion region : threadedChunksRegions) {
                if (region.getWorldId().equals(world.dimension().location().toString())) {
                    // Register the main thread in the region's chunkTickPhaser
//                    region.getChunkTickPhaser().register();
                }
            }
        }
    }


    public static void callTickChunks(ServerLevel world, LevelChunk chunk, int k) {
        int chunkX = chunk.getPos().x;
        int chunkZ = chunk.getPos().z;

        ThreadedChunksRegion matchingRegion = findMatchingRegion(chunkX, chunkZ, world);
        if (matchingRegion == null) {
            // No matching region, delay processing
            delayedChunkTasks.computeIfAbsent(world, w -> new ArrayList<>())
                    .add(() -> world.tickChunk(chunk, k));
            return;
        }

        Executor executor = matchingRegion.getChunkTickExecutor();

        String taskName;
        if (config.opsTracing) {
            taskName = "ChunkTick: " + chunk + "@" + chunk.hashCode();
            matchingRegion.currentTasks.add(taskName);
        } else {
            taskName = "";
        }


        matchingRegion.getChunkTickPhaser().register();

        executor.execute(() -> {
            try {
                matchingRegion.recordChunkStageStart();

                long startTime = System.nanoTime(); // Start timing
                world.tickChunk(chunk, k);
                long endTime = System.nanoTime(); // End timing
                long duration = endTime - startTime;
                matchingRegion.addChunkTickTime(duration); // Store execution time
            } finally {
                matchingRegion.getChunkTickPhaser().arrive();
                if (config.opsTracing) {
                    if (matchingRegion.currentTasks.stream().anyMatch(task -> (task.contains("Entity"))))
                        LOGGER.debug("Mixed tasks detected");
                    matchingRegion.currentTasks.remove(taskName);
                }
            }
        });
    }

    public static void postChunkTick(ServerLevel world) {
        synchronized (threadedChunksRegions) {
            for (ThreadedChunksRegion region : threadedChunksRegions) {
                // Region's post-chunk-tick handler
                if (region.getWorldId().equals(world.dimension().location().toString())) {
                    GlobalAffinityThreadPool.getAffinitySharedPool().execute(region::postChunkTick);
                }
            }
        }

        // Process delayed chunk tasks
        List<Runnable> tasks = delayedChunkTasks.remove(world);
        if (tasks != null) {
            WorldTickStats stats = worldTickStats.computeIfAbsent(world, w -> new WorldTickStats());
            for (Runnable task : tasks) {
                long startTime = System.nanoTime();
                task.run();
                long endTime = System.nanoTime();
                stats.chunkTickTimesCurrent.add(endTime - startTime);
            }
        }
    }


    public static void preEntityTick(ServerLevel world) {
        if (config.disabled || config.disableEntity) {
            return;
        }
        synchronized (threadedChunksRegions) {
            for (ThreadedChunksRegion region : threadedChunksRegions) {
                if (region.getWorldId().equals(world.dimension().location().toString())) {
                    // Register the main thread in the region's entityTickPhaser
//                    region.getEntityTickPhaser().register();
                }
            }
        }
    }


    public static void callEntityTick(Consumer<Entity> tickConsumer, Entity entityIn, ServerLevel serverworld) {
        if (config.disabled || config.disableEntity) {
            tickConsumer.accept(entityIn);
            return;
        }

        if (shouldTickPortalSynchronously(entityIn)) {
            tickConsumer.accept(entityIn);
            return;
        }

        int chunkX = entityIn.chunkPosition().x;
        int chunkZ = entityIn.chunkPosition().z;

        ThreadedChunksRegion matchingRegion = findMatchingRegion(chunkX, chunkZ, serverworld);
        if (matchingRegion == null) {
            // No matching region, delay processing
            delayedEntityTasks.computeIfAbsent(serverworld, w -> new ArrayList<>())
                    .add(() -> {
                        if (entityIn instanceof ServerPlayer player) {
                            BotRegionManager.checkAndManageBot(player);
                        }
                        tickConsumer.accept(entityIn);
                    });
            return;
        }

        Executor executor = shouldUseSingleThread(entityIn) ?
                matchingRegion.getSingleThreadExecutor() :
                matchingRegion.getEntityTickExecutor();


        matchingRegion.getEntityTickPhaser().register();
        executor.execute(() -> {
            String taskName = null;
            try {
                // Wait for chunk tick stage to complete in this region
                matchingRegion.getChunkTickPhaser().awaitAdvance(0);

                if (config.opsTracing) {
                    taskName = "EntityTick: " + entityIn;
                    matchingRegion.currentTasks.add(taskName);
                } else {
                    taskName = "";
                }

                matchingRegion.recordEntityStageStart();

                long startTime = System.nanoTime();

                if (entityIn instanceof ServerPlayer player) {
                    BotRegionManager.checkAndManageBot(player);
                }

                tickConsumer.accept(entityIn);

                long endTime = System.nanoTime();
                long duration = endTime - startTime;
                matchingRegion.addEntityTickTime(duration);
            } finally {
                matchingRegion.getEntityTickPhaser().arrive();
                if (config.opsTracing) {
                    if (matchingRegion.currentTasks.stream().anyMatch(task -> (task.startsWith("Chunk") || task.startsWith("Block"))))
                        LOGGER.debug("Mixed tasks detected");
                    matchingRegion.currentTasks.remove(taskName);
                }
            }
        });
    }


    private static boolean shouldUseSingleThread(Entity entity) {
        return entity instanceof FallingBlockEntity ||
                entity instanceof Allay ||
                entity instanceof PrimedTnt;
    }

    private static boolean shouldTickPortalSynchronously(Entity entity) {
        if (entity.isInsidePortal) {
            return true;
        }
        return entity instanceof Projectile;
    }

    public static void postEntityTick(ServerLevel world) {
        synchronized (threadedChunksRegions) {
            for (ThreadedChunksRegion region : threadedChunksRegions) {
                if (region.getWorldId().equals(world.dimension().location().toString())) {
                    GlobalAffinityThreadPool.getAffinitySharedPool().execute(region::postEntityTick);

                }
            }
        }

        // Process delayed entity tasks
        List<Runnable> tasks = delayedEntityTasks.remove(world);
        if (tasks != null) {
            WorldTickStats stats = worldTickStats.computeIfAbsent(world, w -> new WorldTickStats());
            for (Runnable task : tasks) {
                long startTime = System.nanoTime();
                task.run();
                long endTime = System.nanoTime();
                stats.entityTickTimesCurrent.add(endTime - startTime);
            }
        }

    }

    public static void preBlockEntityTick(ServerLevel world) {
        if (config.disabled || config.disableBlockEntity) {
            return;
        }
        synchronized (threadedChunksRegions) {
            for (ThreadedChunksRegion region : threadedChunksRegions) {
                if (region.getWorldId().equals(world.dimension().location().toString())) {
//                    region.getBlockEntityTickPhaser().register();
                }
            }
        }
    }

    public static void callBlockEntityTick(TickingBlockEntity tte, Level world) {
        if (!(world instanceof ServerLevel) || !(tte instanceof LevelChunk.RebindableTickingBlockEntityWrapper wrappedInvoker)) {
            tte.tick();
            return;
        }

        if (!(wrappedInvoker.ticker instanceof LevelChunk.BoundTickingBlockEntity<?>)) {
            tte.tick();
            return;
        }

        if (config.disabled || config.disableBlockEntity) {
            tte.tick();
            return;
        }

        BlockEntity blockEntity = ((LevelChunk.BoundTickingBlockEntity<?>) wrappedInvoker.ticker).blockEntity;
        int chunkX = blockEntity.getBlockPos().getX() >> 4;
        int chunkZ = blockEntity.getBlockPos().getZ() >> 4;

        ThreadedChunksRegion matchingRegion = findMatchingRegion(chunkX, chunkZ, world);
        if (matchingRegion == null) {
            // No matching region, delay processing
            delayedBlockEntityTasks.computeIfAbsent((ServerLevel) world, w -> new ArrayList<>())
                    .add(tte::tick);
            return;
        }

        Executor executor = shouldUseSingleThread(blockEntity) ?
                matchingRegion.getSingleThreadExecutor() :
                matchingRegion.getBlockEntityTickExecutor();


        matchingRegion.getBlockEntityTickPhaser().register();
        executor.execute(() -> {
            String taskName = null;
            try {
                // Wait for entity tick stage to complete in this region
                matchingRegion.getEntityTickPhaser().awaitAdvance(0);

                if (config.opsTracing) {
                    taskName = "BlockEntityTick: " + tte + "@" + tte.hashCode();
                    matchingRegion.currentTasks.add(taskName);
                } else {
                    taskName = "";
                }

                matchingRegion.recordBlockEntityStageStart();

                long startTime = System.nanoTime();
                tte.tick();
                long endTime = System.nanoTime();
                long duration = endTime - startTime;
                matchingRegion.addBlockEntityTickTime(duration);
            } finally {
                matchingRegion.getBlockEntityTickPhaser().arrive();
                if (config.opsTracing) {
                    if (matchingRegion.currentTasks.stream().anyMatch(task -> (task.startsWith("Chunk") || task.startsWith("EntityTick"))))
                        LOGGER.debug("Mixed tasks detected");
                    matchingRegion.currentTasks.remove(taskName);
                }
            }
        });
    }

    private static boolean shouldUseSingleThread(BlockEntity blockEntity) {
        return blockEntity instanceof PistonMovingBlockEntity ||
                blockEntity instanceof SculkSensorBlockEntity ||
                blockEntity instanceof SculkShriekerBlockEntity ||
                blockEntity instanceof SculkCatalystBlockEntity;
    }

    public static void postBlockEntityTick(ServerLevel world) {
        synchronized (threadedChunksRegions) {
            for (ThreadedChunksRegion region : threadedChunksRegions) {
                if (region.getWorldId().equals(world.dimension().location().toString())) {
                    GlobalAffinityThreadPool.getAffinitySharedPool().execute(region::postBlockEntityTick);
                }
            }
        }

        // Process delayed block entity tasks
        List<Runnable> tasks = delayedBlockEntityTasks.remove(world);
        if (tasks != null) {
            WorldTickStats stats = worldTickStats.computeIfAbsent(world, w -> new WorldTickStats());
            for (Runnable task : tasks) {
                long startTime = System.nanoTime();
                task.run();
                long endTime = System.nanoTime();
                stats.blockEntityTickTimesCurrent.add(endTime - startTime);
            }
        }

        synchronized (threadedChunksRegions) {
            for (ThreadedChunksRegion region : threadedChunksRegions) {
                if (region.getWorldId().equals(world.dimension().location().toString())) {
                    region.getBlockEntityTickPhaser().awaitAdvance(0);
                    region.initializePhaser();
                }
            }
        }

    }

    public static boolean shouldThreadChunks() {
        return !MCMT.config.disableMultiChunk;
    }

}
