package com.nanaios.fieldtheory.core;

import com.nanaios.fieldtheory.api.FieldTheoryAPI;
import com.nanaios.fieldtheory.api.FieldType;
import com.nanaios.fieldtheory.api.interaction.FieldContext;
import com.nanaios.fieldtheory.api.interaction.FieldInteraction;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BuiltinInteractions {
    public static final DeferredRegister<FieldInteraction> REGISTER = DeferredRegister.create(FieldTheoryAPI.INTERACTION_REGISTRY, FieldTheoryAPI.MOD_ID);

    /**
     * The mechanical force of every field with a {@link com.nanaios.fieldtheory.api.Coupling}:
     * {@code a = sign · forceScale · (Q/m) · E}.
     */
    public static final DeferredHolder<FieldInteraction, FieldInteraction> FORCE = REGISTER.register("force", () -> BuiltinInteractions::force);

    private BuiltinInteractions() {
    }

    private static void force(FieldContext context) {
        double multiplier = FieldTheoryConfig.forceMultiplier();
        if (!context.controlsMotion() || multiplier == 0.0) {
            return;
        }
        for (FieldType type : FieldTheoryAPI.fieldTypes()) {
            double sign = type.coupling().sign();
            if (sign == 0.0 || type.forceScale() == 0.0 || !context.world().hasContent(type)) {
                continue;
            }
            double charge = context.charge(type);
            if (charge == 0.0) {
                continue;
            }
            Vec3 e = context.sample(type).vector();
            if (e.lengthSqr() < 1.0E-12) {
                continue;
            }
            context.accelerate(e.scale(multiplier * sign * type.forceScale() * charge / context.mass()));
        }
    }
}
