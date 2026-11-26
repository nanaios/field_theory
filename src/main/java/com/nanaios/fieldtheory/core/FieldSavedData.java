package com.nanaios.fieldtheory.core;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.nanaios.fieldtheory.api.FieldRegion;
import com.nanaios.fieldtheory.api.FieldSource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;
import org.slf4j.Logger;

/** Persists the sources and regions of one dimension in {@code data/field_theory_fields.dat}. */
public final class FieldSavedData extends SavedData {
    public static final String NAME = "field_theory_fields";
    private static final Logger LOGGER = LogUtils.getLogger();

    private final List<FieldSource> loadedSources = new ArrayList<>();
    private final List<FieldRegion> loadedRegions = new ArrayList<>();
    @Nullable
    private FieldWorldImpl world;

    public static SavedData.Factory<FieldSavedData> factory() {
        return new SavedData.Factory<>(FieldSavedData::new, FieldSavedData::load, null);
    }

    void attach(FieldWorldImpl world) {
        this.world = world;
        world.load(loadedSources, loadedRegions);
        loadedSources.clear();
        loadedRegions.clear();
        world.setDirtyListener(this::setDirty);
    }

    private static FieldSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        FieldSavedData data = new FieldSavedData();
        decodeAll(tag.getList("sources", Tag.TAG_COMPOUND), FieldSource.CODEC, data.loadedSources);
        decodeAll(tag.getList("regions", Tag.TAG_COMPOUND), FieldRegion.CODEC, data.loadedRegions);
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("sources", encodeAll(world != null ? world.sources() : loadedSources, FieldSource.CODEC));
        tag.put("regions", encodeAll(world != null ? world.regions() : loadedRegions, FieldRegion.CODEC));
        return tag;
    }

    private static <T> void decodeAll(ListTag list, Codec<T> codec, List<T> out) {
        for (Tag element : list) {
            // Entries whose field type no longer exists (mod removed) are dropped individually.
            codec.parse(NbtOps.INSTANCE, element)
                    .resultOrPartial(error -> LOGGER.warn("Dropping unreadable field entry: {}", error))
                    .ifPresent(out::add);
        }
    }

    private static <T> ListTag encodeAll(Collection<T> values, Codec<T> codec) {
        ListTag list = new ListTag();
        for (T value : values) {
            codec.encodeStart(NbtOps.INSTANCE, value)
                    .resultOrPartial(error -> LOGGER.warn("Could not save field entry: {}", error))
                    .ifPresent(list::add);
        }
        return list;
    }
}
