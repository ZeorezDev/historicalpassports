package historicalpassports.client;

import historicalpassports.HistoricalPassports;
import historicalpassports.client.gui.PassportScreen;
import historicalpassports.network.PassportNetwork;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = HistoricalPassports.ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientEvents {
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> PassportNetwork.clientReceiver = data -> {
            Minecraft mc = Minecraft.getInstance();
            if(data.getString("screen").equals("visa")) { if(mc.screen instanceof historicalpassports.client.gui.VisaScreen v) v.update(data); else mc.setScreen(new historicalpassports.client.gui.VisaScreen(data)); return; }
            if (mc.screen instanceof PassportScreen screen && screen.documentId().equals(data.getString("id"))) screen.update(data);
            else mc.setScreen(new PassportScreen(data));
        });
    }
    private ClientEvents() {}
}
