package net.ivy.mcmtforge.mixin;

import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import net.ivy.mcmtforge.parallelised.fastutil.ConcurrentLongLinkedOpenHashSet;
import net.minecraft.world.level.lighting.LeveledPriorityQueue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LeveledPriorityQueue.class)
public class PendingUpdateQueueMixin {
    @Redirect(method = "<init>", at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/lighting/LeveledPriorityQueue;queues:[Lit/unimi/dsi/fastutil/longs/LongLinkedOpenHashSet;", args = "array=set"))
    private void overwritePendingIdUpdatesByLevel(LongLinkedOpenHashSet[] hashSets, int index, LongLinkedOpenHashSet hashSet, int levelCount, final int expectedLevelSize) {
        hashSets[index] = new ConcurrentLongLinkedOpenHashSet(expectedLevelSize, 0.5f) {
            @Override
            protected void rehash(int newN) {
                if (newN > expectedLevelSize) {
                    super.rehash(newN);
                }
            }
        };
    }
}