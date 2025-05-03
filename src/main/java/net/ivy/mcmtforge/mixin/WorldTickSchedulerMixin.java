package net.ivy.mcmtforge.mixin;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.ivy.mcmtforge.parallelised.ConcurrentCollections;
import net.ivy.mcmtforge.parallelised.fastutil.Long2ObjectOpenConcurrentHashMap;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.LevelTickAccess;
import net.minecraft.world.ticks.LevelTicks;
import net.minecraft.world.ticks.ScheduledTick;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.CopyOnWriteArrayList;

@Mixin(LevelTicks.class)
public abstract class WorldTickSchedulerMixin<T> implements LevelTickAccess<T> {

    @Shadow
    @Final
    @Mutable
    private Long2ObjectMap<LevelChunkTicks<T>> allContainers = new Long2ObjectOpenConcurrentHashMap<>();

//    @Shadow
//    @Final
//    private final Long2LongMap nextTickForContainer = new Long2LongConcurrentHashMap(9223372036854775807L);

    @Shadow
    @Final
    @Mutable
    private Queue<LevelChunkTicks<T>> containersToTick = ConcurrentCollections.newArrayDeque();

    @Shadow
    @Final
    @Mutable
    private Queue<ScheduledTick<T>> toRunThisTick = ConcurrentCollections.newArrayDeque();

    @Shadow
    @Final
    @Mutable
    private List<ScheduledTick<T>> alreadyRunThisTick = new CopyOnWriteArrayList<>();
}
