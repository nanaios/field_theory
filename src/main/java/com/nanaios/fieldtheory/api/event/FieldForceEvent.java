package com.nanaios.fieldtheory.api.event;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.event.entity.EntityEvent;

/**
 * Fired on {@code NeoForge.EVENT_BUS} right before the field-induced acceleration of a tick is
 * applied to an entity, on the side that owns the entity's motion. Modify or cancel it to make
 * something immune to (or more sensitive to) fields.
 */
public class FieldForceEvent extends EntityEvent implements ICancellableEvent {
    private Vec3 acceleration;

    public FieldForceEvent(Entity entity, Vec3 acceleration) {
        super(entity);
        this.acceleration = acceleration;
    }

    public Vec3 getAcceleration() {
        return acceleration;
    }

    public void setAcceleration(Vec3 acceleration) {
        this.acceleration = acceleration;
    }
}
