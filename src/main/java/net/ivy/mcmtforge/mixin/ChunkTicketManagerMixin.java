package net.ivy.mcmtforge.mixin;

import it.unimi.dsi.fastutil.longs.LongSet;
import net.ivy.mcmtforge.parallelised.ConcurrentCollections;
import net.ivy.mcmtforge.parallelised.fastutil.ConcurrentLongLinkedOpenHashSet;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.DistanceManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Set;

@Mixin(DistanceManager.class)
public abstract class ChunkTicketManagerMixin {

    @Shadow
    @Final
    @Mutable
    Set<ChunkHolder> chunksToUpdateFutures = ConcurrentCollections.newHashSet();

    @Shadow
    @Final
    @Mutable
    LongSet ticketsToRelease = new ConcurrentLongLinkedOpenHashSet();
}
