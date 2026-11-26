package com.nanaios.fieldtheory.network;

import com.nanaios.fieldtheory.api.FieldTheoryAPI;
import com.nanaios.fieldtheory.api.charge.EntityCharges;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Induced charges of one entity. */
public record EntityChargesPayload(int entityId, EntityCharges charges) implements CustomPacketPayload {
    public static final Type<EntityChargesPayload> TYPE = new Type<>(FieldTheoryAPI.id("entity_charges"));
    public static final StreamCodec<FriendlyByteBuf, EntityChargesPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, EntityChargesPayload::entityId,
            EntityCharges.STREAM_CODEC, EntityChargesPayload::charges,
            EntityChargesPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
