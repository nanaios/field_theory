package com.nanaios.fieldtheory.core;

import com.nanaios.fieldtheory.api.FieldRegion;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

/**
 * Regions bucketed into 16×16 columns: a region is stored in every column its bounding square
 * touches, so a point only has to look at the regions of its own column.
 */
final class RegionIndex {
    private final Long2ObjectMap<List<FieldRegion>> cells = new Long2ObjectOpenHashMap<>();

    void add(FieldRegion region) {
        forEachCell(region, key -> cells.computeIfAbsent(key, k -> new ArrayList<>()).add(region));
    }

    void remove(FieldRegion region) {
        UUID id = region.id();
        forEachCell(region, key -> {
            List<FieldRegion> list = cells.get(key);
            if (list != null) {
                list.removeIf(r -> r.id().equals(id));
                if (list.isEmpty()) {
                    cells.remove(key);
                }
            }
        });
    }

    void clear() {
        cells.clear();
    }

    @Nullable
    List<FieldRegion> at(Vec3 pos) {
        return cells.get(ChunkPos.asLong(SourceIndex.cell(pos.x), SourceIndex.cell(pos.z)));
    }

    private static void forEachCell(FieldRegion region, java.util.function.LongConsumer action) {
        Vec3 c = region.center();
        double r = region.radius();
        int minX = SourceIndex.cell(c.x - r);
        int maxX = SourceIndex.cell(c.x + r);
        int minZ = SourceIndex.cell(c.z - r);
        int maxZ = SourceIndex.cell(c.z + r);
        for (int cx = minX; cx <= maxX; cx++) {
            for (int cz = minZ; cz <= maxZ; cz++) {
                action.accept(ChunkPos.asLong(cx, cz));
            }
        }
    }
}
