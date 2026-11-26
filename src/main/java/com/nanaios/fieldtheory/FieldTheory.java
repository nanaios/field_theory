package com.nanaios.fieldtheory;

import com.nanaios.fieldtheory.api.FieldTheoryAPI;
import com.nanaios.fieldtheory.api.FieldTypes;
import com.nanaios.fieldtheory.core.BuiltinInteractions;
import com.nanaios.fieldtheory.core.FieldAttachments;
import com.nanaios.fieldtheory.core.FieldDataMaps;
import com.nanaios.fieldtheory.core.FieldEvents;
import com.nanaios.fieldtheory.core.FieldRegistries;
import com.nanaios.fieldtheory.network.FieldNetwork;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Field Theory — a shared layer of physical fields. This mod adds no items: it provides the stage
 * (fields, sources, charges, interactions) on which other mods perform.
 */
@Mod(FieldTheoryAPI.MOD_ID)
public final class FieldTheory {

    public FieldTheory(IEventBus modBus) {
        modBus.addListener(FieldRegistries::onNewRegistry);
        modBus.addListener(FieldDataMaps::register);
        modBus.addListener(FieldNetwork::register);
        FieldTypes.REGISTER.register(modBus);
        BuiltinInteractions.REGISTER.register(modBus);
        FieldAttachments.REGISTER.register(modBus);
        FieldEvents.register(NeoForge.EVENT_BUS);
    }
}
