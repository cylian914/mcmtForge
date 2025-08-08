package net.tessa.mcmtforge.parallelised.threads;

import java.util.concurrent.ThreadFactory;

public class MCMTThreads {
    public static ThreadFactory createNamedVirtualThreadFactory(String name) {
        return Thread.ofVirtual()
                .name(name, 0)
                .factory();
    }

    public static ThreadFactory createNamedPlatformThreadFactory(String name) {
        return Thread.ofPlatform()
                .name(name, 0)
                .factory();
    }
}
