package net.ivy.mcmtforge.syncfu;

import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.ITransformer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.Map.Entry;

// code by jediminer543
/*
public class SyncFuTransformer implements ITransformer<ClassNode>, ITransformationService {
    private static final Logger syncFuTransformerLogger = LogManager.getLogger();

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Marker M_LOCATOR = MarkerManager.getMarker("LOCATE");
    private boolean isActive = true;

    @Override
    public String name() {
        return "sync_fu";
    }

    @Override
    public Entry<Set<String>, Supplier<Function<String, Optional<URL>>>> additionalClassesLocator() {
        // get FastUtil jar
        Optional<URL> fujarurl = Arrays.stream(System.getProperty("java.class.path").split(File.pathSeparator)).flatMap(path -> {
            File file = new File(path);
            if (file.isDirectory()) {
                return Arrays.stream(file.list((d, n) -> n.endsWith(".jar")));
            }
            return Arrays.stream(new String[]{path});
        })
        .map(Paths::get)
        .map(path -> { try { return path.toUri().toURL();} catch (Exception e) { return null;}}).findFirst();
        URL rootUrl = null;
        try {
            rootUrl = fujarurl.get();
        } catch (Exception e) {
            LOGGER.warn("Failed to find FastUtil jar; this WILL result in more exceptions");
            isActive = false;
        }
        LOGGER.info("Found FU jar at {}", fujarurl.get());
        if (!isActive) {
            // We are dead
            return null;
        }
        final URL rootURLf = rootUrl;
        return new Entry<Set<String>, Supplier<Function<String, Optional<URL>>>>() {

            @Override
            public Set<String> getKey() {
                Set<String> out = new HashSet<String>();
                out.add("it.unimi.dsi.fastutil.");
                return out;
            }

            @Override
            public Supplier<Function<String, Optional<URL>>> getValue() {
                return () -> {
                    return s -> {
                        URL urlOut;
                        try {
                            urlOut = new URL("jar:" + rootURLf.toString() + "!/" + s);
                            //LOGGER.debug(urlOut.toString());
                            return Optional.of(urlOut);
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                            return null;
                        }
                    };
                };
            }

            @Override
            public Supplier<Function<String, Optional<URL>>> setValue(Supplier<Function<String, Optional<URL>>> value) {
                throw new IllegalStateException();
            }

        };
    }




    @Override
    public void onPreLaunch() {
        syncFuTransformerLogger.info("On SyncFuTransformer PreLaunch...");
        try {
            FabricLauncherBase.getLauncher().loadIntoTarget("it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap");
            FabricLauncherBase.getLauncher().loadIntoTarget("it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet");
            FabricLauncherBase.getLauncher().loadIntoTarget("it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap");
            FabricLauncherBase.getLauncher().loadIntoTarget("it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap");
            FabricLauncherBase.getLauncher().loadIntoTarget("it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap");
            FabricLauncherBase.getLauncher().loadIntoTarget("it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet");
            FabricLauncherBase.getLauncher().loadIntoTarget("it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap$ValueIterator");
            FabricLauncherBase.getLauncher().loadIntoTarget("it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap$KeySet");
            FabricLauncherBase.getLauncher().loadIntoTarget("it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap$KeyIterator");
            FabricLauncherBase.getLauncher().loadIntoTarget("it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap$MapEntrySet");
            FabricLauncherBase.getLauncher().loadIntoTarget("it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap$EntryIterator");
            FabricLauncherBase.getLauncher().loadIntoTarget("it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap$MapIterator");
            FabricLauncherBase.getLauncher().loadIntoTarget("it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap$MapEntry");
            FabricLauncherBase.getLauncher().loadIntoTarget("it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap$FastEntryIterator");
            FabricLauncherBase.getLauncher().loadIntoTarget("it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap$MapIterator");
//            FabricLauncherBase..getLauncher().loadIntoTarget("it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap$FastEntryIterator");  Error, is a interface
//            FabricLauncherBase..getLauncher().loadIntoTarget("it.unimi.dsi.fastutil.longs.Long2ObjectMap$FastEntrySet");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

}
*/
