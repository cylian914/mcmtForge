package net.tessa.mcmtforge.mixin;

import net.tessa.mcmtforge.ParallelProcessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Level.class)
public abstract class WorldMixin implements LevelAccessor, AutoCloseable {
    @Shadow
    @Final
    @Mutable
    private Thread thread;

    @Inject(method = "tickBlockEntities", at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;"))
    private void postEntityPreBlockEntityTick(CallbackInfo ci) {
        if ((Object) this instanceof ServerLevel) {
            ServerLevel thisWorld = (ServerLevel) (Object) this;
            ParallelProcessor.postEntityTick(thisWorld);
            ParallelProcessor.preBlockEntityTick(thisWorld);
        }
    }

    @Inject(method = "tickBlockEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/ProfilerFiller;pop()V"))
    private void postBlockEntityTick(CallbackInfo ci) {
        if ((Object) this instanceof ServerLevel) {
            ServerLevel thisWorld = (ServerLevel) (Object) this;
            ParallelProcessor.postBlockEntityTick(thisWorld);
        }
    }

    @Redirect(method = "tickBlockEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/TickingBlockEntity;tick()V"))
    private void overwriteBlockEntityTick(TickingBlockEntity blockEntityTickInvoker) {
        ParallelProcessor.callBlockEntityTick(blockEntityTickInvoker, (Level) (Object) this);
    }

    @Redirect(method = "getBlockEntity", at = @At(value = "INVOKE", target = "Ljava/lang/Thread;currentThread()Ljava/lang/Thread;"))
    private Thread overwriteCurrentThread() {
        return this.thread;
    }

//    @Redirect(method = "Lnet/minecraft/world/level/Level;getChunk(II)Lnet/minecraft/world/level/chunk/LevelChunk;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getChunk(IILnet/minecraft/world/level/chunk/ChunkStatus;)Lnet/minecraft/world/level/chunk/ChunkAccess;"))
//   private Chunk getChunk(World world, int x, int z, net.minecraft.world.chunk.ChunkStatus leastStatus, int i, int j) {}
//        Chunk chunk;
//        long startTime, counter = -1;
//        startTime = System.currentTimeMillis();
//
//        do {
//            chunk = world.getChunk(x, z, leastStatus);
//            counter++;
//            if (counter>0)
//                System.out.println("getChunk() retry: " + counter);
//        } while (chunk instanceof ReadOnlyChunk);
//
//        if (counter > 0) {
//            MCMT.LOGGER.warn("Chunk at " + x + ", " + z + " was ReadOnlyChunk for " + counter + " times before completely loaded. Took " + (System.currentTimeMillis() - startTime) + "ms");
//        }
//        return chunk;
//    }

}