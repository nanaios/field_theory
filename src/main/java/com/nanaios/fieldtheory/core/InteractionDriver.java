package com.nanaios.fieldtheory.core;

import com.nanaios.fieldtheory.api.event.FieldForceEvent;
import com.nanaios.fieldtheory.api.interaction.FieldInteraction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Runs every registered {@link FieldInteraction} for every entity, every tick. */
public final class InteractionDriver {
    /** Upper bound of field-induced acceleration per tick (blocks/tick²). */
    public static final double MAX_ACCELERATION = 0.5;

    private static final FieldContextImpl SERVER_CONTEXT = new FieldContextImpl();
    private static final FieldContextImpl CLIENT_CONTEXT = new FieldContextImpl();

    private InteractionDriver() {
    }

    static void onEntityTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (entity.isRemoved() || entity.isSpectator()) {
            return;
        }
        Level level = entity.level();
        boolean serverSide = !level.isClientSide();
        if (!serverSide && !(entity instanceof Player player && player.isLocalPlayer())) {
            return;
        }
        FieldWorldImpl world = FieldWorlds.get(level);
        if (world.isQuiet()) {
            return;
        }
        // Players move themselves: their motion is integrated on their own client.
        boolean controlsMotion = !serverSide || !(entity instanceof Player);
        FieldContextImpl context = serverSide ? SERVER_CONTEXT : CLIENT_CONTEXT;
        context.reset(world, entity, serverSide, controlsMotion);
        try {
            for (FieldInteraction interaction : FieldRegistries.INTERACTIONS) {
                interaction.apply(context);
            }
            if (controlsMotion) {
                applyMotion(entity, context);
            }
        } finally {
            context.release();
        }
    }

    private static void applyMotion(Entity entity, FieldContextImpl context) {
        if (entity instanceof Player player && player.getAbilities().flying) {
            return;
        }
        boolean changed = false;
        Vec3 acceleration = context.acceleration();
        double lengthSqr = acceleration.lengthSqr();
        if (lengthSqr > 1.0E-10) {
            if (lengthSqr > MAX_ACCELERATION * MAX_ACCELERATION) {
                acceleration = acceleration.scale(MAX_ACCELERATION / Math.sqrt(lengthSqr));
            }
            FieldForceEvent event = NeoForge.EVENT_BUS.post(new FieldForceEvent(entity, acceleration));
            if (!event.isCanceled()) {
                entity.setDeltaMovement(entity.getDeltaMovement().add(event.getAcceleration()));
                changed = true;
            }
        }
        double damping = context.damping();
        if (damping < 1.0) {
            entity.setDeltaMovement(entity.getDeltaMovement().scale(damping));
            changed = true;
        }
        if (changed && !(entity instanceof LivingEntity)) {
            // Items, projectiles, minecarts... are simulated by clients too: resync their motion.
            entity.hasImpulse = true;
        }
    }
}
