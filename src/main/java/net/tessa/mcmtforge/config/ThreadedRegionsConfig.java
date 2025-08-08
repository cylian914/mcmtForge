package net.tessa.mcmtforge.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import net.tessa.mcmtforge.parallelised.threads.ThreadedChunksRegion;

import java.util.ArrayList;
import java.util.List;

@Config(name = "mcmt-threaded-regions")
public class ThreadedRegionsConfig implements ConfigData {
    public List<ThreadedChunksRegion> threadedChunksRegions = new ArrayList<>();
}
