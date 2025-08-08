package net.tessa.mcmtforge.mixin;

import net.minecraft.world.level.Level;
import net.tessa.mcmtforge.MCMT;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayDeque;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.world.level.redstone.CollectingNeighborUpdater;
import net.minecraft.world.level.redstone.NeighborUpdater;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CollectingNeighborUpdater.class)
public abstract class ChainRestrictedNeighborUpdaterMixin implements NeighborUpdater {

    @Shadow
    @Final
    @Mutable
    List<CollectingNeighborUpdater.NeighborUpdates> addedThisLayer;


    @Shadow @Final private ArrayDeque<CollectingNeighborUpdater.NeighborUpdates> stack;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void setAddedThisLayer(Level pLevel, int pMaxChainedNeighborUpdates, CallbackInfo ci) {
         addedThisLayer = new CopyOnWriteArrayList<>();
     }

     @Inject(method = "runUpdates", at = @At(value = "INVOKE", target = "Ljava/util/List;clear()V", ordinal = 0))
    private void log(CallbackInfo ci) {
//         MCMT.LOGGER.warn("stack size: {}", stack.size());
     }
}
