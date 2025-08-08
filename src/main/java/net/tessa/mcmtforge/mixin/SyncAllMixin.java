package net.tessa.mcmtforge.mixin;

import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ChunkTracker;
import net.minecraft.util.ThreadingDetector;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.warden.AngerManagement;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.gameevent.EuclideanGameEventListenerRegistry;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.lighting.DynamicGraphMinFixedPoint;
import net.minecraft.world.level.pathfinder.BinaryHeap;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.LevelTicks;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = {BinaryHeap.class, LevelChunkTicks.class, ChunkTracker.class, DynamicGraphMinFixedPoint.class,
        PathNavigation.class, ThreadingDetector.class, EuclideanGameEventListenerRegistry.class, LegacyRandomSource.class,
        AngerManagement.class, SimpleCriterionTrigger.class, WorldBorder.class, LevelTicks.class
})
public class SyncAllMixin {
}
