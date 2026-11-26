package com.nanaios.fieldtheory.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A spherical region that multiplies a field (or every field) by a gain: a <em>lens</em> when the
 * gain is larger than one, a <em>shield</em> when it is smaller. The gain is applied in full
 * inside 80% of the radius and blends linearly back to 1 at the boundary.
 *
 * @param type      the field affected, or empty for all fields
 * @param expiresAt game time at which it disappears, or {@code -1} for a permanent region
 */
public record FieldRegion(UUID id, Optional<FieldType> type, Vec3 center, double radius, double gain, long expiresAt, ResourceLocation origin) {
    public static final Codec<FieldRegion> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(FieldRegion::id),
            FieldType.CODEC.optionalFieldOf("field").forGetter(FieldRegion::type),
            Vec3.CODEC.fieldOf("center").forGetter(FieldRegion::center),
            Codec.DOUBLE.fieldOf("radius").forGetter(FieldRegion::radius),
            Codec.DOUBLE.fieldOf("gain").forGetter(FieldRegion::gain),
            Codec.LONG.optionalFieldOf("expires_at", -1L).forGetter(FieldRegion::expiresAt),
            ResourceLocation.CODEC.optionalFieldOf("origin", FieldSource.UNKNOWN_ORIGIN).forGetter(FieldRegion::origin)
    ).apply(i, FieldRegion::new));

    public static final StreamCodec<FriendlyByteBuf, FieldRegion> STREAM_CODEC = StreamCodec.of(
            (buf, r) -> {
                buf.writeUUID(r.id);
                buf.writeBoolean(r.type.isPresent());
                r.type.ifPresent(t -> FieldType.STREAM_CODEC.encode(buf, t));
                buf.writeDouble(r.center.x);
                buf.writeDouble(r.center.y);
                buf.writeDouble(r.center.z);
                buf.writeDouble(r.radius);
                buf.writeDouble(r.gain);
                buf.writeVarLong(r.expiresAt + 1);
                buf.writeResourceLocation(r.origin);
            },
            buf -> new FieldRegion(
                    buf.readUUID(),
                    buf.readBoolean() ? Optional.of(FieldType.STREAM_CODEC.decode(buf)) : Optional.empty(),
                    new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readVarLong() - 1,
                    buf.readResourceLocation()));

    public static FieldRegion temporary(Level level, Optional<FieldType> type, Vec3 center, double radius, double gain, int durationTicks, ResourceLocation origin) {
        return new FieldRegion(UUID.randomUUID(), type, center, radius, gain, level.getGameTime() + Math.max(1, durationTicks), origin);
    }

    public boolean affects(FieldType fieldType) {
        return type.isEmpty() || type.get() == fieldType;
    }

    public boolean isExpired(long gameTime) {
        return expiresAt >= 0 && gameTime >= expiresAt;
    }

    public boolean contains(Vec3 pos) {
        return pos.distanceToSqr(center) < radius * radius;
    }

    /** The multiplicative gain at {@code pos} (1 outside the region). */
    public double gainAt(Vec3 pos) {
        double d2 = pos.distanceToSqr(center);
        if (d2 >= radius * radius) {
            return 1.0;
        }
        double inner = radius * 0.8;
        double d = Math.sqrt(d2);
        if (d <= inner) {
            return gain;
        }
        double t = (d - inner) / (radius - inner);
        return gain + (1.0 - gain) * t;
    }
}
