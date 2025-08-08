package net.tessa.mcmtforge.parallelised.threads;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CPUCoreManager {
    private static final Set<Integer> usedCores = ConcurrentHashMap.newKeySet();

    public static synchronized int acquireCore(String poolName) {
        int totalCores = Runtime.getRuntime().availableProcessors();
        for (int i = 0; i < totalCores; i++) {
            if (!usedCores.contains(i)) {
                usedCores.add(i);
                return i;
            }
        }
        // No available cores
        return -1;
    }

    public static synchronized void releaseCore(int core, String poolName) {
        usedCores.remove(core);
    }

    public static synchronized int getUsedCoreCount() {
        return usedCores.size();
    }
}
