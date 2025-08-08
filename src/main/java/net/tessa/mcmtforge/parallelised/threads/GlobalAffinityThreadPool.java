package net.tessa.mcmtforge.parallelised.threads;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class GlobalAffinityThreadPool {
    private static ThreadPoolExecutor affinityWorldAndRegionPool;
    private static ThreadPoolExecutor affinitySharedPool;
    private static final Object poolSizeLock = new Object();


    public static synchronized ExecutorService getAffinityWorldAndRegionPool() {
        if (affinityWorldAndRegionPool == null || affinityWorldAndRegionPool.isShutdown()) {
            int totalCores = Runtime.getRuntime().availableProcessors();

            AffinityThreadFactory threadFactory = new AffinityThreadFactory("MCMT-AffWorldRegionThread");

            affinityWorldAndRegionPool = new ThreadPoolExecutor(
                    1,
                    1,
                    0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>(),
                    threadFactory,
                    new ThreadPoolExecutor.AbortPolicy()
            );
        }
        return affinityWorldAndRegionPool;
    }

    public static synchronized ExecutorService getAffinitySharedPool() {
        if (affinitySharedPool == null || affinitySharedPool.isShutdown()) {
            int totalCores = Runtime.getRuntime().availableProcessors();

            AffinityThreadFactory threadFactory = new AffinityThreadFactory("MCMT-AffSharedThread");

            affinitySharedPool = new ThreadPoolExecutor(
                    totalCores - 2,
                    totalCores - 2,
                    0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>(),
                    threadFactory,
                    new ThreadPoolExecutor.AbortPolicy()
            );
        }
        return affinitySharedPool;
    }

    public static void decreaseWorldAndRegionPoolSize() {
        synchronized (poolSizeLock) {
            if (affinityWorldAndRegionPool != null) {
                int targetSize = affinityWorldAndRegionPool.getCorePoolSize() - 1;

                // First decrease the pool sizes
                affinityWorldAndRegionPool.setCorePoolSize(targetSize);
                affinityWorldAndRegionPool.setMaximumPoolSize(targetSize);

                // Wait for active thread count to reach the new target
                while (affinityWorldAndRegionPool.getActiveCount() > targetSize) {
                    try {
                        Thread.sleep(2); // Short sleep to prevent busy waiting
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }

                // Now it's safe to increase the shared pool
                if (affinitySharedPool != null) {
                    increaseSharedPoolSize();
                }
            }
        }
    }

    public static void increaseWorldAndRegionPoolSize() {
        synchronized (poolSizeLock) {
            if (affinityWorldAndRegionPool != null) {
                if (affinitySharedPool != null) {
                    // First decrease shared pool
                    decreaseSharedPoolSize();

                    // Wait for shared pool to properly decrease
                    int targetSharedSize = affinitySharedPool.getCorePoolSize();
                    while (affinitySharedPool.getActiveCount() > targetSharedSize) {
                        try {
                            Thread.sleep(2);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                }

                // Now safely increase WorldAndRegion pool
                int newSize = affinityWorldAndRegionPool.getCorePoolSize() + 1;
                affinityWorldAndRegionPool.setMaximumPoolSize(newSize);
                affinityWorldAndRegionPool.setCorePoolSize(newSize);
            }
        }
    }

    private static void increaseSharedPoolSize() {
        synchronized (poolSizeLock) {
            if (affinitySharedPool != null) {
                int newSize = affinitySharedPool.getMaximumPoolSize() + 1;
                affinitySharedPool.setMaximumPoolSize(newSize);
                affinitySharedPool.setCorePoolSize(newSize);
            }
        }
    }

    private static void decreaseSharedPoolSize() {
        synchronized (poolSizeLock) {
            if (affinitySharedPool != null) {
                int newSize = affinitySharedPool.getCorePoolSize() - 1;
                affinitySharedPool.setCorePoolSize(newSize);
                affinitySharedPool.setMaximumPoolSize(newSize);
            }
        }
    }


    public static void shutdown() {
        if (affinityWorldAndRegionPool != null) {
            affinityWorldAndRegionPool.shutdown();
            try {
                if (!affinityWorldAndRegionPool.awaitTermination(5, TimeUnit.SECONDS)) {
                    affinityWorldAndRegionPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                affinityWorldAndRegionPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
            if (affinityWorldAndRegionPool.getThreadFactory() instanceof AffinityThreadFactory factory) {
                factory.releaseAllCores();
            }
        }

        if (affinitySharedPool != null) {
            affinitySharedPool.shutdown();
            try {
                if (!affinitySharedPool.awaitTermination(5, TimeUnit.SECONDS)) {
                    affinitySharedPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                affinitySharedPool.shutdownNow();
                Thread.currentThread().interrupt();
            }

            if (affinitySharedPool.getThreadFactory() instanceof AffinityThreadFactory factory) {
                factory.releaseAllCores();
            }
        }
    }
}
