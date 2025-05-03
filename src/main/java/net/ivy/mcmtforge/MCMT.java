package net.ivy.mcmtforge;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import net.ivy.mcmtforge.commands.ConfigCommand;
import net.ivy.mcmtforge.commands.StatsCommand;
import net.ivy.mcmtforge.config.GeneralConfig;
import net.ivy.mcmtforge.jmx.JMXRegistration;
import net.ivy.mcmtforge.serdes.SerDesRegistry;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(MCMT.MODID)
public class MCMT {
    public static final String MODID = "mcmtforge";
    public static final Logger LOGGER = LogManager.getLogger();
    public static GeneralConfig config;

    public MCMT(FMLJavaModLoadingContext context) {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.
        LOGGER.info("Initializing MCMTFabric...");
        ConfigHolder<GeneralConfig> holder = AutoConfig.register(GeneralConfig.class, Toml4jConfigSerializer::new);
        holder.registerLoadListener((manager, data) -> {
            holder.getConfig().loadTELists();
            return InteractionResult.SUCCESS;
        });
        holder.load();  // Load again to run loadTELists() handler
        config = holder.getConfig();

        if (System.getProperty("jmt.mcmt.jmx") != null) {
            JMXRegistration.register();
        }

        StatsCommand.runDataThread();
        SerDesRegistry.init();


        LOGGER.info("MCMT Setting up threadpool...");
        ParallelProcessor.setupThreadPool(GeneralConfig.getParallelism());


        // Listener reg begin
        //ServerLifecycleEvents.SERVER_STARTED.register(server -> StatsCommand.resetAll());
        //CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> ConfigCommand.register(dispatcher));
        MinecraftForge.EVENT_BUS.addListener(this::resetStats);
        MinecraftForge.EVENT_BUS.addListener(this::registerCommand);
    }

    @SubscribeEvent
    public void resetStats(ServerStartedEvent e) {
        StatsCommand.resetAll();
    }

    @SubscribeEvent
    public void registerCommand(RegisterCommandsEvent e) {
        ConfigCommand.register(e.getDispatcher());
    }
}
