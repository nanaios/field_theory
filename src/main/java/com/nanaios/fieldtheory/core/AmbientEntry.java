package com.nanaios.fieldtheory.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.nanaios.fieldtheory.api.FieldType;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * One data-driven contribution to the ambient ("vacuum") potential of a field, loaded from
 * {@code data/<ns>/field_theory/ambient/*.json}. A file may hold one object or an array of them.
 *
 * <pre>{@code
 * { "field": "field_theory:fulgur", "dimensions": ["minecraft:overworld"],
 *   "base": 0.2, "rain": 0.8, "thunder": 3.0, "night": 0.0, "min_y": -64, "max_y": 320 }
 * }</pre>
 */
public record AmbientEntry(FieldType field, List<ResourceKey<Level>> dimensions, double base, double rain, double thunder,
                           double night, int minY, int maxY) {
    public static final Codec<AmbientEntry> CODEC = RecordCodecBuilder.create(i -> i.group(
            FieldType.CODEC.fieldOf("field").forGetter(AmbientEntry::field),
            ResourceKey.codec(Registries.DIMENSION).listOf().optionalFieldOf("dimensions", List.of()).forGetter(AmbientEntry::dimensions),
            Codec.DOUBLE.optionalFieldOf("base", 0.0).forGetter(AmbientEntry::base),
            Codec.DOUBLE.optionalFieldOf("rain", 0.0).forGetter(AmbientEntry::rain),
            Codec.DOUBLE.optionalFieldOf("thunder", 0.0).forGetter(AmbientEntry::thunder),
            Codec.DOUBLE.optionalFieldOf("night", 0.0).forGetter(AmbientEntry::night),
            Codec.INT.optionalFieldOf("min_y", Integer.MIN_VALUE).forGetter(AmbientEntry::minY),
            Codec.INT.optionalFieldOf("max_y", Integer.MAX_VALUE).forGetter(AmbientEntry::maxY)
    ).apply(i, AmbientEntry::new));

    public static final StreamCodec<FriendlyByteBuf, AmbientEntry> STREAM_CODEC = StreamCodec.of(
            (buf, e) -> {
                FieldType.STREAM_CODEC.encode(buf, e.field);
                buf.writeVarInt(e.dimensions.size());
                e.dimensions.forEach(buf::writeResourceKey);
                buf.writeDouble(e.base);
                buf.writeDouble(e.rain);
                buf.writeDouble(e.thunder);
                buf.writeDouble(e.night);
                buf.writeInt(e.minY);
                buf.writeInt(e.maxY);
            },
            buf -> {
                FieldType field = FieldType.STREAM_CODEC.decode(buf);
                int n = buf.readVarInt();
                List<ResourceKey<Level>> dims = new ArrayList<>(n);
                for (int k = 0; k < n; k++) {
                    dims.add(buf.readResourceKey(Registries.DIMENSION));
                }
                return new AmbientEntry(field, dims, buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readInt(), buf.readInt());
            });

    public double evaluate(Level level, Vec3 pos) {
        if (!dimensions.isEmpty() && !dimensions.contains(level.dimension())) {
            return 0.0;
        }
        if (pos.y < minY || pos.y > maxY) {
            return 0.0;
        }
        double value = base;
        if (rain != 0.0 && level.isRaining()) {
            value += rain;
        }
        if (thunder != 0.0 && level.isThundering()) {
            value += thunder;
        }
        if (night != 0.0 && isNight(level)) {
            value += night;
        }
        return value;
    }

    private static boolean isNight(Level level) {
        if (level.dimensionType().hasFixedTime()) {
            return false;
        }
        long time = level.getDayTime() % 24000L;
        return time >= 13000L && time < 23000L;
    }
}
