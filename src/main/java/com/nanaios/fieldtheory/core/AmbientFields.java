package com.nanaios.fieldtheory.core;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import com.nanaios.fieldtheory.api.FieldType;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

/** Holds the ambient field definitions for each logical side. */
public final class AmbientFields {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static volatile List<AmbientEntry> serverEntries = List.of();
    private static volatile Map<FieldType, List<AmbientEntry>> serverByType = Map.of();
    private static volatile Map<FieldType, List<AmbientEntry>> clientByType = Map.of();

    private AmbientFields() {
    }

    public static double evaluate(Level level, FieldType type, Vec3 pos) {
        List<AmbientEntry> entries = (level.isClientSide() ? clientByType : serverByType).get(type);
        if (entries == null) {
            return 0.0;
        }
        double sum = 0.0;
        for (AmbientEntry entry : entries) {
            sum += entry.evaluate(level, pos);
        }
        return sum;
    }

    public static List<AmbientEntry> serverEntries() {
        return serverEntries;
    }

    static void setServer(List<AmbientEntry> entries) {
        serverEntries = List.copyOf(entries);
        serverByType = group(entries);
    }

    public static void setClient(List<AmbientEntry> entries) {
        clientByType = group(entries);
    }

    private static Map<FieldType, List<AmbientEntry>> group(List<AmbientEntry> entries) {
        Map<FieldType, List<AmbientEntry>> map = new IdentityHashMap<>();
        for (AmbientEntry entry : entries) {
            map.computeIfAbsent(entry.field(), t -> new ArrayList<>()).add(entry);
        }
        return map;
    }

    public static final class Loader extends SimpleJsonResourceReloadListener {
        public static final String DIRECTORY = "field_theory/ambient";

        public Loader() {
            super(new Gson(), DIRECTORY);
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
            List<AmbientEntry> entries = new ArrayList<>();
            files.forEach((id, json) -> {
                if (json.isJsonArray()) {
                    json.getAsJsonArray().forEach(element -> parse(id, element, entries));
                } else {
                    parse(id, json, entries);
                }
            });
            setServer(entries);
            LOGGER.info("Loaded {} ambient field entries", entries.size());
        }

        private static void parse(ResourceLocation id, JsonElement json, List<AmbientEntry> out) {
            AmbientEntry.CODEC.parse(JsonOps.INSTANCE, json)
                    .resultOrPartial(error -> LOGGER.error("Invalid ambient field entry {}: {}", id, error))
                    .ifPresent(out::add);
        }
    }
}
