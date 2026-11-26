package com.nanaios.fieldtheory.api;

import com.mojang.serialization.Codec;
import com.nanaios.fieldtheory.core.FieldRegistries;
import io.netty.buffer.ByteBuf;
import java.util.Objects;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/**
 * Definition of a kind of field ("Gravis", "Fulgur", ...). Instances are registry singletons and
 * compared by identity.
 */
public final class FieldType {
    public static final Codec<FieldType> CODEC = FieldRegistries.FIELD_TYPES.byNameCodec();
    public static final StreamCodec<ByteBuf, FieldType> STREAM_CODEC = ResourceLocation.STREAM_CODEC.map(
            id -> Objects.requireNonNull(FieldRegistries.FIELD_TYPES.get(id), () -> "Unknown field type " + id),
            type -> Objects.requireNonNull(FieldRegistries.FIELD_TYPES.getKey(type), "Unregistered field type"));

    private final Propagator propagator;
    private final Coupling coupling;
    private final double strength;
    private final double range;
    private final double forceScale;
    private final int color;
    private final boolean equivalence;
    private final double waveSpeed;
    private String descriptionId;

    private FieldType(Builder builder) {
        this.propagator = builder.propagator;
        this.coupling = builder.coupling;
        this.strength = builder.strength;
        this.range = builder.range;
        this.forceScale = builder.forceScale;
        this.color = builder.color;
        this.equivalence = builder.equivalence;
        this.waveSpeed = builder.waveSpeed;
    }

    public static Builder builder(Propagator propagator) {
        return new Builder(propagator);
    }

    public Propagator propagator() {
        return propagator;
    }

    public Coupling coupling() {
        return coupling;
    }

    /** Coupling constant {@code k}: φ = k · q · P(r). */
    public double strength() {
        return strength;
    }

    /** Hard cut-off distance beyond which a source does not contribute. */
    public double range() {
        return range;
    }

    /** Converts {@code (Q/m)·E} into an acceleration in blocks/tick². */
    public double forceScale() {
        return forceScale;
    }

    /** RGB colour used for visualisation. */
    public int color() {
        return color;
    }

    /**
     * When {@code true}, an entity without an explicit charge carries a charge equal to its
     * inertial mass (the equivalence principle): every body falls the same way in this field.
     */
    public boolean equivalence() {
        return equivalence;
    }

    /**
     * Speed (blocks/tick) at which changes of an oscillating source travel. The wavelength of a
     * source with period {@code T} ticks is {@code λ = waveSpeed · T}.
     */
    public double waveSpeed() {
        return waveSpeed;
    }

    /** Potential of a charge {@code q} at distance {@code r}. */
    public double potential(double charge, double r) {
        return strength * charge * propagator.potential(r);
    }

    /** Radial field strength of a charge {@code q} at distance {@code r}. */
    public double field(double charge, double r) {
        return strength * charge * propagator.field(r);
    }

    public ResourceLocation id() {
        return Objects.requireNonNull(FieldRegistries.FIELD_TYPES.getKey(this), "Unregistered field type");
    }

    public String descriptionId() {
        if (descriptionId == null) {
            descriptionId = Util.makeDescriptionId("field_type", id());
        }
        return descriptionId;
    }

    public MutableComponent displayName() {
        return Component.translatable(descriptionId()).withStyle(style -> style.withColor(color));
    }

    @Override
    public String toString() {
        ResourceLocation key = FieldRegistries.FIELD_TYPES.getKey(this);
        return "FieldType[" + (key == null ? "unregistered" : key) + "]";
    }

    public static final class Builder {
        private final Propagator propagator;
        private Coupling coupling = Coupling.LIKE_REPELS;
        private double strength = 1.0;
        private double range = 32.0;
        private double forceScale = 0.05;
        private int color = 0xFFFFFF;
        private boolean equivalence;
        private double waveSpeed = 0.5;

        private Builder(Propagator propagator) {
            this.propagator = Objects.requireNonNull(propagator);
        }

        public Builder coupling(Coupling coupling) {
            this.coupling = coupling;
            return this;
        }

        public Builder strength(double strength) {
            this.strength = strength;
            return this;
        }

        public Builder range(double range) {
            this.range = range;
            return this;
        }

        public Builder forceScale(double forceScale) {
            this.forceScale = forceScale;
            return this;
        }

        public Builder color(int rgb) {
            this.color = rgb & 0xFFFFFF;
            return this;
        }

        public Builder equivalence() {
            this.equivalence = true;
            return this;
        }

        public Builder waveSpeed(double blocksPerTick) {
            if (blocksPerTick <= 0) {
                throw new IllegalArgumentException("waveSpeed must be positive");
            }
            this.waveSpeed = blocksPerTick;
            return this;
        }

        public FieldType build() {
            return new FieldType(this);
        }
    }
}
