package net.tessa.mcmtforge.mixin;

import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import net.tessa.mcmtforge.parallelised.fastutil.Long2ByteConcurrentHashMap;
import net.minecraft.world.level.lighting.DynamicGraphMinFixedPoint;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(DynamicGraphMinFixedPoint.class)
public abstract class LevelPropagatorMixin {

    @Final
    @Shadow
    @Mutable
    Long2ByteMap computedLevels;


    @Redirect(method = "<init>", at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/lighting/DynamicGraphMinFixedPoint;computedLevels:Lit/unimi/dsi/fastutil/longs/Long2ByteMap;", opcode = Opcodes.PUTFIELD))
    private void overwritePendingUpdates(DynamicGraphMinFixedPoint instance, Long2ByteMap value, int levelCount, final int expectedLevelSize, final int expectedTotalSize) {
        computedLevels = new Long2ByteConcurrentHashMap(expectedTotalSize, 0.5f);
    }
}
