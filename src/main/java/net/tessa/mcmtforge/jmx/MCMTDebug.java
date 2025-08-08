package net.tessa.mcmtforge.jmx;

//import net.fabricmc.loader.api.FabricLoader;
//import net.fabricmc.loader.api.ModContainer;
//import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraftforge.fml.ModList;
import net.tessa.mcmtforge.DebugHookTerminator;
import net.minecraft.world.level.ChunkPos;
import javax.management.AttributeChangeNotification;
import javax.management.MBeanNotificationInfo;
import javax.management.NotificationBroadcasterSupport;

/**
 * MBean for debugging modlaunching issues
 *
 * @author jediminer543
 */
public class MCMTDebug extends NotificationBroadcasterSupport implements MCMTDebugMBean {

    ClassLoader ccl = null;

    public MCMTDebug() {
        // needed because classloading issues; classload all TRANSFORMER classes before stuff
        ccl = Thread.currentThread().getContextClassLoader();
    }

    @Override
    public String[] getLoadedMods() {
        Thread.currentThread().setContextClassLoader(ccl);
        return ModList.get().getMods().stream().map(mod -> mod.getModId() + ":" +  mod.getVersion().toString()).toArray(String[]::new);
        //return FabricLoader.getInstance().getAllMods().stream().map(ModContainer::getMetadata).map(info -> info.getId() + ":" + info.getVersion().toString()).toArray(String[]::new);
    }

    @Override
    public String getMainChunkLoadStatus() {
        Thread.currentThread().setContextClassLoader(ccl);
        return DebugHookTerminator.mainThreadChunkLoad.get() + ":" + DebugHookTerminator.mainThreadChunkLoadCount.get();
    }

    @Override
    public String[] getBrokenChunkList() {
        Thread.currentThread().setContextClassLoader(ccl);
        return DebugHookTerminator.breaks.stream().map(bcl -> new ChunkPos(bcl.getChunkPos()).toString()).toArray(String[]::new);
    }

    @Override
    public MBeanNotificationInfo[] getNotificationInfo() {
        String[] types = new String[]{
                AttributeChangeNotification.ATTRIBUTE_CHANGE
        };

        String name = AttributeChangeNotification.class.getName();
        String description = "An attribute of this MBean has changed";
        MBeanNotificationInfo info =
                new MBeanNotificationInfo(types, name, description);
        return new MBeanNotificationInfo[]{info};
    }

}
