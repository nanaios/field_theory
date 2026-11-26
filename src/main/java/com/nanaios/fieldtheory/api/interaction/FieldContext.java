package com.nanaios.fieldtheory.api.interaction;

import com.nanaios.fieldtheory.api.FieldSample;
import com.nanaios.fieldtheory.api.FieldType;
import com.nanaios.fieldtheory.api.FieldWorld;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Per-entity, per-tick view handed to {@link FieldInteraction}s. Samples are cached for the tick. */
public interface FieldContext {

    Entity entity();

    Level level();

    FieldWorld world();

    /** {@code true} on the logical server. */
    boolean isServerSide();

    /** {@code true} if accelerations applied in this context will move the entity. */
    boolean controlsMotion();

    /** Centre of mass of the entity. */
    Vec3 center();

    double mass();

    /** Total charge of the entity for {@code type}. */
    double charge(FieldType type);

    /** The field at the entity, excluding the entity's own contribution. */
    FieldSample sample(FieldType type);

    /** Adds an acceleration (blocks/tick²) to be applied at the end of the tick. */
    void accelerate(Vec3 acceleration);

    /** Multiplies the entity's velocity by {@code factor} at the end of the tick (drag). */
    void damp(double factor);

    /** Sum of accelerations requested so far. */
    Vec3 acceleration();
}
