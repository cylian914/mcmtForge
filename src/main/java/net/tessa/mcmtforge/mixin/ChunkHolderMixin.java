package net.tessa.mcmtforge.mixin;

import it.unimi.dsi.fastutil.shorts.ShortSet;
import net.tessa.mcmtforge.parallelised.fastutil.ConcurrentShortHashSet;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkHolder.class)
public abstract class ChunkHolderMixin {

    @Mutable
    @Shadow
    @Final
    private ShortSet[] changedBlocksPerSection;

    /*@Inject(method = "<init>", at = @At(value = "FIELD", target = "Lnet/minecraft/server/world/ChunkHolder;blockUpdatesBySection:[Lit/unimi/dsi/fastutil/shorts/ShortSet;", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER))
    private void overwriteShortSet(ChunkPos pos, int level, HeightLimitView world, LightingProvider lightingProvider, ChunkHolder.LevelUpdateListener levelUpdateListener, ChunkHolder.PlayersWatchingChunkProvider playersWatchingChunkProvider, CallbackInfo ci) {
        this.blockUpdatesBySection = new ConcurrentShortHashSet[world.countVerticalSections()];
    }*/
    @Inject(method = "<init>", at = @At("TAIL"))
    private void overwriteShortSet(ChunkPos pos, int level, LevelHeightAccessor world, LevelLightEngine lightingProvider, ChunkHolder.LevelChangeListener levelUpdateListener, ChunkHolder.PlayerProvider playersWatchingChunkProvider, CallbackInfo ci) {
        this.changedBlocksPerSection = new ConcurrentShortHashSet[world.getSectionsCount()];
    }

    @Redirect(method = "blockChanged", at = @At(value = "FIELD", target = "Lnet/minecraft/server/level/ChunkHolder;changedBlocksPerSection:[Lit/unimi/dsi/fastutil/shorts/ShortSet;", args = "array=set"))
    private void setBlockUpdatesBySection(ShortSet[] array, int index, ShortSet value) {
        array[index] = new ConcurrentShortHashSet();
    }
}
