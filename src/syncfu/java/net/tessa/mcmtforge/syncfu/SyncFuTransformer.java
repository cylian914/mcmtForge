package net.tessa.mcmtforge.syncfu;

import cpw.mods.modlauncher.api.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.ClassNode;

import java.util.*;
import org.objectweb.asm.Opcodes;

public class SyncFuTransformer implements ITransformer<ClassNode>, ITransformationService {

    private static final Logger syncFuTransformerLogger = LogManager.getLogger();

    @Override
    public String name() {

        return "syncFu";
    }

    @Override
    public void initialize(IEnvironment environment) {
        syncFuTransformerLogger.warn("env 123");
    }

    @Override
    public void onLoad(IEnvironment env, Set<String> otherServices) throws IncompatibleEnvironmentException {
        syncFuTransformerLogger.warn("load 123");
    }

    @Override
    public List<ITransformer> transformers() {
        return List.of(this);
    }

    @Override
    public  ClassNode transform(ClassNode input, ITransformerVotingContext context) {
        input.methods.forEach((m) -> {
            m.access |= Opcodes.ACC_SYNCHRONIZED;
        });
        return input;
    }

    @Override
    public  TransformerVoteResult castVote(ITransformerVotingContext context) {
        return TransformerVoteResult.YES;
    }

    @Override
    public Set<Target> targets() {
        syncFuTransformerLogger.warn("Targets 123");
        return Set.of(
            Target.targetClass("it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap"),
            Target.targetClass("it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet"),
            Target.targetClass("it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap"),
            Target.targetClass("it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap"),
            Target.targetClass("it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap"),
            Target.targetClass("it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet"),
            Target.targetClass("it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap$ValueIterator"),
            Target.targetClass("it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap$KeySet"),
            Target.targetClass("it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap$KeyIterator"),
            Target.targetClass("it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap$MapEntrySet"),
            Target.targetClass("it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap$EntryIterator"),
            Target.targetClass("it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap$MapIterator"),
            Target.targetClass("it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap$MapEntry"),
            Target.targetClass("it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap$FastEntryIterator"),
            Target.targetClass("it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap$MapIterator)"));
    }
}

