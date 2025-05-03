package net.ivy.mcmtforge.mixin;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import net.ivy.mcmtforge.parallelised.fastutil.ConcurrentLongLinkedOpenHashSet;
import net.ivy.mcmtforge.parallelised.fastutil.Long2ObjectConcurrentHashMap;
import net.minecraft.world.level.chunk.storage.SectionStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Optional;

@Mixin(SectionStorage.class)
public abstract class SerializingRegionBasedStorageMixin<R> implements AutoCloseable {
    @Shadow
    @Final
    @Mutable
    private Long2ObjectMap<Optional<R>> storage = new Long2ObjectConcurrentHashMap<>();

    @Shadow
    @Final
    @Mutable
    private LongLinkedOpenHashSet dirty = new ConcurrentLongLinkedOpenHashSet();
}
