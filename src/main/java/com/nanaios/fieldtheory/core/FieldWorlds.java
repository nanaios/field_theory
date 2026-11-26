package com.nanaios.fieldtheory.core;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/** Owns the {@link FieldWorldImpl} of every loaded level, one map per logical side. */
public final class FieldWorlds {
    /** Server thread only. */
    private static final Map<Level, FieldWorldImpl> SERVER = new IdentityHashMap<>();
    /** Client thread only. Weak so that preview/fake client levels don't leak. */
    private static final Map<Level, FieldWorldImpl> CLIENT = new WeakHashMap<>();

    private FieldWorlds() {
    }

    public static FieldWorldImpl get(Level level) {
        if (level.isClientSide()) {
            return CLIENT.computeIfAbsent(level, l -> new FieldWorldImpl(l, false));
        }
        FieldWorldImpl world = SERVER.get(level);
        if (world == null) {
            world = new FieldWorldImpl(level, true);
            if (level instanceof ServerLevel serverLevel) {
                FieldSavedData data = serverLevel.getDataStorage().computeIfAbsent(FieldSavedData.factory(), FieldSavedData.NAME);
                data.attach(world);
            }
            SERVER.put(level, world);
        }
        return world;
    }

    static List<FieldWorldImpl> serverWorlds() {
        return new ArrayList<>(SERVER.values());
    }

    static void unload(Level level) {
        if (level.isClientSide()) {
            CLIENT.remove(level);
        } else {
            SERVER.remove(level);
        }
    }

    static void clearServer() {
        SERVER.clear();
    }

    public static void clearClient() {
        CLIENT.clear();
    }
}
