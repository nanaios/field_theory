package com.nanaios.fieldtheory.core;

import com.nanaios.fieldtheory.api.FieldTheoryAPI;
import com.nanaios.fieldtheory.api.charge.BaseCharges;
import com.mojang.serialization.Codec;
import com.nanaios.fieldtheory.api.FieldType;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

public final class FieldDataMaps {
    /** {@code data/<ns>/data_maps/entity_type/base_charges.json} */
    public static final DataMapType<EntityType<?>, BaseCharges> BASE_CHARGES = DataMapType
            .builder(FieldTheoryAPI.id("base_charges"), Registries.ENTITY_TYPE, BaseCharges.CODEC)
            .synced(BaseCharges.CODEC, false)
            .build();

    /**
     * {@code data/<ns>/data_maps/item/polarizability.json}: how strongly a dropped stack of the
     * item is pulled into strong fields (metals are conductors).
     */
    public static final Codec<Map<FieldType, Double>> POLARIZABILITY_CODEC = Codec.unboundedMap(FieldType.CODEC, Codec.DOUBLE);
    public static final DataMapType<Item, Map<FieldType, Double>> ITEM_POLARIZABILITY = DataMapType
            .builder(FieldTheoryAPI.id("polarizability"), Registries.ITEM, POLARIZABILITY_CODEC)
            .synced(POLARIZABILITY_CODEC, false)
            .build();

    private FieldDataMaps() {
    }

    public static void register(RegisterDataMapTypesEvent event) {
        event.register(BASE_CHARGES);
        event.register(ITEM_POLARIZABILITY);
    }
}
