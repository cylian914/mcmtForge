package net.tessa.mcmtforge.mixin;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.tessa.mcmtforge.parallelised.ConcurrentCollections;
import net.tessa.mcmtforge.parallelised.fastutil.Long2ObjectOpenConcurrentHashMap;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.LevelTickAccess;
import net.minecraft.world.ticks.LevelTicks;
import net.minecraft.world.ticks.ScheduledTick;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.LongPredicate;
import java.util.function.Supplier;

@Mixin(LevelTicks.class)
public abstract class WorldTickSchedulerMixin<T> implements LevelTickAccess<T> {

    @Shadow
    @Final
    @Mutable
    private Long2ObjectMap<LevelChunkTicks<T>> allContainers;

//    @Shadow
//    @Final
//    private final Long2LongMap nextTickForContainer = new Long2LongConcurrentHashMap(9223372036854775807L);

    @Shadow
    @Final
    @Mutable
    private Queue<LevelChunkTicks<T>> containersToTick;

    @Shadow
    @Final
    @Mutable
    private Queue<ScheduledTick<T>> toRunThisTick;

    @Shadow
    @Final
    @Mutable
    private List<ScheduledTick<T>> alreadyRunThisTick;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void fix(LongPredicate pTickCheck, Supplier pProfiler, CallbackInfo ci) {
        allContainers = new Long2ObjectOpenConcurrentHashMap<>();
        containersToTick = ConcurrentCollections.newArrayDeque();
        toRunThisTick = ConcurrentCollections.newArrayDeque();
        alreadyRunThisTick = new CopyOnWriteArrayList<>();
    }
}
