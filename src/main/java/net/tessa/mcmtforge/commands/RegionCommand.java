package net.tessa.mcmtforge.commands;


import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.tessa.mcmtforge.ParallelProcessor;
import net.tessa.mcmtforge.config.ThreadedRegionsConfig;
import net.tessa.mcmtforge.debug.WorldTickStats;
import net.tessa.mcmtforge.parallelised.threads.ThreadedChunksRegion;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

import static net.minecraft.commands.Commands.argument;

import static net.minecraft.commands.Commands.literal;

public class RegionCommand {
    private static final DynamicCommandExceptionType INVALID_WORLD_EXCEPTION = new DynamicCommandExceptionType(
            (id) -> Component.literal("Invalid world: " + id));

    public static LiteralArgumentBuilder<CommandSourceStack> registerRegion(LiteralArgumentBuilder<CommandSourceStack> root) {
        return root.requires(cmdSrc -> cmdSrc.hasPermission(0))
                .then(literal("add")
                        .then(literal("radius")
                                .then(argument("x", IntegerArgumentType.integer())
                                        .suggests((context, builder) -> suggestChunkCoordinate(context.getSource(), builder, true))
                                        .then(argument("z", IntegerArgumentType.integer())
                                                .suggests((context, builder) -> suggestChunkCoordinate(context.getSource(), builder, false))
                                                .then(argument("radius", IntegerArgumentType.integer(1))
                                                        .then(literal("in")
                                                                .then(argument("world", ResourceKeyArgument.key(Registries.DIMENSION))
                                                                        .executes(cmdCtx -> executeRadiusAdd(cmdCtx, null))
                                                                        .then(literal("named")
                                                                                .then(argument("name", StringArgumentType.word())
                                                                                        .executes(cmdCtx -> executeRadiusAdd(cmdCtx,
                                                                                                StringArgumentType.getString(cmdCtx, "name")))))))))))
                        .then(literal("chunks")
                                .then(literal("from")
                                        .then(argument("x1", IntegerArgumentType.integer())
                                                .suggests((context, builder) -> suggestChunkCoordinate(context.getSource(), builder, true))
                                                .then(argument("z1", IntegerArgumentType.integer())
                                                        .suggests((context, builder) -> suggestChunkCoordinate(context.getSource(), builder, false))
                                                        .then(literal("to")
                                                                .then(argument("x2", IntegerArgumentType.integer())
                                                                        .suggests((context, builder) -> suggestChunkCoordinate(context.getSource(), builder, true))
                                                                        .then(argument("z2", IntegerArgumentType.integer())
                                                                                .suggests((context, builder) -> suggestChunkCoordinate(context.getSource(), builder, false))
                                                                                .then(literal("in")
                                                                                        .then(argument("world", ResourceKeyArgument.key(Registries.DIMENSION))
                                                                                                .executes(cmdCtx -> executeExplicitAdd(cmdCtx, null))
                                                                                                .then(literal("named")
                                                                                                        .then(argument("name", StringArgumentType.word())
                                                                                                                .executes(cmdCtx -> executeExplicitAdd(cmdCtx,
                                                                                                                        StringArgumentType.getString(cmdCtx, "name")))))))))))))))
                .then(literal("set")
                        .then(argument("name", StringArgumentType.word())
                                .suggests((context, builder) -> suggestRegionNames(context.getSource(), builder))
                                .then(literal("chunkTick")
                                        .then(argument("enabled", BoolArgumentType.bool())
                                                .executes(cmdCtx -> executeSetTick(cmdCtx, "chunkTick"))))
                                .then(literal("entityTick")
                                        .then(argument("enabled", BoolArgumentType.bool())
                                                .executes(cmdCtx -> executeSetTick(cmdCtx, "entityTick"))))
                                .then(literal("blockEntityTick")
                                        .then(argument("enabled", BoolArgumentType.bool())
                                                .executes(cmdCtx -> executeSetTick(cmdCtx, "blockEntityTick"))))))
                .then(literal("remove")
                        .then(argument("name", StringArgumentType.word())
                                .suggests((context, builder) -> suggestRegionNames(context.getSource(), builder))
                                .executes(RegionCommand::executeRemove)))
                .then(literal("list")
                        .executes(RegionCommand::executeListCompact))
                .then(literal("show")
                        .then(argument("name", StringArgumentType.word())
                                .suggests((context, builder) -> suggestRegionNames(context.getSource(), builder))
                                .executes(RegionCommand::executeShow)))
                .then(literal("stats")
                        .executes(cmdCtx -> {
                            dumpRegionStats(cmdCtx.getSource());
                            return 1;
                        })
                );
    }

    /**
     * Checks if a region with the same coordinates already exists
     */
    private static boolean isRegionOverlapping(ThreadedChunksRegion newRegion, ThreadedRegionsConfig config) {
        if (config.threadedChunksRegions == null) return false;

        for (ThreadedChunksRegion existingRegion : config.threadedChunksRegions) {
            if (existingRegion.getWorldId().equals(newRegion.getWorldId()) &&
                    existingRegion.getX1() == newRegion.getX1() &&
                    existingRegion.getZ1() == newRegion.getZ1() &&
                    existingRegion.getX2() == newRegion.getX2() &&
                    existingRegion.getZ2() == newRegion.getZ2()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a region with the same name already exists
     */
    private static boolean doesRegionNameExist(String name, ThreadedRegionsConfig config) {
        if (config.threadedChunksRegions == null) return false;

        return config.threadedChunksRegions.stream()
                .anyMatch(region -> region.getName().equals(name));
    }

    private static void saveRegionToConfig(ThreadedChunksRegion region) {
        ThreadedRegionsConfig regionsConfig = AutoConfig.getConfigHolder(ThreadedRegionsConfig.class).getConfig();

        // Initialize the list if null
        if (regionsConfig.threadedChunksRegions == null) {
            regionsConfig.threadedChunksRegions = new ArrayList<>();
        }

        // Check for duplicates
        if (doesRegionNameExist(region.getName(), regionsConfig)) {
            throw new IllegalStateException("A region with the name '" + region.getName() + "' already exists");
        }

        if (isRegionOverlapping(region, regionsConfig)) {
            throw new IllegalStateException(
                    String.format("A region with the same coordinates (%d,%d) to (%d,%d) already exists",
                            region.getX1(), region.getZ1(), region.getX2(), region.getZ2())
            );
        }

        // Add the new region
        regionsConfig.threadedChunksRegions.add(region);

        // Save the config
        AutoConfig.getConfigHolder(ThreadedRegionsConfig.class).save();
    }

    private static int executeRemove(CommandContext<CommandSourceStack> cmdCtx) {
        String name = StringArgumentType.getString(cmdCtx, "name");
        ThreadedRegionsConfig regionsConfig = AutoConfig.getConfigHolder(ThreadedRegionsConfig.class).getConfig();

        // Check if region exists
        ThreadedChunksRegion targetRegion = null;
        for (ThreadedChunksRegion region : ParallelProcessor.threadedChunksRegions) {
            if (region.getName().equals(name)) {
                targetRegion = region;
                break;
            }
        }

        if (targetRegion == null) {
            cmdCtx.getSource().sendFailure(Component.literal("No region found with name: " + name));
            return 0;
        }

        // Remove from runtime list first
        ParallelProcessor.removeThreadedChunksRegionByName(name);

        // Then remove from config
        if (targetRegion.getSource().equals("config"))
            removeRegionFromConfig(name);

        String message = String.format("Removed threaded region '%s'", name);
        cmdCtx.getSource().sendSuccess(() -> Component.literal(message), true);
        return 1;
    }

    private static void removeRegionFromConfig(String name) {
        ThreadedRegionsConfig regionsConfig = AutoConfig.getConfigHolder(ThreadedRegionsConfig.class).getConfig();

        if (regionsConfig.threadedChunksRegions != null) {
            regionsConfig.threadedChunksRegions.removeIf(r -> r.getName().equals(name));
            AutoConfig.getConfigHolder(ThreadedRegionsConfig.class).save();
        }
    }

    private static int executeRadiusAdd(CommandContext<CommandSourceStack> cmdCtx, String providedName) throws CommandSyntaxException {
        int centerX = IntegerArgumentType.getInteger(cmdCtx, "x");
        int centerZ = IntegerArgumentType.getInteger(cmdCtx, "z");
        int radius = IntegerArgumentType.getInteger(cmdCtx, "radius");

        ResourceKey<Level> worldKey = ResourceKeyArgument.getRegistryKey(cmdCtx, "world", Registries.DIMENSION, INVALID_WORLD_EXCEPTION);
        String worldId = worldKey.location().toString();

        int x1 = centerX - (radius - 1);
        int z1 = centerZ - (radius - 1);
        int x2 = centerX + (radius - 1);
        int z2 = centerZ + (radius - 1);

        String name = providedName != null ? providedName :
                String.format("chunk_%s_%d_%d_to_%d_%d", worldKey.location().getPath(), x1, z1, x2, z2);

        ThreadedChunksRegion region = new ThreadedChunksRegion(name, worldId, x1, z1, x2, z2);
        region.setMultiThreadChunkTick(false);
        region.setMultiThreadEntityTick(false);
        region.setMultiThreadBlockEntityTick(false);

        try {
            saveRegionToConfig(region);
            ParallelProcessor.addThreadedChunksRegion(region);

            String message = String.format("Added new threaded region '%s' with radius %d around chunk (%d, %d) in world %s",
                    name, radius, centerX, centerZ, worldKey.location());
            cmdCtx.getSource().sendSuccess(() -> Component.literal(message), true);
            return 1;
        } catch (IllegalStateException e) {
            cmdCtx.getSource().sendFailure(Component.literal(e.getMessage()));
            return 0;
        }
    }

    private static int executeExplicitAdd(CommandContext<CommandSourceStack> cmdCtx, String providedName) throws CommandSyntaxException {
        int x1 = IntegerArgumentType.getInteger(cmdCtx, "x1");
        int z1 = IntegerArgumentType.getInteger(cmdCtx, "z1");
        int x2 = IntegerArgumentType.getInteger(cmdCtx, "x2");
        int z2 = IntegerArgumentType.getInteger(cmdCtx, "z2");

        ResourceKey<Level> worldKey = ResourceKeyArgument.getRegistryKey(cmdCtx, "world", Registries.DIMENSION, INVALID_WORLD_EXCEPTION);
        String worldId = worldKey.location().toString();

        String name = providedName != null ? providedName :
                String.format("chunk_%s_%d_%d_to_%d_%d", worldKey.location().getPath(), x1, z1, x2, z2);

        ThreadedChunksRegion region = new ThreadedChunksRegion(name, worldId, x1, z1, x2, z2);
        region.setMultiThreadChunkTick(false);
        region.setMultiThreadEntityTick(false);
        region.setMultiThreadBlockEntityTick(false);

        try {
            saveRegionToConfig(region);
            ParallelProcessor.addThreadedChunksRegion(region);

            String message = String.format("Added new threaded region '%s' from (%d, %d) to (%d, %d) in world %s",
                    name, x1, z1, x2, z2, worldKey.location());
            cmdCtx.getSource().sendSuccess(() -> Component.literal(message), true);
            return 1;
        } catch (IllegalStateException e) {
            cmdCtx.getSource().sendFailure(Component.literal(e.getMessage()));
            return 0;
        }
    }

    private static int executeSetTick(CommandContext<CommandSourceStack> cmdCtx, String tickType) throws CommandSyntaxException {
        String name = StringArgumentType.getString(cmdCtx, "name");
        boolean enabled = BoolArgumentType.getBool(cmdCtx, "enabled");

        ThreadedChunksRegion targetRegion = null;

        for (ThreadedChunksRegion region : ParallelProcessor.threadedChunksRegions) {
            if (region.getName().equals(name)) {
                targetRegion = region;
                break;
            }
        }

        if (targetRegion == null) {
            cmdCtx.getSource().sendFailure(Component.literal("No region found with name: " + name));
            return 0;
        }

        switch (tickType) {
            case "chunkTick":
                targetRegion.setMultiThreadChunkTick(enabled);
                break;
            case "entityTick":
                targetRegion.setMultiThreadEntityTick(enabled);
                break;
            case "blockEntityTick":
                targetRegion.setMultiThreadBlockEntityTick(enabled);
                break;
        }

        // Save the updated config
        AutoConfig.getConfigHolder(ThreadedRegionsConfig.class).save();

        String message = String.format("Set %s to %s for region '%s'", tickType, enabled, name);
        cmdCtx.getSource().sendSuccess(() -> Component.literal(message), true);
        return 1;
    }

    private static int executeListCompact(CommandContext<CommandSourceStack> cmdCtx) {

        if (ParallelProcessor.threadedChunksRegions.isEmpty()) {
            cmdCtx.getSource().sendSuccess(() -> Component.literal("No threaded regions configured."), false);
            return 0;
        }

        cmdCtx.getSource().sendSuccess(() -> Component.literal("=== Regions | C: ChunkTick, E: EntityTick, B: BlockEntityTick ==="), false);

        for (ThreadedChunksRegion region : ParallelProcessor.threadedChunksRegions) {
            // Extract world path from full ID (e.g., "minecraft:overworld" -> "overworld")
            String worldPath = region.getWorldId().substring(region.getWorldId().lastIndexOf(':') + 1);

            String regionInfo = String.format(
                    "§6%s§r %s (%d,%d)->(%d,%d) [%s %s %s]",
                    region.getName(),
                    worldPath,
                    region.getX1(), region.getZ1(),
                    region.getX2(), region.getZ2(),
                    formatEnabledCompact(region.isMultiThreadChunkTick(), 'C'),
                    formatEnabledCompact(region.isMultiThreadEntityTick(), 'E'),
                    formatEnabledCompact(region.isMultiThreadBlockEntityTick(), 'B')
            );

            cmdCtx.getSource().sendSuccess(() -> Component.literal(regionInfo), false);
        }

        return 1;
    }

    private static int executeList(CommandContext<CommandSourceStack> cmdCtx) {

        if (ParallelProcessor.threadedChunksRegions.isEmpty()) {
            cmdCtx.getSource().sendSuccess(() -> Component.literal("No threaded regions configured."), false);
            return 0;
        }

        cmdCtx.getSource().sendSuccess(() -> Component.literal("=== Configured Regions ==="), false);

        for (ThreadedChunksRegion region : ParallelProcessor.threadedChunksRegions) {
            String regionInfo = String.format(
                    "§6%s§r: %s (%d, %d) to (%d, %d)\n" +
                            "   §7Chunk tick: %s, Entity tick: %s, Block Entity tick: %s§r",
                    region.getName(),
                    region.getWorldId(),
                    region.getX1(), region.getZ1(),
                    region.getX2(), region.getZ2(),
                    formatEnabled(region.isMultiThreadChunkTick()),
                    formatEnabled(region.isMultiThreadEntityTick()),
                    formatEnabled(region.isMultiThreadBlockEntityTick())
            );

            cmdCtx.getSource().sendSuccess(() -> Component.literal(regionInfo), false);
        }

        return 1;
    }

    private static int executeShow(CommandContext<CommandSourceStack> cmdCtx) {
        String name = StringArgumentType.getString(cmdCtx, "name");

        ThreadedChunksRegion targetRegion = null;
        for (ThreadedChunksRegion region : ParallelProcessor.threadedChunksRegions) {
            if (region.getName().equals(name)) {
                targetRegion = region;
                break;
            }
        }

        if (targetRegion == null) {
            cmdCtx.getSource().sendFailure(Component.literal("No region found with name: " + name));
            return 0;
        }

        int width = Math.abs(targetRegion.getX2() - targetRegion.getX1()) + 1;
        int length = Math.abs(targetRegion.getZ2() - targetRegion.getZ1()) + 1;

        // Calculate center coordinates
        int centerX = (targetRegion.getX1() + targetRegion.getX2()) / 2;
        int centerZ = (targetRegion.getZ1() + targetRegion.getZ2()) / 2;

        // Calculate radius (if it's a square region)
        int radiusX = (targetRegion.getX2() - targetRegion.getX1()) / 2;
        int radiusZ = (targetRegion.getZ2() - targetRegion.getZ1()) / 2;
        boolean isSquare = radiusX == radiusZ;

        String[] details = {
                String.format("§6=== Region Details: %s ===§r", targetRegion.getName()),
                String.format("World: %s", targetRegion.getWorldId()),
                String.format("Coordinates: (%d, %d) to (%d, %d)",
                        targetRegion.getX1(), targetRegion.getZ1(),
                        targetRegion.getX2(), targetRegion.getZ2()),
                String.format("Center: (%d, %d)%s",
                        centerX, centerZ,
                        isSquare ? String.format(" (radius: %d chunks)", radiusX + 1) : ""),
                String.format("Dimensions: %dx%d chunks (%d total)", width, length, width * length),
                "Settings:",
                String.format("  - Chunk tick: %s", formatEnabled(targetRegion.isMultiThreadChunkTick())),
                String.format("  - Entity tick: %s", formatEnabled(targetRegion.isMultiThreadEntityTick())),
                String.format("  - Block Entity tick: %s", formatEnabled(targetRegion.isMultiThreadBlockEntityTick()))
        };

        for (String detail : details) {
            cmdCtx.getSource().sendSuccess(() -> Component.literal(detail), false);
        }

        return 1;
    }

    private static String formatEnabled(boolean enabled) {
        return enabled ? "§aenabled§r" : "§cdisabled§r";
    }

    private static String formatEnabledCompact(boolean enabled, char letter) {
        return enabled ? "§a" + letter + "§r" : "§c" + letter + "§r";
    }

    private static CompletableFuture<Suggestions> suggestChunkCoordinate(CommandSourceStack source, SuggestionsBuilder builder, boolean isX) {
        try {
            if (source.getPlayer() != null) {
                int chunkCoord = isX ?
                        source.getPlayer().chunkPosition().x :
                        source.getPlayer().chunkPosition().z;

                // First suggest current position
                builder.suggest(chunkCoord, Component.literal(String.format("current %s", isX ? "X" : "Z")));
            }
        } catch (Exception e) {
        }
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestRegionNames(CommandSourceStack source, SuggestionsBuilder builder) {
        for (ThreadedChunksRegion region : ParallelProcessor.threadedChunksRegions) {
            builder.suggest(region.getName());
        }
        return builder.buildFuture();
    }

    private static void dumpRegionStats(CommandSourceStack source) {
        synchronized (ParallelProcessor.threadedChunksRegions) {
            if (ParallelProcessor.threadedChunksRegions.isEmpty()) {
                MutableComponent message = Component.literal("No regions are defined.")
                        .withStyle(ChatFormatting.RED);
                source.sendSuccess(() -> message, false);
                return;
            }

            // Output stats for worlds
            for (Map.Entry<ServerLevel, WorldTickStats> entry : ParallelProcessor.worldTickStats.entrySet()) {
                ServerLevel world = entry.getKey();
                WorldTickStats stats = entry.getValue();
                String worldName = world.dimension().location().toString();

                dumpTickStats(source, "World", worldName,
                        stats.chunkTickTimesLast,
                        stats.entityTickTimesLast,
                        stats.blockEntityTickTimesLast);
            }

            // Output stats for regions
            for (ThreadedChunksRegion region : ParallelProcessor.threadedChunksRegions) {
                dumpTickStats(source, "Region", region.getName(),
                        region.getChunkTickTimesLast(),
                        region.getEntityTickTimesLast(),
                        region.getBlockEntityTickTimesLast());
            }
        }
    }

    private static void dumpTickStats(
            CommandSourceStack source,
            String categoryName,
            String instanceName,
            ConcurrentLinkedQueue<Long> chunkTimes,
            ConcurrentLinkedQueue<Long> entityTimes,
            ConcurrentLinkedQueue<Long> blockEntityTimes) {

        ThreadedChunksRegion region = null;
        if (categoryName.equals("Region")) {
            // Find the matching region to get stage durations
            for (ThreadedChunksRegion r : ParallelProcessor.threadedChunksRegions) {
                if (r.getName().equals(instanceName)) {
                    region = r;
                    break;
                }
            }
        }

        // Build the header message
        MutableComponent message = Component.literal(categoryName + " '" + instanceName + "':\n")
                .withStyle(ChatFormatting.BOLD, ChatFormatting.YELLOW);

        // Add chunk statistics
        message.append(formatTickStats("Chunk Ticks", chunkTimes,
                region != null ? region.getLastChunkStageDuration() : null,
                ChatFormatting.GREEN));

        // Add entity statistics
        message.append(formatTickStats("Entity Ticks", entityTimes,
                region != null ? region.getLastEntityStageDuration() : null,
                ChatFormatting.AQUA));

        // Add block entity statistics
        message.append(formatTickStats("Block Entity Ticks", blockEntityTimes,
                region != null ? region.getLastBlockEntityStageDuration() : null,
                ChatFormatting.LIGHT_PURPLE));

        // Send the message to the command source
        source.sendSuccess(() -> message, false);
    }

    private static MutableComponent formatTickStats(
            String label,
            ConcurrentLinkedQueue<Long> times,
            Long actualStageDuration,
            ChatFormatting color) {

        if (times.isEmpty()) {
            if (actualStageDuration != null) {
                return Component.literal(String.format("  %s - No individual ticks\n",
                                label))
                        .withStyle(ChatFormatting.GRAY);
            }
            return Component.literal("  " + label + " - No data\n")
                    .withStyle(ChatFormatting.GRAY);
        }

        // Aggregate data
        long totalDuration = times.stream().mapToLong(Long::longValue).sum();
        int count = times.size();
        double averageDuration = (double) totalDuration / count;

        // Convert nanoseconds to milliseconds
        double totalMs = totalDuration / 1_000_000.0;
        double averageMs = averageDuration / 1_000_000.0;

        if (actualStageDuration != null) {
            double actualMs = actualStageDuration / 1_000_000.0;
            return Component.literal(String.format(
                            "  %s - Count: %d, Accumulated Time: %.2f ms (avg %.2f ms), Actual Stage Time: %.2f ms\n",
                            label, count, totalMs, averageMs, actualMs))
                    .withStyle(color);
        } else {
            return Component.literal(String.format(
                            "  %s - Count: %d, Total Time: %.2f ms, Average Time: %.2f ms\n",
                            label, count, totalMs, averageMs))
                    .withStyle(color);
        }
    }

}
