package net.tessa.mcmtforge.mixin;

import net.minecraft.util.ClassInstanceMultiMap;
import net.tessa.mcmtforge.MCMT;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClassInstanceMultiMap.class)
public class dMixin {
    @Inject(method = "remove", at = @At(value = "INVOKE", target = "Ljava/util/List;remove(Ljava/lang/Object;)Z"))
    private void log(Object pKey, CallbackInfoReturnable<Boolean> cir) {
        MCMT.LOGGER.warn("Class: {}", pKey.getClass().toString());
    }
}
