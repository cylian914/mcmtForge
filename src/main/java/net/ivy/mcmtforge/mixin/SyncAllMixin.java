package net.ivy.mcmtforge.mixin;

import net.minecraft.util.ThreadingDetector;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.gameevent.EuclideanGameEventListenerRegistry;
import net.minecraft.world.level.lighting.DynamicGraphMinFixedPoint;
import net.minecraft.world.level.pathfinder.BinaryHeap;
import net.minecraft.world.ticks.LevelChunkTicks;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = {BinaryHeap.class, LevelChunkTicks.class, DynamicGraphMinFixedPoint.class, PathNavigation.class, ThreadingDetector.class, EuclideanGameEventListenerRegistry.class})
public class SyncAllMixin {
}
