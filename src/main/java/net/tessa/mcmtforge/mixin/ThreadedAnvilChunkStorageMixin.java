package net.tessa.mcmtforge.mixin;

import com.mojang.datafixers.DataFixer;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.entity.ChunkStatusUpdateListener;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.tessa.mcmtforge.parallelised.fastutil.Int2ObjectConcurrentHashMap;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.world.level.chunk.storage.ChunkStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

@Mixin(ChunkMap.class)
public abstract class ThreadedAnvilChunkStorageMixin extends ChunkStorage implements ChunkHolder.PlayerProvider {

    public ThreadedAnvilChunkStorageMixin(Path directory, DataFixer dataFixer, boolean dsync) {
        super(directory, dataFixer, dsync);
    }

    @Shadow
    @Final
    @Mutable
    private Int2ObjectMap<ChunkMap.TrackedEntity> entityMap;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void fix(ServerLevel pLevel, LevelStorageSource.LevelStorageAccess pLevelStorageAccess, DataFixer pFixerUpper, StructureTemplateManager pStructureManager, Executor pDispatcher, BlockableEventLoop pMainThreadExecutor, LightChunkGetter pLightChunk, ChunkGenerator pGenerator, ChunkProgressListener pProgressListener, ChunkStatusUpdateListener pChunkStatusListener, Supplier pOverworldDataStorage, int pViewDistance, boolean pSync, CallbackInfo ci) {
        entityMap = new Int2ObjectConcurrentHashMap<>();
    }
}