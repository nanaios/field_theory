package com.nanaios.fieldtheory.core;

import com.nanaios.fieldtheory.api.FieldSource;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

/**
 * Sources of one field type, bucketed into 16×16 columns. Small sets are scanned linearly; large
 * sets are queried through the grid.
 */
final class SourceIndex {
    static final int GRID_THRESHOLD = 32;
    static final double CELL = 16.0;

    private final List<FieldSource> all = new ArrayList<>();
    private final Long2ObjectMap<List<FieldSource>> cells = new Long2ObjectOpenHashMap<>();

    static int cell(double coordinate) {
        return Mth.floor(coordinate / CELL);
    }

    private static long key(Vec3 pos) {
        return ChunkPos.asLong(cell(pos.x), cell(pos.z));
    }

    void add(FieldSource source) {
        all.add(source);
        cells.computeIfAbsent(key(source.position()), k -> new ArrayList<>()).add(source);
    }

    void remove(FieldSource source) {
        UUID id = source.id();
        all.removeIf(s -> s.id().equals(id));
        long key = key(source.position());
        List<FieldSource> list = cells.get(key);
        if (list != null) {
            list.removeIf(s -> s.id().equals(id));
            if (list.isEmpty()) {
                cells.remove(key);
            }
        }
    }

    boolean isEmpty() {
        return all.isEmpty();
    }

    boolean useGrid() {
        return all.size() > GRID_THRESHOLD;
    }

    List<FieldSource> all() {
        return all;
    }

    @Nullable
    List<FieldSource> cell(int cx, int cz) {
        return cells.get(ChunkPos.asLong(cx, cz));
    }
}
