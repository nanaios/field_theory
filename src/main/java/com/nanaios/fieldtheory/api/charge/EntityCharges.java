package com.nanaios.fieldtheory.api.charge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.nanaios.fieldtheory.api.FieldType;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * Charges induced on an entity at runtime (by magic, machines, commands...). Immutable; stored as
 * an entity data attachment, persisted and synchronised to tracking clients.
 */
public record EntityCharges(Map<FieldType, Charge> charges) {
    public static final EntityCharges EMPTY = new EntityCharges(Map.of());

    public static final Codec<EntityCharges> CODEC = Codec.unboundedMap(FieldType.CODEC, Charge.CODEC)
            .xmap(EntityCharges::new, EntityCharges::charges);

    public static final StreamCodec<FriendlyByteBuf, EntityCharges> STREAM_CODEC = StreamCodec.of(
            (buf, c) -> {
                buf.writeVarInt(c.charges.size());
                c.charges.forEach((type, charge) -> {
                    FieldType.STREAM_CODEC.encode(buf, type);
                    buf.writeDouble(charge.value());
                    buf.writeVarLong(charge.expiresAt() + 1);
                });
            },
            buf -> {
                int size = buf.readVarInt();
                Map<FieldType, Charge> map = new IdentityHashMap<>();
                for (int i = 0; i < size; i++) {
                    FieldType type = FieldType.STREAM_CODEC.decode(buf);
                    map.put(type, new Charge(buf.readDouble(), buf.readVarLong() - 1));
                }
                return new EntityCharges(map);
            });

    public EntityCharges {
        charges = Map.copyOf(charges);
    }

    public boolean isEmpty() {
        return charges.isEmpty();
    }

    public boolean has(FieldType type) {
        return charges.containsKey(type);
    }

    public double value(FieldType type) {
        Charge c = charges.get(type);
        return c == null ? 0.0 : c.value();
    }

    public EntityCharges with(FieldType type, Charge charge) {
        Map<FieldType, Charge> copy = new HashMap<>(charges);
        copy.put(type, charge);
        return new EntityCharges(copy);
    }

    public EntityCharges without(FieldType type) {
        if (!charges.containsKey(type)) {
            return this;
        }
        Map<FieldType, Charge> copy = new HashMap<>(charges);
        copy.remove(type);
        return new EntityCharges(copy);
    }

    /** Drops expired charges; returns {@code this} if nothing changed. */
    public EntityCharges purge(long gameTime) {
        boolean any = false;
        for (Charge c : charges.values()) {
            if (c.isExpired(gameTime)) {
                any = true;
                break;
            }
        }
        if (!any) {
            return this;
        }
        Map<FieldType, Charge> copy = new HashMap<>(charges);
        copy.values().removeIf(c -> c.isExpired(gameTime));
        return new EntityCharges(copy);
    }

    /**
     * @param expiresAt game time at which the charge vanishes, or {@code -1} for permanent
     */
    public record Charge(double value, long expiresAt) {
        public static final Codec<Charge> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("value").forGetter(Charge::value),
                Codec.LONG.optionalFieldOf("expires_at", -1L).forGetter(Charge::expiresAt)
        ).apply(i, Charge::new));

        public boolean isExpired(long gameTime) {
            return expiresAt >= 0 && gameTime >= expiresAt;
        }
    }
}
