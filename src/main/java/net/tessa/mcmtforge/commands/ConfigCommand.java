package net.tessa.mcmtforge.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.shedaniel.autoconfig.AutoConfig;
import net.tessa.mcmtforge.MCMT;
import net.tessa.mcmtforge.ParallelProcessor;
import net.tessa.mcmtforge.config.BlockEntityLists;
import net.tessa.mcmtforge.config.GeneralConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import static net.minecraft.commands.Commands.literal;

public class ConfigCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> mcmtconfig = literal("mcmt");
        mcmtconfig = mcmtconfig.then(registerConfig(literal("config")));
        mcmtconfig = mcmtconfig.then(DebugCommand.registerDebug(literal("debug")));
        mcmtconfig = mcmtconfig.then(RegionCommand.registerRegion(literal("region")));
        mcmtconfig = StatsCommand.registerStatus(mcmtconfig);
        dispatcher.register(mcmtconfig);
    }

    public static ArgumentBuilder<CommandSourceStack, ?> registerConfig(LiteralArgumentBuilder<CommandSourceStack> root) {
        GeneralConfig config = MCMT.config;
        return root.then(literal("toggle").requires(cmdSrc -> {
                            return cmdSrc.hasPermission(2);
                        }).executes(cmdCtx -> {
                            config.disabled = !config.disabled;
                            MutableComponent message = Component.literal(
                                    "MCMT is now " + (config.disabled ? "disabled" : "enabled"));
                            cmdCtx.getSource().sendSuccess(() -> message, true);
                            return 1;
                        }).then(literal("te").executes(cmdCtx -> {
                            config.disableBlockEntity = !config.disableBlockEntity;
                            MutableComponent message = Component.literal("MCMT's tile entity threading is now "
                                    + (config.disableBlockEntity ? "disabled" : "enabled"));
                            cmdCtx.getSource().sendSuccess(() -> message, true);
                            return 1;
                        })).then(literal("entity").executes(cmdCtx -> {
                            config.disableEntity = !config.disableEntity;
                            MutableComponent message = Component.literal(
                                    "MCMT's entity threading is now " + (config.disableEntity ? "disabled" : "enabled"));
                            cmdCtx.getSource().sendSuccess(() -> message, true);
                            return 1;
                        })).then(literal("environment").executes(cmdCtx -> {
                            config.disableEnvironment = !config.disableEnvironment;
                            MutableComponent message = Component.literal("MCMT's environment threading is now "
                                    + (config.disableEnvironment ? "disabled" : "enabled"));
                            cmdCtx.getSource().sendSuccess(() -> message, true);
                            return 1;
                        })).then(literal("world").executes(cmdCtx -> {
                            config.disableWorld = !config.disableWorld;
                            MutableComponent message = Component.literal(
                                    "MCMT's world threading is now " + (config.disableWorld ? "disabled" : "enabled"));
                            cmdCtx.getSource().sendSuccess(() -> message, true);
                            return 1;
                        })).then(literal("ops").executes(cmdCtx -> {
                            config.opsTracing = !config.opsTracing;
                            MutableComponent message = Component.literal(
                                    "MCMT's ops tracing is now " + (!config.opsTracing ? "disabled" : "enabled"));
                            cmdCtx.getSource().sendSuccess(() -> message, true);
                            return 1;
                        }))
                )
                .then(literal("state").executes(cmdCtx -> {
                    StringBuilder messageString = new StringBuilder(
                            "MCMT is currently " + (config.disabled ? "disabled" : "enabled"));
                    if (!config.disabled) {
                        messageString.append(" World:" + (config.disableWorld ? "disabled" : "enabled"));
                        messageString.append(" Entity:" + (config.disableEntity ? "disabled" : "enabled"));
                        messageString.append(" TE:" + (config.disableBlockEntity ? "disabled"
                                : "enabled" + (config.chunkLockModded ? "(ChunkLocking Modded)" : "")));
                        messageString.append(" Env:" + (config.disableEnvironment ? "disabled" : "enabled"));
                        messageString.append(" SCP:" + (config.disableChunkProvider ? "disabled" : "enabled"));
                    }
                    MutableComponent message = Component.literal(messageString.toString());
                    cmdCtx.getSource().sendSuccess(() -> message, true);
                    return 1;
                }))
                .then(literal("save").requires(cmdSrc -> {
                    return cmdSrc.hasPermission(2);
                }).executes(cmdCtx -> {
                    MutableComponent message = Component.literal("Saving MCMT config to disk...");
                    cmdCtx.getSource().sendSuccess(() -> message, true);
                    AutoConfig.getConfigHolder(GeneralConfig.class).save();
                    cmdCtx.getSource().sendSuccess(() -> Component.literal("Done!"), true);
                    return 1;
                }))
                .then(literal("temanage").requires(cmdSrc -> {
                            return cmdSrc.hasPermission(2);
                        }).then(literal("list")
                                .executes(cmdCtx -> {
                                    MutableComponent message = Component.literal("NYI");
                                    cmdCtx.getSource().sendSuccess(() -> message, true);
                                    return 1;
                                })).then(literal("target")
                                .requires(cmdSrc -> {
                                    if (cmdSrc.getPlayer() != null) {
                                        return true;
                                    }
                                    MutableComponent message = Component.literal("Only runnable by player!");
                                    cmdSrc.sendFailure(message);
                                    return false;
                                }).then(literal("whitelist").executes(cmdCtx -> {
                                    MutableComponent message;
                                    HitResult htr = cmdCtx.getSource().getPlayer().pick(20, 0.0F, false);
                                    if (htr.getType() == HitResult.Type.BLOCK) {
                                        BlockPos bp = ((BlockHitResult) htr).getBlockPos();
                                        BlockEntity te = cmdCtx.getSource().getLevel().getBlockEntity(bp);
                                        if (te != null && isTickableBe(te)) {
                                            if (config.teWhiteListString.contains(te.getClass().getName())) {
                                                message = Component.literal("Class " + te.getClass().getName() + " already exists in TE Whitelist");
                                                cmdCtx.getSource().sendSuccess(() -> message, true);
                                                return 0;
                                            }
                                            BlockEntityLists.teWhiteList.add(te.getClass());
                                            config.teWhiteListString.add(te.getClass().getName());
                                            BlockEntityLists.teBlackList.remove(te.getClass());
                                            config.teBlackListString.remove(te.getClass().getName());
                                            message = Component.literal("Added " + te.getClass().getName() + " to TE Whitelist");
                                            cmdCtx.getSource().sendSuccess(() -> message, true);
                                            return 1;
                                        }
                                        message = Component.literal("That block doesn't contain a tickable TE!");
                                        cmdCtx.getSource().sendFailure(message);
                                        return 0;
                                    }
                                    message = Component.literal("Only runable by player!");
                                    cmdCtx.getSource().sendFailure(message);
                                    return 0;
                                })).then(literal("blacklist").executes(cmdCtx -> {
                                    MutableComponent message;
                                    HitResult htr = cmdCtx.getSource().getPlayer().pick(20, 0.0F, false);
                                    if (htr.getType() == HitResult.Type.BLOCK) {
                                        BlockPos bp = ((BlockHitResult) htr).getBlockPos();
                                        BlockEntity te = cmdCtx.getSource().getLevel().getBlockEntity(bp);
                                        if (te != null && isTickableBe(te)) {
                                            if (config.teBlackListString.contains(te.getClass().getName())) {
                                                message = Component.literal("Class " + te.getClass().getName() + " already exists in TE Blacklist");
                                                cmdCtx.getSource().sendSuccess(() -> message, true);
                                                return 0;
                                            }
                                            BlockEntityLists.teBlackList.add(te.getClass());
                                            config.teBlackListString.add(te.getClass().getName());
                                            BlockEntityLists.teWhiteList.remove(te.getClass());
                                            config.teWhiteListString.remove(te.getClass().getName());
                                            message = Component.literal("Added " + te.getClass().getName() + " to TE Blacklist");
                                            cmdCtx.getSource().sendSuccess(() -> message, true);
                                            return 1;
                                        }
                                        message = Component.literal("That block doesn't contain a tickable TE!");
                                        cmdCtx.getSource().sendFailure(message);
                                        return 0;
                                    }
                                    message = Component.literal("Only runnable by player!");
                                    cmdCtx.getSource().sendFailure(message);
                                    return 0;
                                })).then(literal("remove").executes(cmdCtx -> {
                                    MutableComponent message;
                                    HitResult htr = cmdCtx.getSource().getPlayer().pick(20, 0.0F, false);
                                    if (htr.getType() == HitResult.Type.BLOCK) {
                                        BlockPos bp = ((BlockHitResult) htr).getBlockPos();
                                        BlockEntity te = cmdCtx.getSource().getLevel().getBlockEntity(bp);
                                        if (te != null && isTickableBe(te)) {
                                            BlockEntityLists.teBlackList.remove(te.getClass());
                                            config.teBlackListString.remove(te.getClass().getName());
                                            BlockEntityLists.teWhiteList.remove(te.getClass());
                                            config.teWhiteListString.remove(te.getClass().getName());
                                            message = Component.literal("Removed " + te.getClass().getName() + " from TE classlists");
                                            cmdCtx.getSource().sendSuccess(() -> message, true);
                                            return 1;
                                        }
                                        message = Component.literal("That block doesn't contain a tickable TE!");
                                        cmdCtx.getSource().sendFailure(message);
                                        return 0;
                                    }
                                    message = Component.literal("Only runable by player!");
                                    cmdCtx.getSource().sendFailure(message);
                                    return 0;
                                }))
                        )
                );
    }

    public static boolean isTickableBe(BlockEntity be) {
        BlockEntityTicker<?> blockEntityTicker = be.getBlockState().getTicker(be.getLevel(), be.getType());
        return blockEntityTicker != null;
    }
}
