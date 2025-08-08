package net.tessa.mcmtforge.mixin;

import it.unimi.dsi.fastutil.longs.Long2ObjectFunction;
import it.unimi.dsi.fastutil.longs.LongSortedSet;
import net.tessa.mcmtforge.parallelised.fastutil.ConcurrentLongSortedSet;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntitySectionStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(EntitySectionStorage.class)
public abstract class SectionedEntityCacheMixin<T extends EntityAccess> {

//    @Shadow
//    @Final
//    private final Long2ObjectMap<EntityTrackingSection<T>> trackingSections = new Long2ObjectConcurrentHashMap<>();

    @Shadow
    @Final
    @Mutable
    private LongSortedSet sectionIds;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void fix(Class pEntityClass, Long2ObjectFunction pInitialSectionVisibility, CallbackInfo ci) {
        sectionIds = new ConcurrentLongSortedSet();
    }
}
