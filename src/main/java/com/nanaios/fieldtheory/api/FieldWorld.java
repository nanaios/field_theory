package com.nanaios.fieldtheory.api;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * All fields of one level. Obtain it with {@link FieldTheoryAPI#get(Level)}.
 *
 * <p>Reading works on both sides (the client receives a synchronised copy of the sources and
 * regions). Mutating methods may only be called on the logical server; changes are synchronised
 * to clients automatically at the end of the tick.</p>
 */
public interface FieldWorld {

    Level level();

    /** Samples {@code type} at {@code pos}. */
    default FieldSample sample(FieldType type, Vec3 pos) {
        return sample(type, pos, null);
    }

    /**
     * Samples {@code type} at {@code pos}, ignoring the contribution of {@code exclude} (an entity
     * does not feel its own field).
     */
    FieldSample sample(FieldType type, Vec3 pos, @Nullable Entity exclude);

    /** Ambient (background) potential at {@code pos}, without region gain. */
    double ambient(FieldType type, Vec3 pos);

    /** Product of region gains for {@code type} at {@code pos}. */
    double gain(FieldType type, Vec3 pos);

    /** {@code true} if any source or emitter of {@code type} exists in this level. */
    boolean hasContent(FieldType type);

    /** {@code true} if the level has no sources, regions or emitters at all. */
    boolean isQuiet();

    Collection<FieldSource> sources();

    Optional<FieldSource> source(UUID id);

    /** Sources of {@code type} (or of any type if {@code null}) within {@code radius} of {@code pos}. */
    List<FieldSource> sourcesNear(@Nullable FieldType type, Vec3 pos, double radius);

    Collection<FieldRegion> regions();

    /** Charged entities currently acting as sources of {@code type} (snapshot of this tick). */
    List<Emitter> emitters(FieldType type);

    // ---------------------------------------------------------------- server-side mutation

    /**
     * Adds or replaces (same id) a source. Returns {@code false} if a listener of
     * {@link com.nanaios.fieldtheory.api.event.FieldSourceEvent.Added} cancelled it, or if the
     * dimension already holds the configured maximum number of sources.
     */
    boolean putSource(FieldSource source);

    boolean removeSource(UUID id);

    /** Removes every source matching {@code filter}; returns how many were removed. */
    int removeSources(Predicate<FieldSource> filter);

    void putRegion(FieldRegion region);

    boolean removeRegion(UUID id);

    int removeRegions(Predicate<FieldRegion> filter);

    /**
     * A charged entity acting as a field source.
     *
     * @param position the entity's centre of mass at snapshot time
     * @param charge   its total charge for the field
     */
    record Emitter(Entity entity, Vec3 position, double charge) {
    }
}
