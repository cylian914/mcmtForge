package net.ivy.mcmtforge.mixin;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.ivy.mcmtforge.parallelised.fastutil.Int2ObjectConcurrentHashMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityTickList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityTickList.class)
public abstract class EntityListMixin {
    @Shadow
    private Int2ObjectMap<Entity> active = new Int2ObjectConcurrentHashMap<>();

    @Shadow
    private Int2ObjectMap<Entity> passive = new Int2ObjectConcurrentHashMap<>();

    @Inject(method = "ensureActiveIsNotIterated", at = @At(value = "HEAD"), cancellable = true)
    private void notSafeAnyWay(CallbackInfo ci) {
        ci.cancel();
    }
}
