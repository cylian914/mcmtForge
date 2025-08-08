package net.tessa.mcmtforge.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.gameevent.EuclideanGameEventListenerRegistry;
import net.minecraft.world.level.gameevent.GameEventListenerRegistry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LevelChunk.class)
public abstract class WorldChunkMixin {
    @Shadow
    protected abstract void removeGameEventListenerRegistry(int ySectionCoord);

    @Shadow
    public abstract Level getLevel();

    @Shadow
    @Final
    Level level;

    @WrapMethod(method = "getListenerRegistry")
    private synchronized GameEventListenerRegistry getGameEventDispatcher(int ySectionCoord, Operation<GameEventListenerRegistry> original) {
        GameEventListenerRegistry dispatcher = original.call(ySectionCoord);
        if (dispatcher == null && this.level instanceof ServerLevel serverWorld) {
            return new EuclideanGameEventListenerRegistry(serverWorld, ySectionCoord, this::removeGameEventListenerRegistry);
        }
        return dispatcher;
    }
}
