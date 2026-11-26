package com.nanaios.fieldtheory.core;

import com.nanaios.fieldtheory.api.FieldType;
import com.nanaios.fieldtheory.api.charge.Charges;
import com.nanaios.fieldtheory.api.charge.EntityCharges;
import com.nanaios.fieldtheory.network.EntityChargesPayload;
import javax.annotation.Nullable;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;

/** Mutates induced charges and keeps emitter tracking and clients in sync. */
public final class ChargeManager {
    private static final double EPSILON = 1.0E-9;

    private ChargeManager() {
    }

    public static void induce(Entity entity, FieldType type, double amount, int durationTicks, boolean additive) {
        requireServer(entity);
        long now = entity.level().getGameTime();
        EntityCharges current = Charges.induced(entity).purge(now);
        long expiresAt = durationTicks < 0 ? -1L : now + durationTicks;
        double value = amount;
        EntityCharges.Charge old = current.charges().get(type);
        if (additive && old != null) {
            value += old.value();
            expiresAt = (old.expiresAt() < 0 || expiresAt < 0) ? -1L : Math.max(old.expiresAt(), expiresAt);
        }
        EntityCharges next = Math.abs(value) < EPSILON
                ? current.without(type)
                : current.with(type, new EntityCharges.Charge(value, expiresAt));
        set(entity, next, true);
    }

    public static void clear(Entity entity, @Nullable FieldType type) {
        requireServer(entity);
        EntityCharges current = Charges.induced(entity);
        set(entity, type == null ? EntityCharges.EMPTY : current.without(type), true);
    }

    /** Applies charges received from the server. */
    public static void applyClient(Entity entity, EntityCharges charges) {
        set(entity, charges, false);
    }

    static void purgeExpired(FieldWorldImpl world, long now) {
        for (Entity entity : world.emitterEntities()) {
            EntityCharges current = Charges.induced(entity);
            EntityCharges purged = current.purge(now);
            if (purged != current) {
                set(entity, purged, true);
            }
        }
    }

    private static void set(Entity entity, EntityCharges next, boolean broadcast) {
        if (next.isEmpty()) {
            if (entity.hasData(FieldAttachments.ENTITY_CHARGES)) {
                entity.removeData(FieldAttachments.ENTITY_CHARGES);
            }
        } else {
            entity.setData(FieldAttachments.ENTITY_CHARGES, next);
        }
        FieldWorlds.get(entity.level()).updateEmitter(entity);
        if (broadcast && !entity.level().isClientSide()) {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, new EntityChargesPayload(entity.getId(), next));
        }
    }

    private static void requireServer(Entity entity) {
        if (entity.level().isClientSide()) {
            throw new IllegalStateException("Charges can only be changed on the logical server");
        }
    }
}
