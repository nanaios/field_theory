package com.nanaios.fieldtheory.network;

import com.nanaios.fieldtheory.api.FieldRegion;
import com.nanaios.fieldtheory.api.FieldSource;
import com.nanaios.fieldtheory.api.FieldTheoryAPI;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Changes of one server tick, broadcast to every player in the dimension. */
public record FieldDeltaPayload(List<FieldSource> putSources, List<UUID> removedSources,
                                List<FieldRegion> putRegions, List<UUID> removedRegions) implements CustomPacketPayload {
    public static final Type<FieldDeltaPayload> TYPE = new Type<>(FieldTheoryAPI.id("delta"));
    public static final StreamCodec<FriendlyByteBuf, FieldDeltaPayload> STREAM_CODEC = StreamCodec.composite(
            FieldSource.STREAM_CODEC.apply(ByteBufCodecs.list()), FieldDeltaPayload::putSources,
            UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs.list()), FieldDeltaPayload::removedSources,
            FieldRegion.STREAM_CODEC.apply(ByteBufCodecs.list()), FieldDeltaPayload::putRegions,
            UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs.list()), FieldDeltaPayload::removedRegions,
            FieldDeltaPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
