package net.tessa.mcmtforge.mixin;

import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.tessa.mcmtforge.parallelised.fastutil.Long2ByteConcurrentHashMap;
import net.tessa.mcmtforge.parallelised.fastutil.Long2ObjectOpenConcurrentHashMap;
import net.minecraft.server.level.ChunkTracker;
import net.minecraft.server.level.Ticket;
import net.minecraft.server.level.TickingTracker;
import net.minecraft.util.SortedArraySet;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TickingTracker.class)
public abstract class SimulationDistanceLevelPropagatorMixin extends ChunkTracker {
    @Shadow
    @Final
    @Mutable
    protected Long2ByteMap chunks;

    @Shadow
    @Final
    @Mutable
    private Long2ObjectOpenHashMap<SortedArraySet<Ticket<?>>> tickets = new Long2ObjectOpenConcurrentHashMap<>();

    protected SimulationDistanceLevelPropagatorMixin(int i, int j, int k) {
        super(i, j, k);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void fix(CallbackInfo ci) {
        chunks =  new Long2ByteConcurrentHashMap();
    }
}
