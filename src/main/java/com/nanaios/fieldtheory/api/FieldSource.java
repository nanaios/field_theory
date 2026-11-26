package com.nanaios.fieldtheory.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A point charge that sources a field.
 *
 * @param id        stable identity; putting a source with an existing id replaces it
 * @param type      the field it sources
 * @param position  where it sits
 * @param charge    signed strength
 * @param expiresAt game time at which it disappears, or {@code -1} for a permanent source
 * @param origin    free-form tag describing who created it (e.g. {@code field_arcana:staff})
 */
public record FieldSource(UUID id, FieldType type, Vec3 position, double charge, long expiresAt, ResourceLocation origin) {
    public static final ResourceLocation UNKNOWN_ORIGIN = FieldTheoryAPI.id("unknown");

    public static final Codec<FieldSource> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(FieldSource::id),
            FieldType.CODEC.fieldOf("field").forGetter(FieldSource::type),
            Vec3.CODEC.fieldOf("pos").forGetter(FieldSource::position),
            Codec.DOUBLE.fieldOf("charge").forGetter(FieldSource::charge),
            Codec.LONG.optionalFieldOf("expires_at", -1L).forGetter(FieldSource::expiresAt),
            ResourceLocation.CODEC.optionalFieldOf("origin", UNKNOWN_ORIGIN).forGetter(FieldSource::origin)
    ).apply(i, FieldSource::new));

    public static final StreamCodec<FriendlyByteBuf, FieldSource> STREAM_CODEC = StreamCodec.of(
            (buf, s) -> {
                buf.writeUUID(s.id);
                FieldType.STREAM_CODEC.encode(buf, s.type);
                buf.writeDouble(s.position.x);
                buf.writeDouble(s.position.y);
                buf.writeDouble(s.position.z);
                buf.writeDouble(s.charge);
                buf.writeVarLong(s.expiresAt + 1);
                buf.writeResourceLocation(s.origin);
            },
            buf -> new FieldSource(
                    buf.readUUID(),
                    FieldType.STREAM_CODEC.decode(buf),
                    new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()),
                    buf.readDouble(),
                    buf.readVarLong() - 1,
                    buf.readResourceLocation()));

    /** A source that lives until removed. */
    public static FieldSource permanent(UUID id, FieldType type, Vec3 position, double charge, ResourceLocation origin) {
        return new FieldSource(id, type, position, charge, -1L, origin);
    }

    /** A source with a random id that disappears after {@code durationTicks}. */
    public static FieldSource temporary(Level level, FieldType type, Vec3 position, double charge, int durationTicks, ResourceLocation origin) {
        return new FieldSource(UUID.randomUUID(), type, position, charge, level.getGameTime() + Math.max(1, durationTicks), origin);
    }

    public boolean isPermanent() {
        return expiresAt < 0;
    }

    public boolean isExpired(long gameTime) {
        return expiresAt >= 0 && gameTime >= expiresAt;
    }

    public FieldSource withCharge(double newCharge) {
        return new FieldSource(id, type, position, newCharge, expiresAt, origin);
    }

    public FieldSource withPosition(Vec3 newPosition) {
        return new FieldSource(id, type, newPosition, charge, expiresAt, origin);
    }
}
