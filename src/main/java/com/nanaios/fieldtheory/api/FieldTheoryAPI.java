package com.nanaios.fieldtheory.api;

import com.nanaios.fieldtheory.api.interaction.FieldInteraction;
import com.nanaios.fieldtheory.core.FieldRegistries;
import com.nanaios.fieldtheory.core.FieldWorlds;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * Entry point of the Field Theory API.
 *
 * <p>Field Theory models the world as a superposition of <em>fields</em>. Every field has a
 * {@link FieldType} which defines how it propagates (its Green's function, see {@link Propagator})
 * and how it couples to matter (see {@link Coupling}). The value of a field at a point is
 *
 * <pre>
 *   φ(x) = G(x) · ( φ_ambient(x) + Σ_i k · q_i · P(|x - x_i|) )
 *   E(x) = G(x) · Σ_i k · q_i · P'(|x - x_i|) · (x - x_i)/|x - x_i|
 * </pre>
 *
 * where the sum runs over point {@link FieldSource sources} and charged entities ("emitters"),
 * {@code G} is the product of all {@link FieldRegion region} gains and {@code φ_ambient} is the
 * data-driven background value of the dimension. Use {@link #get(Level)} to obtain the
 * {@link FieldWorld} of a level; it works on both the logical server and the client.</p>
 */
public final class FieldTheoryAPI {
    public static final String MOD_ID = "field_theory";

    /** Registry of field types. Register your own with {@code DeferredRegister.create(FIELD_TYPE_REGISTRY, modid)}. */
    public static final ResourceKey<Registry<FieldType>> FIELD_TYPE_REGISTRY =
            ResourceKey.createRegistryKey(id("field_type"));

    /** Registry of field interactions (per-entity rules evaluated every tick). */
    public static final ResourceKey<Registry<FieldInteraction>> INTERACTION_REGISTRY =
            ResourceKey.createRegistryKey(id("interaction"));

    private FieldTheoryAPI() {
    }

    /** Returns the field world of the given level (server or client). */
    public static FieldWorld get(Level level) {
        return FieldWorlds.get(level);
    }

    public static Registry<FieldType> fieldTypes() {
        return FieldRegistries.FIELD_TYPES;
    }

    public static Registry<FieldInteraction> interactions() {
        return FieldRegistries.INTERACTIONS;
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
