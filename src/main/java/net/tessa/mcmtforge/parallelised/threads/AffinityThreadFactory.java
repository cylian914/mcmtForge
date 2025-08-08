package net.tessa.mcmtforge.parallelised.threads;

import net.openhft.affinity.AffinityLock;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class AffinityThreadFactory implements ThreadFactory {
    private final String prefix;
    private final AtomicInteger count = new AtomicInteger();
    private final List<Integer> assignedCpuCores = new CopyOnWriteArrayList<>();

    public AffinityThreadFactory(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public Thread newThread(Runnable r) {
        int cpuCore = CPUCoreManager.acquireCore(prefix);
        if (cpuCore == -1) {
            throw new RuntimeException("No available CPU cores for thread affinity");
        }
        assignedCpuCores.add(cpuCore);

        Thread thread = new Thread(r) {
            private final int assignedCpuCore = cpuCore;
            private AffinityLock affinityLock;

            @Override
            public void run() {
                try {
                    affinityLock = AffinityLock.acquireLock(assignedCpuCore);
                    super.run();
                } finally {
                    if (affinityLock != null) {
                        affinityLock.release();
                    }
                    // Core release is managed globally on shutdown
                    CPUCoreManager.releaseCore(assignedCpuCore, prefix);
                    assignedCpuCores.remove(Integer.valueOf(assignedCpuCore));
                }
            }
        };

        thread.setName(prefix + "-" + count.incrementAndGet());
        thread.setDaemon(true);
        return thread;
    }

    public void releaseAllCores() {
        for (int core : assignedCpuCores) {
            CPUCoreManager.releaseCore(core, prefix);
        }
        assignedCpuCores.clear();
    }
}
