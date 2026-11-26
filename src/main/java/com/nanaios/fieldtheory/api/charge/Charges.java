package com.nanaios.fieldtheory.api.charge;

import com.nanaios.fieldtheory.api.FieldType;
import com.nanaios.fieldtheory.core.ChargeManager;
import com.nanaios.fieldtheory.core.FieldAttachments;
import com.nanaios.fieldtheory.core.FieldDataMaps;
import javax.annotation.Nullable;
import java.util.Map;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;

/**
 * How entities couple to fields.
 *
 * <p>The total charge of an entity for a field is its <em>intrinsic</em> charge (from the
 * {@code base_charges} data map; for fields obeying the equivalence principle the default is the
 * entity's mass) plus any <em>induced</em> charge stored on the entity. An entity is an emitter of
 * a field — it sources that field itself — when it has an explicit intrinsic or induced charge.</p>
 */
public final class Charges {
    /** Lower bound of inertial mass, so tiny entities don't get infinite acceleration. */
    public static final double MIN_MASS = 0.05;

    private Charges() {
    }

    @Nullable
    @SuppressWarnings("deprecation") // the built-in holder is the cheapest data map lookup for entity types
    public static BaseCharges base(Entity entity) {
        return entity.getType().builtInRegistryHolder().getData(FieldDataMaps.BASE_CHARGES);
    }

    public static EntityCharges induced(Entity entity) {
        return entity.hasData(FieldAttachments.ENTITY_CHARGES) ? entity.getData(FieldAttachments.ENTITY_CHARGES) : EntityCharges.EMPTY;
    }

    /** Inertial mass. Defaults to the bounding box volume. */
    public static double mass(Entity entity) {
        BaseCharges base = base(entity);
        if (base != null && base.mass().isPresent()) {
            return Math.max(MIN_MASS, base.mass().get());
        }
        double w = entity.getBbWidth();
        return Math.max(MIN_MASS, w * w * entity.getBbHeight());
    }

    /** Total charge (intrinsic + induced). */
    public static double charge(Entity entity, FieldType type) {
        BaseCharges base = base(entity);
        Double explicit = base == null ? null : base.charges().get(type);
        double q = explicit != null ? explicit : (type.equivalence() ? mass(entity) : 0.0);
        return q + induced(entity).value(type);
    }

    /**
     * Polarizability α of the entity for {@code type}: from {@code base_charges} for entity types,
     * from the {@code polarizability} item data map for dropped items.
     */
    @SuppressWarnings("deprecation")
    public static double polarizability(Entity entity, FieldType type) {
        if (entity instanceof ItemEntity item) {
            Map<FieldType, Double> map = item.getItem().getItem().builtInRegistryHolder().getData(FieldDataMaps.ITEM_POLARIZABILITY);
            Double alpha = map == null ? null : map.get(type);
            return alpha == null ? 0.0 : alpha;
        }
        BaseCharges base = base(entity);
        Double alpha = base == null ? null : base.polarizability().get(type);
        return alpha == null ? 0.0 : alpha;
    }

    /** Whether the entity sources {@code type} itself. */
    public static boolean emits(Entity entity, FieldType type) {
        BaseCharges base = base(entity);
        return (base != null && base.charges().containsKey(type)) || induced(entity).has(type);
    }

    /** Whether the entity sources any field. */
    public static boolean isEmitter(Entity entity) {
        BaseCharges base = base(entity);
        return (base != null && !base.charges().isEmpty()) || !induced(entity).isEmpty();
    }

    /** Centre of mass used as the position of an entity in field computations. */
    public static Vec3 center(Entity entity) {
        return entity.position().add(0.0, entity.getBbHeight() * 0.5, 0.0);
    }

    // ------------------------------------------------------------------ server-side mutation

    /**
     * Adds {@code amount} to the induced charge. The merged charge keeps the later expiry; a
     * negative {@code durationTicks} makes it permanent. Server only.
     */
    public static void induce(Entity entity, FieldType type, double amount, int durationTicks) {
        ChargeManager.induce(entity, type, amount, durationTicks, true);
    }

    /** Replaces the induced charge. Server only. */
    public static void setInduced(Entity entity, FieldType type, double amount, int durationTicks) {
        ChargeManager.induce(entity, type, amount, durationTicks, false);
    }

    /** Removes the induced charge of {@code type}, or of every field if {@code null}. Server only. */
    public static void clearInduced(Entity entity, @Nullable FieldType type) {
        ChargeManager.clear(entity, type);
    }
}
