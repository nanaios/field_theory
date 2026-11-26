package com.nanaios.fieldtheory.core;

import com.nanaios.fieldtheory.api.FieldTheoryAPI;
import com.nanaios.fieldtheory.api.charge.BaseCharges;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

public final class FieldDataMaps {
    /** {@code data/<ns>/data_maps/entity_type/base_charges.json} */
    public static final DataMapType<EntityType<?>, BaseCharges> BASE_CHARGES = DataMapType
            .builder(FieldTheoryAPI.id("base_charges"), Registries.ENTITY_TYPE, BaseCharges.CODEC)
            .synced(BaseCharges.CODEC, false)
            .build();

    private FieldDataMaps() {
    }

    public static void register(RegisterDataMapTypesEvent event) {
        event.register(BASE_CHARGES);
    }
}
