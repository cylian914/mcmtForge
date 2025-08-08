package net.tessa.mcmtforge.mixin;

import net.minecraft.core.Registry;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.tessa.mcmtforge.parallelised.ConcurrentCollections;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(ChunkAccess.class)
public abstract class ChunkMixin {

    @Shadow
    @Final
    @Mutable
    private Map<BlockPos, BlockEntity> blockEntities;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void fixChunkAcess(ChunkPos pChunkPos, UpgradeData pUpgradeData, LevelHeightAccessor pLevelHeightAccessor, Registry pBiomeRegistry, long pInhabitedTime, LevelChunkSection[] pSections, BlendingData pBlendingData, CallbackInfo ci) {
        blockEntities = ConcurrentCollections.newHashMap();
    }
}
