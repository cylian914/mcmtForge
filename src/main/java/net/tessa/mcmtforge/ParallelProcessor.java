package net.tessa.mcmtforge;

import net.tessa.mcmtforge.config.BlockEntityLists;
import net.tessa.mcmtforge.config.GeneralConfig;
import net.tessa.mcmtforge.serdes.SerDesHookTypes;
import net.tessa.mcmtforge.serdes.SerDesRegistry;
import net.tessa.mcmtforge.serdes.filter.ISerDesFilter;
import net.tessa.mcmtforge.serdes.pools.PostExecutePool;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
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

    static ConcurrentHashMap<ServerLevel, Phaser> sharedPhasers = new ConcurrentHashMap<>();
    static ExecutorService worldPool;
    static ExecutorService tickPool;
    static MinecraftServer mcs;
    static AtomicBoolean isTicking = new AtomicBoolean();

    public static void setupThreadPool(int parallelism) {
        AtomicInteger worldPoolThreadID = new AtomicInteger();
        AtomicInteger tickPoolThreadID = new AtomicInteger();
        final ClassLoader cl = MCMT.class.getClassLoader();
        ForkJoinPool.ForkJoinWorkerThreadFactory worldThreadFactory = p -> {
            ForkJoinWorkerThread fjwt = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(p);
            fjwt.setName("MCMT-World-Pool-Thread-" + worldPoolThreadID.getAndIncrement());
            regThread("MCMT-World", fjwt);
            fjwt.setContextClassLoader(cl);
            return fjwt;
        };
        ForkJoinPool.ForkJoinWorkerThreadFactory tickThreadFactory = p -> {
            ForkJoinWorkerThread fjwt = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(p);
            fjwt.setName("MCMT-Tick-Pool-Thread-" + tickPoolThreadID.getAndIncrement());
            regThread("MCMT-Tick", fjwt);
            fjwt.setContextClassLoader(cl);
            return fjwt;
        };
        worldPool = new ForkJoinPool(Math.min(3, Math.max(parallelism / 2, 1)), worldThreadFactory, null, true);
        tickPool = new ForkJoinPool(parallelism, tickThreadFactory, null, true);
    }

    /**
     * Creates and sets up the thread pool
     */
    static {
        // Must be static here due to class loading shenanagins
        // setupThreadPool(4);
    }

    static Map<String, Set<Thread>> mcThreadTracker = new ConcurrentHashMap<String, Set<Thread>>();

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
        config = MCMT.config; // Load when config are loaded. Static loads before config update.
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

    public static long[] lastTickTime = new long[32];
    public static int lastTickTimePos = 0;
    public static int lastTickTimeFill = 0;

    public static void postTick(MinecraftServer server) {
        if (!config.disabled && !config.disableWorld) {
            if (mcs != server) {
                LOGGER.warn("Multiple servers?");
                return;
            } else {
               // MCMT.LOGGER.warn("TICKED WP {}", worldPhaser.toString());
                worldPhaser.arriveAndAwaitAdvance();
                //MCMT.LOGGER.warn("ETICKED WP");

                isTicking.set(false);
                worldPhaser = null;
                //PostExecute logic
                Deque<Runnable> queue = PostExecutePool.POOL.getQueue();
                Iterator<Runnable> qi = queue.iterator();
                while (qi.hasNext()) {
                    Runnable r = qi.next();
                    r.run();
                    qi.remove();
                }
                lastTickTime[lastTickTimePos] = System.nanoTime() - tickStart;
                lastTickTimePos = (lastTickTimePos + 1) % lastTickTime.length;
                lastTickTimeFill = Math.min(lastTickTimeFill + 1, lastTickTime.length - 1);
            }
        }
    }

    public static void preChunkTick(ServerLevel world) {
        Phaser phaser; // Keep a party throughout 3 ticking phases
        if (!config.disabled && !config.disableEnvironment) {
            phaser = new Phaser(2);
        } else {
            phaser = new Phaser(1);
        }
        sharedPhasers.put(world, phaser);
    }

    public static void callTickChunks(ServerLevel world, LevelChunk chunk, int k) {
        if (config.disabled || config.disableEnvironment) {
            world.tickChunk(chunk, k);
            return;
        }
        String taskName = null;
        if (config.opsTracing) {
            taskName = "EnvTick: " + chunk.toString() + "@" + chunk.hashCode();
            currentTasks.add(taskName);
        }
        String finalTaskName = taskName;
        sharedPhasers.get(world).register();
        tickPool.execute(() -> {
            try {
                currentEnvs.incrementAndGet();
                world.tickChunk(chunk, k);
            } finally {
                if (config.opsTracing) currentTasks.remove(finalTaskName);
                sharedPhasers.get(world).arriveAndDeregister();
                currentEnvs.decrementAndGet();
            }
        });
    }

    public static void postChunkTick(ServerLevel world) {
        if (!config.disabled && !config.disableEnvironment) {
            var phaser = sharedPhasers.get(world);
            phaser.arriveAndDeregister();
            phaser.arriveAndAwaitAdvance();
        }
    }

    public static void preEntityTick(ServerLevel world) {
        if (!config.disabled && !config.disableEntity) sharedPhasers.get(world).register();
    }

    public static void callEntityTick(Consumer<Entity> tickConsumer, Entity entityIn, ServerLevel serverworld) {
        if (config.disabled || config.disableEntity) {
            tickConsumer.accept(entityIn);
            return;
        }
        if (entityIn instanceof Player || entityIn instanceof FallingBlockEntity ) {
            tickConsumer.accept(entityIn);
            return;
        }
        String taskName = null;
        if (config.opsTracing) {
            taskName = "EntityTick: " + /*entityIn.toString() + KG: Wayyy too slow. Maybe for debug but needs to be done via flag in that circumstance */ "@" + entityIn.hashCode();
            currentTasks.add(taskName);
        }
        String finalTaskName = taskName;
        sharedPhasers.get(serverworld).register();
        tickPool.execute(() -> {
            try {
                final ISerDesFilter filter = SerDesRegistry.getFilter(SerDesHookTypes.EntityTick, entityIn.getClass());
                currentEnts.incrementAndGet();
                if (filter != null) {
                    filter.serialise(() -> tickConsumer.accept(entityIn), entityIn, entityIn.blockPosition(), serverworld, SerDesHookTypes.EntityTick);
                } else {
                    tickConsumer.accept(entityIn);
                }
            } finally {
                if (config.opsTracing) currentTasks.remove(finalTaskName);
                sharedPhasers.get(serverworld).arriveAndDeregister();
                currentEnts.decrementAndGet();
            }
        });
    }

    public static void postEntityTick(ServerLevel world) {
        if (!config.disabled && !config.disableEntity) {
            var phaser = sharedPhasers.get(world);
            phaser.arriveAndDeregister();
            phaser.arriveAndAwaitAdvance();
        }
    }

    public static void preBlockEntityTick(ServerLevel world) {
        if (!config.disabled && !config.disableTileEntity) sharedPhasers.get(world).register();
    }

    public static void callBlockEntityTick(TickingBlockEntity tte, Level world) {
        if ((world instanceof ServerLevel) && tte instanceof LevelChunk.RebindableTickingBlockEntityWrapper && (((LevelChunk.RebindableTickingBlockEntityWrapper) tte).ticker instanceof LevelChunk.BoundTickingBlockEntity<?>)) {
            if (config.disabled || config.disableTileEntity) {
                tte.tick();
                return;
            }
            if (((LevelChunk.BoundTickingBlockEntity<?>) ((LevelChunk.RebindableTickingBlockEntityWrapper) tte).ticker).blockEntity instanceof PistonMovingBlockEntity) {
                tte.tick();
                return;
            }
            String taskName = null;
            if (config.opsTracing) {
                taskName = "TETick: " + tte.toString() + "@" + tte.hashCode();
                currentTasks.add(taskName);
            }
            String finalTaskName = taskName;
            sharedPhasers.get(world).register();
            tickPool.execute(() -> {
                try {
                    final ISerDesFilter filter = SerDesRegistry.getFilter(SerDesHookTypes.TETick, ((LevelChunk.RebindableTickingBlockEntityWrapper) tte).ticker.getClass());
                    currentTEs.incrementAndGet();
                    if (filter != null) filter.serialise(tte::tick, tte, tte.getPos(), world, SerDesHookTypes.TETick);
                    else tte.tick();
                } catch (Exception e) {
                    System.err.println("Exception ticking TE at " + tte.getPos());
                    e.printStackTrace();
                } finally {
                    if (config.opsTracing) currentTasks.remove(finalTaskName);
                    sharedPhasers.get(world).arriveAndDeregister();
                    currentTEs.decrementAndGet();
                }
            });
        } else tte.tick();
    }

    public static boolean filterTE(TickingBlockEntity tte) {
        boolean isLocking = false;
        if (BlockEntityLists.teBlackList.contains(tte.getClass())) {
            isLocking = true;
        }
        // Apparently a string starts with check is faster than Class.getPackage; who knew (I didn't)
        if (!isLocking && config.chunkLockModded && !tte.getClass().getName().startsWith("net.minecraft.block.entity.")) {
            isLocking = true;
        }
        if (isLocking && BlockEntityLists.teWhiteList.contains(tte.getClass())) {
            isLocking = false;
        }
        if (tte instanceof PistonMovingBlockEntity) {
            isLocking = true;
        }
        return isLocking;
    }

    public static void postBlockEntityTick(ServerLevel world) {
        if (!config.disabled && !config.disableTileEntity) {
            var phaser = sharedPhasers.get(world);
            phaser.arriveAndDeregister();
            phaser.arriveAndAwaitAdvance();
        }
    }

    public static boolean shouldThreadChunks() {
        return !MCMT.config.disableMultiChunk;
    }

}
