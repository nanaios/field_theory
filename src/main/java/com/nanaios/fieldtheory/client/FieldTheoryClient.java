package com.nanaios.fieldtheory.client;

import com.nanaios.fieldtheory.api.FieldTheoryAPI;
import com.nanaios.fieldtheory.core.AmbientFields;
import com.nanaios.fieldtheory.core.FieldWorlds;
import java.util.List;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = FieldTheoryAPI.MOD_ID, dist = Dist.CLIENT)
public final class FieldTheoryClient {

    public FieldTheoryClient(IEventBus modBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        NeoForge.EVENT_BUS.addListener(FieldTheoryClient::onLoggingOut);
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        FieldWorlds.clearClient();
        AmbientFields.setClient(List.of());
    }
}
