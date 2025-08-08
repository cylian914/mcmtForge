package net.tessa.mcmtforge.mixin;

import net.tessa.mcmtforge.parallelised.ConcurrentCollections;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Set;

@Mixin(GoalSelector.class)
public abstract class GoalSelectorMixin {
    @Shadow
    private final Set<WrappedGoal> availableGoals = ConcurrentCollections.newHashSet();
}
