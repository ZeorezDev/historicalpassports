package historicalpassports;

import historicalpassports.command.PassportCommands;
import historicalpassports.config.PassportConfig;
import historicalpassports.network.PassportNetwork;
import historicalpassports.passport.PassportService;
import historicalpassports.registry.ModRegistry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraft.server.level.ServerPlayer;

@Mod(HistoricalPassports.ID)
public final class HistoricalPassports {
    public static final String ID = "historicalpassports";
    public HistoricalPassports() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModRegistry.register(bus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, PassportConfig.SERVER);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, PassportConfig.CLIENT);
        PassportNetwork.register();
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.server.ServerStartingEvent e) -> historicalpassports.visa.VisaRules.load(e.getServer()));
        MinecraftForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> { PassportCommands.register(e.getDispatcher()); historicalpassports.visa.VisaCommands.register(e.getDispatcher()); });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer player) PassportService.deliverMail(player);
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> PassportService.close(e.getEntity().getUUID()));
    }
}
