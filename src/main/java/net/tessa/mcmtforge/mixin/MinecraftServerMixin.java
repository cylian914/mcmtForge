package net.tessa.mcmtforge.mixin;

import net.tessa.mcmtforge.DebugHookTerminator;
import net.tessa.mcmtforge.ParallelProcessor;
import net.minecraft.commands.CommandSource;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.thread.ReentrantBlockableEventLoop;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin extends ReentrantBlockableEventLoop<TickTask> implements CommandSource, AutoCloseable {
    @Shadow
    public abstract ServerLevel overworld();

    @Shadow
    @Final
    private Map<ResourceKey<Level>, ServerLevel> levels;

    public MinecraftServerMixin(String string) {
        super(string);
    }

    @Inject(method = "tickChildren", at = @At(remap = false, value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getWorldArray()[Lnet/minecraft/server/level/ServerLevel;"))
    private void preTick(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        ParallelProcessor.preTick(this.levels.size(), (MinecraftServer) (Object) this);
    }

    @Inject(method = "tickChildren", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V", ordinal = 1))
    private void postTick(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        ParallelProcessor.postTick((MinecraftServer) (Object) this);
    }

    @Redirect(method = "tickChildren", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;tick(Ljava/util/function/BooleanSupplier;)V"))
    private void overwriteTick(ServerLevel serverWorld, BooleanSupplier shouldKeepTicking) {
        ParallelProcessor.callTick(serverWorld, shouldKeepTicking, (MinecraftServer) (Object) this);
    }

    @Redirect(method = "reloadResources", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;isSameThread()Z"))
    private boolean onServerExecutionThreadPatch(MinecraftServer minecraftServer) {
        return ParallelProcessor.serverExecutionThreadPatch(minecraftServer);
    }

    @Redirect(method = "prepareLevels", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerChunkCache;getTickingGenerated()I"))
    private int initialChunkCountBypass(ServerChunkCache instance) {
        if (DebugHookTerminator.isBypassLoadTarget())
            return 441;
        int loaded = this.overworld().getChunkSource().getLoadedChunksCount();
        return Math.min(loaded, 441); // Maybe because multi loading caused overflow
    }
}

