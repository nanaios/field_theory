package com.nanaios.fieldtheory.network;

import com.nanaios.fieldtheory.api.FieldRegion;
import com.nanaios.fieldtheory.api.FieldSource;
import com.nanaios.fieldtheory.api.FieldTheoryAPI;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Full state of the player's current dimension (sent on login / dimension change). */
public record FieldSnapshotPayload(List<FieldSource> sources, List<FieldRegion> regions) implements CustomPacketPayload {
    public static final Type<FieldSnapshotPayload> TYPE = new Type<>(FieldTheoryAPI.id("snapshot"));
    public static final StreamCodec<FriendlyByteBuf, FieldSnapshotPayload> STREAM_CODEC = StreamCodec.composite(
            FieldSource.STREAM_CODEC.apply(ByteBufCodecs.list()), FieldSnapshotPayload::sources,
            FieldRegion.STREAM_CODEC.apply(ByteBufCodecs.list()), FieldSnapshotPayload::regions,
            FieldSnapshotPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
