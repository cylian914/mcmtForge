package net.tessa.mcmtforge.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    @Shadow
    private Optional<BlockPos> lastClimbablePos;

    @Shadow
    protected abstract boolean trapdoorUsableAsLadder(BlockPos pos, BlockState state);

    public LivingEntityMixin(EntityType<?> type, Level world) {
        super(type, world);
    }

    @Inject(method = "onClimbable", at = @At(value = "INVOKE", remap = false, target = "Lnet/minecraftforge/common/ForgeHooks;isLivingOnLadder(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/LivingEntity;)Ljava/util/Optional;"), cancellable = true)
    private void modifyIsInClimbable(CallbackInfoReturnable<Boolean> cir) {
        try {
            if (this.isSpectator()) {
                cir.setReturnValue(false);
            } else {
                BlockPos blockPos = this.blockPosition();
                BlockState blockState = this.getBlockStateOn();
                if (blockState.is(BlockTags.CLIMBABLE)) {
                    this.lastClimbablePos = Optional.of(blockPos);
                    cir.setReturnValue(true);
                } else if (blockState.getBlock() instanceof TrapDoorBlock && this.trapdoorUsableAsLadder(blockPos, blockState)) {
                    this.lastClimbablePos = Optional.of(blockPos);
                    cir.setReturnValue(true);
                } else {
                    cir.setReturnValue(false);
                }
            }
        } catch (Exception e) {
            cir.setReturnValue(false);
        }
    }


    @WrapMethod(method = "die")
    private synchronized void onDeath(DamageSource damageSource, Operation<Void> original) {
        original.call(damageSource);
    }
}
