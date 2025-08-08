package net.tessa.mcmtforge.mixin;

import com.mojang.datafixers.DataFixer;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.RandomSequences;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.ServerLevelData;
import net.tessa.mcmtforge.ParallelProcessor;
import net.tessa.mcmtforge.parallelised.ConcurrentCollections;
import net.tessa.mcmtforge.parallelised.ParaServerChunkProvider;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockEventData;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.entity.ChunkStatusUpdateListener;
import net.minecraft.world.level.entity.EntityTickList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

@Mixin(ServerLevel.class)
public abstract class ServerWorldMixin implements WorldGenLevel {

    ConcurrentLinkedDeque<BlockEventData> syncedBlockEventCLinkedQueue = new ConcurrentLinkedDeque<BlockEventData>();

    @Shadow
    @Final
    @Mutable
    Set<Mob> navigatingMobs;

    @Shadow
    @Final
    @Mutable
    private ObjectLinkedOpenHashSet<BlockEventData> blockEvents = null;

    @Shadow
    @Final
    EntityTickList entityTickList;

    @Unique
    ServerLevel thisWorld = (ServerLevel) (Object) this;

    @Redirect(method = "<init>", at = @At(value = "NEW", target = "(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/storage/LevelStorageSource$LevelStorageAccess;Lcom/mojang/datafixers/DataFixer;Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplateManager;Ljava/util/concurrent/Executor;Lnet/minecraft/world/level/chunk/ChunkGenerator;IIZLnet/minecraft/server/level/progress/ChunkProgressListener;Lnet/minecraft/world/level/entity/ChunkStatusUpdateListener;Ljava/util/function/Supplier;)Lnet/minecraft/server/level/ServerChunkCache;"))
    private ServerChunkCache overwriteServerChunkManager(ServerLevel world, LevelStorageSource.LevelStorageAccess session, DataFixer dataFixer, StructureTemplateManager structureTemplateManager, Executor workerExecutor, ChunkGenerator chunkGenerator, int viewDistance, int simulationDistance, boolean dsync, ChunkProgressListener worldGenerationProgressListener, ChunkStatusUpdateListener chunkStatusChangeListener, Supplier<DimensionDataStorage> persistentStateManagerFactory) {
        return new ParaServerChunkProvider(world, session, dataFixer, structureTemplateManager, workerExecutor, chunkGenerator, viewDistance, simulationDistance, dsync, worldGenerationProgressListener, chunkStatusChangeListener, persistentStateManagerFactory);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void fix(MinecraftServer pServer, Executor pDispatcher, LevelStorageSource.LevelStorageAccess pLevelStorageAccess, ServerLevelData pServerLevelData, ResourceKey pDimension, LevelStem pLevelStem, ChunkProgressListener pProgressListener, boolean pIsDebug, long pBiomeZoomSeed, List pCustomSpawners, boolean pTickTime, RandomSequences pRandomSequences, CallbackInfo ci) {
        navigatingMobs = ConcurrentCollections.newHashSet();
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V", ordinal = 5))
    private void postChunkTick(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        ParallelProcessor.postChunkTick(thisWorld);
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/ProfilerFiller;push(Ljava/lang/String;)V", ordinal = 2))
    private void preEntityTick(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        ParallelProcessor.preEntityTick(thisWorld);
    }

    //@Redirect(remap = false, method = "m_184063_", at = @At(remap = false, value = "INVOKE", target = "m_46653_"))
    @Redirect(remap = false, method = "m_184063_", at = @At( value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;m_46653_(Ljava/util/function/Consumer;Lnet/minecraft/world/entity/Entity;)V"))
    private void overwriteEntityTicking(ServerLevel instance, Consumer consumer, Entity entity) {
        ParallelProcessor.callEntityTick(consumer, entity, thisWorld);
    }

    @Redirect(method = "blockEvent", at = @At(remap = false,value = "INVOKE", target = "Lit/unimi/dsi/fastutil/objects/ObjectLinkedOpenHashSet;add(Ljava/lang/Object;)Z"))
    private boolean overwriteQueueAdd(ObjectLinkedOpenHashSet<BlockEventData> objectLinkedOpenHashSet, Object object) {
        return syncedBlockEventCLinkedQueue.add((BlockEventData) object);
    }

    @Redirect(method = "clearBlockEvents", at = @At(remap = false, value = "INVOKE", target = "Lit/unimi/dsi/fastutil/objects/ObjectLinkedOpenHashSet;removeIf(Ljava/util/function/Predicate;)Z"))
    private boolean overwriteQueueRemoveIf(ObjectLinkedOpenHashSet<BlockEventData> objectLinkedOpenHashSet, Predicate<BlockEventData> filter) {
        return syncedBlockEventCLinkedQueue.removeIf(filter);
    }

    @Redirect(method = "runBlockEvents", at = @At(remap = false,value = "INVOKE", target = "Lit/unimi/dsi/fastutil/objects/ObjectLinkedOpenHashSet;isEmpty()Z"))
    private boolean overwriteEmptyCheck(ObjectLinkedOpenHashSet<BlockEventData> objectLinkedOpenHashSet) {
        return syncedBlockEventCLinkedQueue.isEmpty();
    }

    @Redirect(method = "runBlockEvents", at = @At(remap = false,value = "INVOKE", target = "Lit/unimi/dsi/fastutil/objects/ObjectLinkedOpenHashSet;removeFirst()Ljava/lang/Object;"))
    private Object overwriteQueueRemoveFirst(ObjectLinkedOpenHashSet<BlockEventData> objectLinkedOpenHashSet) {
        return syncedBlockEventCLinkedQueue.removeFirst();
    }

    @Redirect(method = "runBlockEvents", at = @At(remap = false,value = "INVOKE", target = "Lit/unimi/dsi/fastutil/objects/ObjectLinkedOpenHashSet;addAll(Ljava/util/Collection;)Z"))
    private boolean overwriteQueueAddAll(ObjectLinkedOpenHashSet<BlockEventData> instance, Collection<? extends BlockEventData> c) {
        return syncedBlockEventCLinkedQueue.addAll(c);
    }

    @Redirect(method = "sendBlockUpdated", at = @At(value = "FIELD", target = "Lnet/minecraft/server/level/ServerLevel;isUpdatingNavigations:Z", opcode = Opcodes.PUTFIELD))
    private void skipSendBlockUpdatedCheck(ServerLevel instance, boolean value) {

    }
}
