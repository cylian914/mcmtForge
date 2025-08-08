package net.tessa.mcmtforge.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.tessa.mcmtforge.MCMT;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.Structure;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class DebugCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> registerDebug(LiteralArgumentBuilder<CommandSourceStack> root) {
        return root.then(literal("getBlockState").then(argument("location", Vec3Argument.vec3()).executes(cmdCtx -> {
            Coordinates loc = Vec3Argument.getCoordinates(cmdCtx, "location");
            BlockPos bp = loc.getBlockPos(cmdCtx.getSource());
            ServerLevel sw = cmdCtx.getSource().getLevel();
            BlockState bs = sw.getBlockState(bp);
            MutableComponent message = Component.literal("Block at " + bp + " is " + bs.getBlock().getName());
            cmdCtx.getSource().sendSuccess(() -> message, true);
            System.out.println(message);
            return 1;
        }))).then(literal("nbtdump").then(argument("location", Vec3Argument.vec3()).executes(cmdCtx -> {
            Coordinates loc = Vec3Argument.getCoordinates(cmdCtx, "location");
            BlockPos bp = loc.getBlockPos(cmdCtx.getSource());
            ServerLevel sw = cmdCtx.getSource().getLevel();
            BlockState bs = sw.getBlockState(bp);
            BlockEntity te = sw.getBlockEntity(bp);
            if (te == null) {
                MutableComponent message = Component.literal("Block at " + bp + " is " + bs.getBlock().getName() + " has no NBT");
                cmdCtx.getSource().sendSuccess(() -> message, true);
                return 1;
            }
            CompoundTag nbt = te.getUpdateTag();
            String nbtStr = nbt.toString();
            MutableComponent message = Component.literal("Block at " + bp + " is " + bs.getBlock().getName() + " with TE NBT:");
            cmdCtx.getSource().sendSuccess(() -> message, true);
            cmdCtx.getSource().sendSuccess(() -> Component.nullToEmpty(nbtStr), true);
            return 1;
        }))).then(literal("tick").requires(cmdSrc -> cmdSrc.hasPermission(2)).then(literal("te")).then(argument("location", Vec3Argument.vec3()).executes(cmdCtx -> {
try {
    Coordinates loc = Vec3Argument.getCoordinates(cmdCtx, "location");
    BlockPos bp = loc.getBlockPos(cmdCtx.getSource());
    ServerLevel sw = cmdCtx.getSource().getLevel();
    BlockEntity te = sw.getBlockEntity(bp);
    if (te != null && ConfigCommand.isTickableBe(te)) {
        ((TickingBlockEntity) te).tick();
        MutableComponent message = Component.literal("Ticked " + te.getClass().getName() + " at " + bp);
        cmdCtx.getSource().sendSuccess(() -> message, true);
    } else {
        MutableComponent message = Component.literal("No tickable TE at " + bp);
        cmdCtx.getSource().sendFailure(message);
    }
    return 1;
} catch (Exception e) {
    MCMT.LOGGER.warn("ERR:", e);
    return 0;
}
        }))).then(literal("classpathDump").requires(cmdSrc -> cmdSrc.hasPermission(2)).executes(cmdCtx -> {
            java.nio.file.Path base = Paths.get("classpath_dump/");
            try {
                Files.createDirectories(base);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            // Copypasta from syncfu;
            Arrays.stream(System.getProperty("java.class.path").split(File.pathSeparator)).flatMap(path -> {
                File file = new File(path);
                if (file.isDirectory()) {
                    return Arrays.stream(file.list((d, n) -> n.endsWith(".jar")));
                }
                return Arrays.stream(new String[]{path});
            }).filter(s -> s.endsWith(".jar")).map(Paths::get).forEach(path -> {
                Path name = path.getFileName();
                try {
                    Files.copy(path, Paths.get(base.toString(), name.toString()), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });


            MutableComponent message = Component.literal("Classpath Dumped to: " + base.toAbsolutePath().toString());
            cmdCtx.getSource().sendSuccess(() -> message, true);
            System.out.println(message);
            return 1;
        }))
                /* 1.16.1 code; AKA the only thing that changed  */.then(literal("test").requires(cmdSrc -> cmdSrc.hasPermission(2)).then(literal("structures").executes(cmdCtx -> {
                    ServerPlayer p = cmdCtx.getSource().getPlayer();
                    assert p != null;
                    BlockPos srcPos = p.blockPosition();
                    UUID id = p.getUUID();
                    int index = structureIdx.computeIfAbsent(id.toString(), (s) -> new AtomicInteger()).getAndIncrement();
                    Registry<Structure> registry = cmdCtx.getSource().getLevel().registryAccess().registryOrThrow(Registries.STRUCTURE);
                    var targets = registry.holders().toList();
                    Holder.Reference<Structure> target;
                    if (index >= targets.size()) {
                        target = targets.get(0);
                        structureIdx.computeIfAbsent(id.toString(), (s) -> new AtomicInteger()).set(0);
                    } else {
                        target = targets.get(index);
                    }
//                    Pair<BlockPos, RegistryKey<Structure>> dst = cmdCtx.getSource().getWorld().getChunkManager().getChunkGenerator().locateStructure(cmdCtx.getSource().getWorld(), RegistryKey.of(target), srcPos, 100, false);
//                    if (dst == null) {
//                        MutableText message = Text.literal("Failed locating " + target.registryKey().getValue().toString() + " from " + srcPos);
//                        cmdCtx.getSource().sendFeedback(message, true);
//                        return 1;
//                    }
//                    MutableText message = Text.literal("Found target; loading now");
//                    cmdCtx.getSource().sendFeedback(message, true);
//                    p.teleport(dst.getFirst().getX(), srcPos.getY(), dst.getFirst().getZ());
                    //LocateCommand.showLocateResult(cmdCtx.getSource(), ResourceOrTagLocationArgument.getStructureFeature(p_207508_, "structure"), srcpos, dst, "commands.locate.success");
                    return 1;
                })));
        /* */
				/*
				.then(literal("goinf").requires(cmdSrc -> {
					return cmdSrc.hasPermissionLevel(2);
				}).executes(cmdCtx -> {
					ServerPlayerEntity p = cmdCtx.getSource().asPlayer();
					p.setPosition(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
					return 1;
				}))
				*/
    }

    private static Map<String, AtomicInteger> structureIdx = new ConcurrentHashMap<>();
}
