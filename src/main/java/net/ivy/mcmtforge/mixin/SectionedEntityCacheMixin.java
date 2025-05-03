package net.ivy.mcmtforge.mixin;

import it.unimi.dsi.fastutil.longs.LongSortedSet;
import net.ivy.mcmtforge.parallelised.fastutil.ConcurrentLongSortedSet;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntitySectionStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;


@Mixin(EntitySectionStorage.class)
public abstract class SectionedEntityCacheMixin<T extends EntityAccess> {

//    @Shadow
//    @Final
//    private final Long2ObjectMap<EntityTrackingSection<T>> trackingSections = new Long2ObjectConcurrentHashMap<>();

    @Shadow
    @Final
    @Mutable
    private LongSortedSet sectionIds = new ConcurrentLongSortedSet();

}
