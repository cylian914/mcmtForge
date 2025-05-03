package net.ivy.mcmtforge.mixin;

import com.mojang.datafixers.DataFixer;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.ivy.mcmtforge.parallelised.fastutil.Int2ObjectConcurrentHashMap;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.world.level.chunk.storage.ChunkStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import java.nio.file.Path;

@Mixin(ChunkMap.class)
public abstract class ThreadedAnvilChunkStorageMixin extends ChunkStorage implements ChunkHolder.PlayerProvider {

    public ThreadedAnvilChunkStorageMixin(Path directory, DataFixer dataFixer, boolean dsync) {
        super(directory, dataFixer, dsync);
    }

    @Shadow
    @Final
    @Mutable
    private Int2ObjectMap<ChunkMap.TrackedEntity> entityMap = new Int2ObjectConcurrentHashMap<>();
}