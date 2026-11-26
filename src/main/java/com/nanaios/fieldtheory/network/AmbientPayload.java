package com.nanaios.fieldtheory.network;

import com.nanaios.fieldtheory.api.FieldTheoryAPI;
import com.nanaios.fieldtheory.core.AmbientEntry;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Ambient field definitions from the server's data packs. */
public record AmbientPayload(List<AmbientEntry> entries) implements CustomPacketPayload {
    public static final Type<AmbientPayload> TYPE = new Type<>(FieldTheoryAPI.id("ambient"));
    public static final StreamCodec<FriendlyByteBuf, AmbientPayload> STREAM_CODEC = AmbientEntry.STREAM_CODEC
            .apply(ByteBufCodecs.list())
            .map(AmbientPayload::new, AmbientPayload::entries);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
