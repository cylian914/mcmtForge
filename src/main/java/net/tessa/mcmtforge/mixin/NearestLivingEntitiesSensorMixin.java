package net.tessa.mcmtforge.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.sensing.NearestLivingEntitySensor;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.function.ToDoubleFunction;

@Mixin(NearestLivingEntitySensor.class)
public class NearestLivingEntitiesSensorMixin<T extends LivingEntity> {
    @Redirect(method = "doTick", at = @At(value = "INVOKE", target = "Ljava/util/Comparator;comparingDouble(Ljava/util/function/ToDoubleFunction;)Ljava/util/Comparator;"))
    private Comparator<LivingEntity> syncSense(ToDoubleFunction<? super LivingEntity> keyExtractor, ServerLevel world, T entity) {
        Map<LivingEntity, Vec3> positionCache = new HashMap<>();

        return (e1, e2) -> {
            Vec3 pos1 = positionCache.computeIfAbsent(e1, Entity::position);
            Vec3 pos2 = positionCache.computeIfAbsent(e2, Entity::position);
            double dist1 = entity.distanceToSqr(pos1);
            double dist2 = entity.distanceToSqr(pos2);
            return Double.compare(dist1, dist2);
        };
    }
}
