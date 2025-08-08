package net.tessa.mcmtforge.mixin;

import net.minecraft.core.SectionPos;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.tessa.mcmtforge.MCMT;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.world.level.entity.PersistentEntitySectionManager$Callback")
public class PersistentEntitySectionManagerMixin {
    @Shadow private long currentSectionKey;

    @Unique Thread t;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void recordThread(PersistentEntitySectionManager this$0, EntityAccess pEntity, long pCurrentSectionKey, EntitySection pCurrentSection, CallbackInfo ci) {
        t = Thread.currentThread();
    }

}
