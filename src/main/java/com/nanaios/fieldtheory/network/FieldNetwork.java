package com.nanaios.fieldtheory.network;

import com.nanaios.fieldtheory.core.AmbientFields;
import com.nanaios.fieldtheory.core.ChargeManager;
import com.nanaios.fieldtheory.core.FieldWorlds;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Server → client synchronisation. Handlers run on the main thread and only touch common code, so
 * this class is safe to load on a dedicated server.
 */
public final class FieldNetwork {
    public static final String VERSION = "2";

    private FieldNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VERSION);
        registrar.playToClient(FieldSnapshotPayload.TYPE, FieldSnapshotPayload.STREAM_CODEC,
                (payload, context) -> FieldWorlds.get(context.player().level()).applySnapshot(payload.sources(), payload.regions()));
        registrar.playToClient(FieldDeltaPayload.TYPE, FieldDeltaPayload.STREAM_CODEC,
                (payload, context) -> FieldWorlds.get(context.player().level()).applyDelta(payload));
        registrar.playToClient(EntityChargesPayload.TYPE, EntityChargesPayload.STREAM_CODEC, (payload, context) -> {
            Entity entity = context.player().level().getEntity(payload.entityId());
            if (entity != null) {
                ChargeManager.applyClient(entity, payload.charges());
            }
        });
        registrar.playToClient(AmbientPayload.TYPE, AmbientPayload.STREAM_CODEC,
                (payload, context) -> AmbientFields.setClient(payload.entries()));
    }
}
