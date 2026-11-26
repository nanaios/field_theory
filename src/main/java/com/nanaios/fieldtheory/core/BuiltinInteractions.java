package com.nanaios.fieldtheory.core;

import com.nanaios.fieldtheory.api.FieldTheoryAPI;
import com.nanaios.fieldtheory.api.FieldType;
import com.nanaios.fieldtheory.api.charge.Charges;
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

    /**
     * Induced-dipole force on neutral conductors (dielectrophoresis):
     * {@code a = forceScale · α · ∇|E|² / (2m)} — always towards the stronger field, whatever the
     * sign of the sources. ∇|E|² is taken by central differences.
     */
    public static final DeferredHolder<FieldInteraction, FieldInteraction> POLARIZATION = REGISTER.register("polarization", () -> BuiltinInteractions::polarization);

    private static final double GRADIENT_STEP = 0.25;

    private BuiltinInteractions() {
    }

    private static void polarization(FieldContext context) {
        double multiplier = FieldTheoryConfig.forceMultiplier();
        if (!context.controlsMotion() || multiplier == 0.0) {
            return;
        }
        for (FieldType type : FieldTheoryAPI.fieldTypes()) {
            if (type.forceScale() == 0.0) {
                continue;
            }
            double alpha = Charges.polarizability(context.entity(), type);
            if (alpha == 0.0 || !context.world().hasContent(type)) {
                continue;
            }
            Vec3 c = context.center();
            double h = GRADIENT_STEP;
            double gx = (e2(context, type, c.add(h, 0, 0)) - e2(context, type, c.add(-h, 0, 0))) / (2 * h);
            double gy = (e2(context, type, c.add(0, h, 0)) - e2(context, type, c.add(0, -h, 0))) / (2 * h);
            double gz = (e2(context, type, c.add(0, 0, h)) - e2(context, type, c.add(0, 0, -h))) / (2 * h);
            double k = multiplier * type.forceScale() * alpha / (2.0 * context.mass());
            context.accelerate(new Vec3(gx * k, gy * k, gz * k));
        }
    }

    private static double e2(FieldContext context, FieldType type, Vec3 at) {
        return context.world().sample(type, at, context.entity()).vector().lengthSqr();
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
