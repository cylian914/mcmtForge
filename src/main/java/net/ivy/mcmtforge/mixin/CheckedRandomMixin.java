package net.ivy.mcmtforge.mixin;

import net.minecraft.world.level.levelgen.LegacyRandomSource;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LegacyRandomSource.class)
public abstract class CheckedRandomMixin {
}
