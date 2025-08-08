package net.tessa.mcmtforge.mixin;

import com.mojang.datafixers.DataFixer;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.LevelHeightAccessor;
import net.tessa.mcmtforge.parallelised.fastutil.ConcurrentLongLinkedOpenHashSet;
import net.tessa.mcmtforge.parallelised.fastutil.Long2ObjectConcurrentHashMap;
import net.minecraft.world.level.chunk.storage.SectionStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Function;

@Mixin(SectionStorage.class)
public abstract class SerializingRegionBasedStorageMixin<R> implements AutoCloseable {
    @Shadow
    @Final
    @Mutable
    private Long2ObjectMap<Optional<R>> storage;

    @Shadow
    @Final
    @Mutable
    private LongLinkedOpenHashSet dirty;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void fix(Path pFolder, Function pCodec, Function pFactory, DataFixer pFixerUpper, DataFixTypes pType, boolean pSync, RegistryAccess pRegistryAccess, LevelHeightAccessor pLevelHeightAccessor, CallbackInfo ci) {
        storage = new Long2ObjectConcurrentHashMap<>();
        dirty = new ConcurrentLongLinkedOpenHashSet();
    }
}
