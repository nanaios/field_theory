package com.nanaios.fieldtheory.core;

import com.nanaios.fieldtheory.api.FieldTheoryAPI;
import com.nanaios.fieldtheory.api.FieldType;
import com.nanaios.fieldtheory.api.interaction.FieldInteraction;
import net.minecraft.core.Registry;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

public final class FieldRegistries {
    public static final Registry<FieldType> FIELD_TYPES = new RegistryBuilder<>(FieldTheoryAPI.FIELD_TYPE_REGISTRY)
            .sync(true)
            .create();

    public static final Registry<FieldInteraction> INTERACTIONS = new RegistryBuilder<>(FieldTheoryAPI.INTERACTION_REGISTRY)
            .create();

    private FieldRegistries() {
    }

    public static void onNewRegistry(NewRegistryEvent event) {
        event.register(FIELD_TYPES);
        event.register(INTERACTIONS);
    }
}
