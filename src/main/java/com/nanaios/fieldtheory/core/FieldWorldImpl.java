package com.nanaios.fieldtheory.core;

import com.nanaios.fieldtheory.api.FieldRegion;
import com.nanaios.fieldtheory.api.FieldSample;
import com.nanaios.fieldtheory.api.FieldSource;
import com.nanaios.fieldtheory.api.FieldType;
import com.nanaios.fieldtheory.api.FieldWorld;
import com.nanaios.fieldtheory.api.charge.BaseCharges;
import com.nanaios.fieldtheory.api.charge.Charges;
import com.nanaios.fieldtheory.api.charge.EntityCharges;
import com.nanaios.fieldtheory.api.event.FieldSourceEvent;
import com.nanaios.fieldtheory.network.FieldDeltaPayload;
import com.nanaios.fieldtheory.network.FieldSnapshotPayload;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The field state of one level. On the server it is authoritative and persisted through
 * {@link FieldSavedData}; on the client it is a mirror fed by network payloads.
 */
public final class FieldWorldImpl implements FieldWorld {
    private final Level level;
    private final boolean authoritative;

    private final Map<UUID, FieldSource> sources = new LinkedHashMap<>();
    private final Map<FieldType, SourceIndex> indices = new IdentityHashMap<>();
    private final Map<UUID, FieldRegion> regions = new LinkedHashMap<>();
    private final RegionIndex regionIndex = new RegionIndex();

    private final Int2ObjectMap<Entity> emitterEntities = new Int2ObjectOpenHashMap<>();
    private final Map<FieldType, List<Emitter>> emitterSnapshot = new IdentityHashMap<>();
    private long snapshotTime = Long.MIN_VALUE;

    private long nextExpiry = Long.MAX_VALUE;
    private Runnable dirtyListener = () -> {
    };

    // Changes not yet sent to clients (server only).
    private final Map<UUID, FieldSource> pendingSourcePuts = new LinkedHashMap<>();
    private final Set<UUID> pendingSourceRemovals = new LinkedHashSet<>();
    private final Map<UUID, FieldRegion> pendingRegionPuts = new LinkedHashMap<>();
    private final Set<UUID> pendingRegionRemovals = new LinkedHashSet<>();

    FieldWorldImpl(Level level, boolean authoritative) {
        this.level = level;
        this.authoritative = authoritative;
    }

    @Override
    public Level level() {
        return level;
    }

    // ================================================================== sampling

    /** Visits every source of {@code type} that may lie within {@code range} of {@code pos}. */
    private void forEachSourceNear(FieldType type, Vec3 pos, double range, Consumer<FieldSource> visitor) {
        SourceIndex index = indices.get(type);
        if (index == null) {
            return;
        }
        if (index.useGrid()) {
            int minX = SourceIndex.cell(pos.x - range);
            int maxX = SourceIndex.cell(pos.x + range);
            int minZ = SourceIndex.cell(pos.z - range);
            int maxZ = SourceIndex.cell(pos.z + range);
            for (int cx = minX; cx <= maxX; cx++) {
                for (int cz = minZ; cz <= maxZ; cz++) {
                    List<FieldSource> cell = index.cell(cx, cz);
                    if (cell != null) {
                        cell.forEach(visitor);
                    }
                }
            }
        } else {
            index.all().forEach(visitor);
        }
    }

    @Override
    public FieldSample sample(FieldType type, Vec3 pos, @Nullable Entity exclude) {
        double gain = gain(type, pos);
        if (gain == 0.0) {
            return new FieldSample(0.0, 0.0, Vec3.ZERO, 0.0);
        }
        double ambient = ambient(type, pos);
        double range = type.range();
        double time = level.getGameTime();
        double[] acc = new double[4];

        forEachSourceNear(type, pos, range, s -> accumulate(type, pos, s.position(), s.charge(), s.frequency(), s.phase(), time, range, acc));

        for (Emitter emitter : emitters(type)) {
            if (emitter.entity() != exclude) {
                accumulate(type, pos, emitter.position(), emitter.charge(), 0.0, 0.0, time, range, acc);
            }
        }

        return new FieldSample(gain * (acc[0] + ambient), gain * ambient,
                new Vec3(acc[1] * gain, acc[2] * gain, acc[3] * gain), gain);
    }

    /**
     * Adds one source. For an oscillating source the retarded phase θ = ωt − kr + φ0 multiplies
     * the potential, and the radial field gains the term from ∂θ/∂r:
     * E_r = Q·(−P′(r)·cos θ − k·P(r)·sin θ).
     */
    private static void accumulate(FieldType type, Vec3 pos, Vec3 source, double charge, double frequency, double phase,
                                   double time, double range, double[] acc) {
        double dx = pos.x - source.x;
        double dy = pos.y - source.y;
        double dz = pos.z - source.z;
        double d2 = dx * dx + dy * dy + dz * dz;
        if (d2 > range * range) {
            return;
        }
        double r = Math.sqrt(d2);
        double radial;
        if (frequency == 0.0) {
            acc[0] += type.potential(charge, r);
            radial = type.field(charge, r);
        } else {
            double k = frequency / type.waveSpeed();
            double theta = frequency * time - k * r + phase;
            double cos = Math.cos(theta);
            double potential = type.potential(charge, r);
            acc[0] += potential * cos;
            radial = type.field(charge, r) * cos - k * potential * Math.sin(theta);
        }
        if (r > 1.0E-9) {
            double f = radial / r;
            acc[1] += f * dx;
            acc[2] += f * dy;
            acc[3] += f * dz;
        }
    }

    @Override
    public double amplitude(FieldType type, Vec3 pos) {
        double gain = gain(type, pos);
        if (gain == 0.0) {
            return 0.0;
        }
        double range = type.range();
        // frequency -> {Re, Im} of the summed phasor Σ Q·P(r)·e^{i(φ0 − kr)}
        Map<Double, double[]> phasors = new HashMap<>();
        forEachSourceNear(type, pos, range, s -> {
            if (!s.isOscillating()) {
                return;
            }
            double r = s.position().distanceTo(pos);
            if (r > range) {
                return;
            }
            double magnitude = type.potential(s.charge(), r);
            double angle = s.phase() - s.frequency() / type.waveSpeed() * r;
            double[] p = phasors.computeIfAbsent(s.frequency(), f -> new double[2]);
            p[0] += magnitude * Math.cos(angle);
            p[1] += magnitude * Math.sin(angle);
        });
        double total = 0.0;
        for (double[] p : phasors.values()) {
            total += Math.sqrt(p[0] * p[0] + p[1] * p[1]);
        }
        return gain * total;
    }

    @Override
    public double ambient(FieldType type, Vec3 pos) {
        return AmbientFields.evaluate(level, type, pos);
    }

    @Override
    public double gain(FieldType type, Vec3 pos) {
        if (regions.isEmpty()) {
            return 1.0;
        }
        List<FieldRegion> nearby = regionIndex.at(pos);
        if (nearby == null) {
            return 1.0;
        }
        double g = 1.0;
        for (FieldRegion region : nearby) {
            if (region.affects(type)) {
                g *= region.gainAt(pos);
            }
        }
        return g;
    }

    @Override
    public boolean hasContent(FieldType type) {
        SourceIndex index = indices.get(type);
        return (index != null && !index.isEmpty()) || !emitters(type).isEmpty();
    }

    @Override
    public boolean isQuiet() {
        return sources.isEmpty() && regions.isEmpty() && emitterEntities.isEmpty();
    }

    @Override
    public Collection<FieldSource> sources() {
        return Collections.unmodifiableCollection(sources.values());
    }

    @Override
    public Optional<FieldSource> source(UUID id) {
        return Optional.ofNullable(sources.get(id));
    }

    @Override
    public List<FieldSource> sourcesNear(@Nullable FieldType type, Vec3 pos, double radius) {
        List<FieldSource> out = new ArrayList<>();
        double r2 = radius * radius;
        Collection<FieldSource> candidates;
        if (type == null) {
            candidates = sources.values();
        } else {
            SourceIndex index = indices.get(type);
            if (index == null) {
                return out;
            }
            candidates = index.all();
        }
        for (FieldSource s : candidates) {
            if (s.position().distanceToSqr(pos) <= r2) {
                out.add(s);
            }
        }
        return out;
    }

    @Override
    public Collection<FieldRegion> regions() {
        return Collections.unmodifiableCollection(regions.values());
    }

    // ================================================================== emitters

    @Override
    public List<Emitter> emitters(FieldType type) {
        long now = level.getGameTime();
        if (now != snapshotTime) {
            rebuildSnapshot(now);
        }
        List<Emitter> list = emitterSnapshot.get(type);
        return list == null ? List.of() : list;
    }

    private void rebuildSnapshot(long now) {
        snapshotTime = now;
        emitterSnapshot.clear();
        if (emitterEntities.isEmpty()) {
            return;
        }
        var iterator = emitterEntities.int2ObjectEntrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            Entity entity = entry.getValue();
            if (entity.isRemoved() || entity.level() != level || level.getEntity(entry.getIntKey()) != entity) {
                iterator.remove();
                continue;
            }
            Vec3 center = Charges.center(entity);
            BaseCharges base = Charges.base(entity);
            EntityCharges induced = Charges.induced(entity);
            if (base != null) {
                for (FieldType type : base.charges().keySet()) {
                    addEmitter(entity, center, type);
                }
            }
            for (FieldType type : induced.charges().keySet()) {
                if (base == null || !base.charges().containsKey(type)) {
                    addEmitter(entity, center, type);
                }
            }
        }
    }

    private void addEmitter(Entity entity, Vec3 center, FieldType type) {
        double q = Charges.charge(entity, type);
        if (q != 0.0) {
            emitterSnapshot.computeIfAbsent(type, t -> new ArrayList<>()).add(new Emitter(entity, center, q));
        }
    }

    /** Starts or stops tracking {@code entity} as an emitter depending on its charges. */
    public void updateEmitter(Entity entity) {
        if (!entity.isRemoved() && Charges.isEmitter(entity)) {
            emitterEntities.put(entity.getId(), entity);
        } else if (emitterEntities.get(entity.getId()) == entity) {
            emitterEntities.remove(entity.getId());
        }
        snapshotTime = Long.MIN_VALUE;
    }

    public void removeEmitter(Entity entity) {
        if (emitterEntities.get(entity.getId()) == entity) {
            emitterEntities.remove(entity.getId());
            snapshotTime = Long.MIN_VALUE;
        }
    }

    List<Entity> emitterEntities() {
        return new ArrayList<>(emitterEntities.values());
    }

    // ================================================================== mutation (server)

    private void requireAuthority() {
        if (!authoritative) {
            throw new IllegalStateException("Fields can only be modified on the logical server");
        }
    }

    @Override
    public boolean putSource(FieldSource source) {
        requireAuthority();
        boolean isNew = !sources.containsKey(source.id());
        if (isNew && sources.size() >= FieldTheoryConfig.maxSourcesPerLevel()) {
            return false;
        }
        if (isNew && NeoForge.EVENT_BUS.post(new FieldSourceEvent.Added(level, source)).isCanceled()) {
            return false;
        }
        putSourceInternal(source);
        pendingSourceRemovals.remove(source.id());
        pendingSourcePuts.put(source.id(), source);
        trackExpiry(source.expiresAt());
        dirtyListener.run();
        return true;
    }

    @Override
    public boolean removeSource(UUID id) {
        requireAuthority();
        FieldSource removed = removeSourceInternal(id);
        if (removed == null) {
            return false;
        }
        NeoForge.EVENT_BUS.post(new FieldSourceEvent.Removed(level, removed));
        pendingSourcePuts.remove(id);
        pendingSourceRemovals.add(id);
        dirtyListener.run();
        return true;
    }

    @Override
    public int removeSources(Predicate<FieldSource> filter) {
        requireAuthority();
        List<UUID> ids = new ArrayList<>();
        for (FieldSource s : sources.values()) {
            if (filter.test(s)) {
                ids.add(s.id());
            }
        }
        ids.forEach(this::removeSource);
        return ids.size();
    }

    @Override
    public void putRegion(FieldRegion region) {
        requireAuthority();
        putRegionInternal(region);
        pendingRegionRemovals.remove(region.id());
        pendingRegionPuts.put(region.id(), region);
        trackExpiry(region.expiresAt());
        dirtyListener.run();
    }

    @Override
    public boolean removeRegion(UUID id) {
        requireAuthority();
        if (removeRegionInternal(id) == null) {
            return false;
        }
        pendingRegionPuts.remove(id);
        pendingRegionRemovals.add(id);
        dirtyListener.run();
        return true;
    }

    @Override
    public int removeRegions(Predicate<FieldRegion> filter) {
        requireAuthority();
        List<UUID> ids = new ArrayList<>();
        for (FieldRegion r : regions.values()) {
            if (filter.test(r)) {
                ids.add(r.id());
            }
        }
        ids.forEach(this::removeRegion);
        return ids.size();
    }

    private void putRegionInternal(FieldRegion region) {
        FieldRegion old = regions.put(region.id(), region);
        if (old != null) {
            regionIndex.remove(old);
        }
        regionIndex.add(region);
    }

    @Nullable
    private FieldRegion removeRegionInternal(UUID id) {
        FieldRegion old = regions.remove(id);
        if (old != null) {
            regionIndex.remove(old);
        }
        return old;
    }

    private void putSourceInternal(FieldSource source) {
        FieldSource old = sources.put(source.id(), source);
        if (old != null) {
            unindex(old);
        }
        indices.computeIfAbsent(source.type(), t -> new SourceIndex()).add(source);
    }

    @Nullable
    private FieldSource removeSourceInternal(UUID id) {
        FieldSource old = sources.remove(id);
        if (old != null) {
            unindex(old);
        }
        return old;
    }

    private void unindex(FieldSource source) {
        SourceIndex index = indices.get(source.type());
        if (index != null) {
            index.remove(source);
            if (index.isEmpty()) {
                indices.remove(source.type());
            }
        }
    }

    private void trackExpiry(long expiresAt) {
        if (expiresAt >= 0 && expiresAt < nextExpiry) {
            nextExpiry = expiresAt;
        }
    }

    void setDirtyListener(Runnable listener) {
        this.dirtyListener = listener;
    }

    /** Loads persisted state without firing events or syncing. */
    void load(List<FieldSource> loadedSources, List<FieldRegion> loadedRegions) {
        for (FieldSource s : loadedSources) {
            putSourceInternal(s);
            trackExpiry(s.expiresAt());
        }
        for (FieldRegion r : loadedRegions) {
            putRegionInternal(r);
            trackExpiry(r.expiresAt());
        }
    }

    /** Called at the end of every server level tick. */
    void tickServer() {
        long now = level.getGameTime();
        if (now >= nextExpiry) {
            expire(now);
        }
        if (now % 10 == 0 && !emitterEntities.isEmpty()) {
            ChargeManager.purgeExpired(this, now);
        }
        flush();
    }

    private void expire(long now) {
        List<UUID> deadSources = new ArrayList<>();
        List<UUID> deadRegions = new ArrayList<>();
        long next = Long.MAX_VALUE;
        for (FieldSource s : sources.values()) {
            if (s.isExpired(now)) {
                deadSources.add(s.id());
            } else if (s.expiresAt() >= 0) {
                next = Math.min(next, s.expiresAt());
            }
        }
        for (FieldRegion r : regions.values()) {
            if (r.isExpired(now)) {
                deadRegions.add(r.id());
            } else if (r.expiresAt() >= 0) {
                next = Math.min(next, r.expiresAt());
            }
        }
        deadSources.forEach(this::removeSource);
        deadRegions.forEach(this::removeRegion);
        nextExpiry = next;
    }

    private void flush() {
        if (pendingSourcePuts.isEmpty() && pendingSourceRemovals.isEmpty() && pendingRegionPuts.isEmpty() && pendingRegionRemovals.isEmpty()) {
            return;
        }
        FieldDeltaPayload payload = new FieldDeltaPayload(
                List.copyOf(pendingSourcePuts.values()), List.copyOf(pendingSourceRemovals),
                List.copyOf(pendingRegionPuts.values()), List.copyOf(pendingRegionRemovals));
        pendingSourcePuts.clear();
        pendingSourceRemovals.clear();
        pendingRegionPuts.clear();
        pendingRegionRemovals.clear();
        if (level instanceof ServerLevel serverLevel) {
            PacketDistributor.sendToPlayersInDimension(serverLevel, payload);
        }
    }

    public FieldSnapshotPayload snapshot() {
        return new FieldSnapshotPayload(List.copyOf(sources.values()), List.copyOf(regions.values()));
    }

    // ================================================================== client mirror

    public void applySnapshot(List<FieldSource> newSources, List<FieldRegion> newRegions) {
        sources.clear();
        indices.clear();
        regions.clear();
        regionIndex.clear();
        newSources.forEach(this::putSourceInternal);
        newRegions.forEach(this::putRegionInternal);
    }

    public void applyDelta(FieldDeltaPayload delta) {
        delta.removedSources().forEach(this::removeSourceInternal);
        delta.putSources().forEach(this::putSourceInternal);
        delta.removedRegions().forEach(this::removeRegionInternal);
        delta.putRegions().forEach(this::putRegionInternal);
    }
}
