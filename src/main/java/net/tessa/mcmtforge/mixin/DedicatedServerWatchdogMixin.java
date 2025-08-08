package net.tessa.mcmtforge.mixin;


import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.server.dedicated.ServerWatchdog;
import net.tessa.mcmtforge.ParallelProcessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

@Mixin(ServerWatchdog.class)
public class DedicatedServerWatchdogMixin {

    @Inject(method = "run", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/Bootstrap;realStdoutPrintln(Ljava/lang/String;)V"), locals = LocalCapture.CAPTURE_FAILSOFT)
    private void addCustomCrashReport(CallbackInfo ci, long l, long m, long n, ThreadMXBean threadMXBean, ThreadInfo threadInfos[], StringBuilder stringBuilder, Error error, CrashReport crashReport, CrashReportCategory crashReportSection, CrashReportCategory crashReportSection2) {
        CrashReportCategory MCMTSection = crashReport.addCategory("MCMT");
        MCMTSection.setDetail("currentTasks", () -> ParallelProcessor.currentTasks.toString());
    }
}
