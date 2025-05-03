package net.ivy.mcmtforge.mixin;

import net.ivy.mcmtforge.parallelised.ConcurrentCollections;
import net.minecraft.util.ClassInstanceMultiMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.AbstractCollection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collector;

@Mixin(ClassInstanceMultiMap.class)
public abstract class TypeFilterableListMixin<T> extends AbstractCollection<T> {
    @Shadow
    @Final
    @Mutable
    private Map<Class<?>, List<T>> byClass = new ConcurrentHashMap<>();

    @Shadow
    @Final
    @Mutable
    private List<T> allInstances = new CopyOnWriteArrayList<>();

    @ModifyArg(method = "lambda$find$0", at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;collect(Ljava/util/stream/Collector;)Ljava/lang/Object;"))
    private <T> Collector<T, ?, List<T>> overwriteCollectToList(Collector<T, ?, List<T>> collector) {
        return ConcurrentCollections.toList();
    }
}