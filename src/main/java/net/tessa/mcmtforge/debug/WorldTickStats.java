package net.tessa.mcmtforge.debug;

import java.util.concurrent.ConcurrentLinkedQueue;

public class WorldTickStats {
    public ConcurrentLinkedQueue<Long> chunkTickTimesCurrent = new ConcurrentLinkedQueue<>();
    public ConcurrentLinkedQueue<Long> chunkTickTimesLast = new ConcurrentLinkedQueue<>();
    public ConcurrentLinkedQueue<Long> entityTickTimesCurrent = new ConcurrentLinkedQueue<>();
    public ConcurrentLinkedQueue<Long> entityTickTimesLast = new ConcurrentLinkedQueue<>();
    public ConcurrentLinkedQueue<Long> blockEntityTickTimesCurrent = new ConcurrentLinkedQueue<>();
    public ConcurrentLinkedQueue<Long> blockEntityTickTimesLast = new ConcurrentLinkedQueue<>();

    public void swapExecutionTimeBuffers() {
        // Swap chunk tick times
        ConcurrentLinkedQueue<Long> tempChunk = chunkTickTimesLast;
        chunkTickTimesLast = chunkTickTimesCurrent;
        chunkTickTimesCurrent = tempChunk;
        chunkTickTimesCurrent.clear();

        // Swap entity tick times
        ConcurrentLinkedQueue<Long> tempEntity = entityTickTimesLast;
        entityTickTimesLast = entityTickTimesCurrent;
        entityTickTimesCurrent = tempEntity;
        entityTickTimesCurrent.clear();

        // Swap block entity tick times
        ConcurrentLinkedQueue<Long> tempBlockEntity = blockEntityTickTimesLast;
        blockEntityTickTimesLast = blockEntityTickTimesCurrent;
        blockEntityTickTimesCurrent = tempBlockEntity;
        blockEntityTickTimesCurrent.clear();
    }
}