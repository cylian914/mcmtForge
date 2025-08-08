package net.tessa.mcmtforge.mixin;

import net.tessa.mcmtforge.ParallelProcessor;
import net.minecraft.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;

@Mixin(Util.class)
public abstract class UtilMixin {


    @Inject(remap = false, method = "m_201861_", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/ForkJoinWorkerThread;setName(Ljava/lang/String;)V"), locals = LocalCapture.CAPTURE_FAILHARD)
    //@Inject(remap = false, method = "m_201861_", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/ForkJoinWorkerThread;setName(Ljava/lang/String;)V"), locals = LocalCapture.CAPTURE_FAILHARD)
    private static void registerThread(String string, ForkJoinPool forkJoinPool, CallbackInfoReturnable<ForkJoinWorkerThread> cir, ForkJoinWorkerThread forkJoinWorkerThread) {
        ParallelProcessor.regThread(string, forkJoinWorkerThread);
    }
}