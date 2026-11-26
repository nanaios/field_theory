package com.nanaios.fieldtheory.api.charge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.nanaios.fieldtheory.api.FieldType;
import java.util.Map;
import java.util.Optional;

/**
 * Intrinsic charges of an entity type, loaded from the data map
 * {@code data/<ns>/data_maps/entity_type/base_charges.json}:
 *
 * <pre>{@code
 * { "values": { "minecraft:creeper": { "charges": { "field_theory:fulgur": 2.0 } },
 *               "minecraft:iron_golem": { "mass": 6.0 } } }
 * }</pre>
 *
 * An entity type with an explicit charge for a field is an <em>emitter</em> of that field.
 *
 * @param mass    inertial mass override (default: bounding-box volume)
 * @param charges explicit charges per field
 */
public record BaseCharges(Optional<Double> mass, Map<FieldType, Double> charges) {
    public static final Codec<BaseCharges> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.DOUBLE.optionalFieldOf("mass").forGetter(BaseCharges::mass),
            Codec.unboundedMap(FieldType.CODEC, Codec.DOUBLE).optionalFieldOf("charges", Map.of()).forGetter(BaseCharges::charges)
    ).apply(i, BaseCharges::new));
}
