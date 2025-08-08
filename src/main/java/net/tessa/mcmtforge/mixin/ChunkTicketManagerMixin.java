package net.tessa.mcmtforge.mixin;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.server.level.Ticket;
import net.minecraft.util.SortedArraySet;
import net.tessa.mcmtforge.parallelised.ConcurrentCollections;
import net.tessa.mcmtforge.parallelised.fastutil.ConcurrentLongLinkedOpenHashSet;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.DistanceManager;
import net.tessa.mcmtforge.parallelised.fastutil.Long2ObjectOpenConcurrentHashMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;
import java.util.concurrent.Executor;

@Mixin(DistanceManager.class)
public abstract class ChunkTicketManagerMixin {

    @Final
    @Shadow
    @Mutable
    Set<ChunkHolder> chunksToUpdateFutures;

    @Final
    @Shadow
    @Mutable
    LongSet ticketsToRelease;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void changeType(Executor pDispatcher, Executor pMainThreadExecutor, CallbackInfo ci) {
        chunksToUpdateFutures = ConcurrentCollections.newHashSet();
        ticketsToRelease =  new ConcurrentLongLinkedOpenHashSet();
    }
}
