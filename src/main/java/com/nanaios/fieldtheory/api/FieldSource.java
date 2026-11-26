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
 * A point charge that sources a field. A source may <em>oscillate</em>: its charge then varies as
 * {@code q·cos(ωt + φ0)} and the change travels outwards at the field's
 * {@link FieldType#waveSpeed() wave speed}, so that at distance {@code r} the source contributes
 * {@code q·cos(ωt − kr + φ0)·P(r)} with {@code k = ω / c}. Coherent oscillating sources interfere.
 *
 * @param id        stable identity; putting a source with an existing id replaces it
 * @param type      the field it sources
 * @param position  where it sits
 * @param charge    signed strength
 * @param expiresAt game time at which it disappears, or {@code -1} for a permanent source
 * @param origin    free-form tag describing who created it (e.g. {@code field_arcana:staff})
 * @param frequency angular frequency ω in radians per tick; {@code 0} for a static source
 * @param phase     phase offset φ0 in radians
 */
public record FieldSource(UUID id, FieldType type, Vec3 position, double charge, long expiresAt, ResourceLocation origin,
                          double frequency, double phase) {
    public static final ResourceLocation UNKNOWN_ORIGIN = FieldTheoryAPI.id("unknown");

    public static final Codec<FieldSource> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(FieldSource::id),
            FieldType.CODEC.fieldOf("field").forGetter(FieldSource::type),
            Vec3.CODEC.fieldOf("pos").forGetter(FieldSource::position),
            Codec.DOUBLE.fieldOf("charge").forGetter(FieldSource::charge),
            Codec.LONG.optionalFieldOf("expires_at", -1L).forGetter(FieldSource::expiresAt),
            ResourceLocation.CODEC.optionalFieldOf("origin", UNKNOWN_ORIGIN).forGetter(FieldSource::origin),
            Codec.DOUBLE.optionalFieldOf("frequency", 0.0).forGetter(FieldSource::frequency),
            Codec.DOUBLE.optionalFieldOf("phase", 0.0).forGetter(FieldSource::phase)
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
                buf.writeDouble(s.frequency);
                buf.writeDouble(s.phase);
            },
            buf -> new FieldSource(
                    buf.readUUID(),
                    FieldType.STREAM_CODEC.decode(buf),
                    new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()),
                    buf.readDouble(),
                    buf.readVarLong() - 1,
                    buf.readResourceLocation(),
                    buf.readDouble(),
                    buf.readDouble()));

    /** A static source. */
    public FieldSource(UUID id, FieldType type, Vec3 position, double charge, long expiresAt, ResourceLocation origin) {
        this(id, type, position, charge, expiresAt, origin, 0.0, 0.0);
    }

    /** A source that lives until removed. */
    public static FieldSource permanent(UUID id, FieldType type, Vec3 position, double charge, ResourceLocation origin) {
        return new FieldSource(id, type, position, charge, -1L, origin);
    }

    /** A source with a random id that disappears after {@code durationTicks}. */
    public static FieldSource temporary(Level level, FieldType type, Vec3 position, double charge, int durationTicks, ResourceLocation origin) {
        return new FieldSource(UUID.randomUUID(), type, position, charge, level.getGameTime() + Math.max(1, durationTicks), origin);
    }

    /** Angular frequency (radians/tick) of an oscillation with the given period in ticks. */
    public static double frequencyForPeriod(double periodTicks) {
        return periodTicks <= 0 ? 0.0 : 2.0 * Math.PI / periodTicks;
    }

    public boolean isOscillating() {
        return frequency != 0.0;
    }

    /** Period in ticks, or infinity for a static source. */
    public double period() {
        return frequency == 0.0 ? Double.POSITIVE_INFINITY : 2.0 * Math.PI / Math.abs(frequency);
    }

    public FieldSource withOscillation(double newFrequency, double newPhase) {
        return new FieldSource(id, type, position, charge, expiresAt, origin, newFrequency, newPhase);
    }

    public boolean isPermanent() {
        return expiresAt < 0;
    }

    public boolean isExpired(long gameTime) {
        return expiresAt >= 0 && gameTime >= expiresAt;
    }

    public FieldSource withCharge(double newCharge) {
        return new FieldSource(id, type, position, newCharge, expiresAt, origin, frequency, phase);
    }

    public FieldSource withPosition(Vec3 newPosition) {
        return new FieldSource(id, type, newPosition, charge, expiresAt, origin, frequency, phase);
    }
}
