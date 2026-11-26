package com.nanaios.fieldtheory.core;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server config ({@code serverconfig/field_theory-server.toml}). Server configs are synchronised
 * to clients, which matters because the local player's motion is integrated client-side.
 */
public final class FieldTheoryConfig {
    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.DoubleValue FORCE_MULTIPLIER;
    private static final ModConfigSpec.DoubleValue MAX_ACCELERATION;
    private static final ModConfigSpec.BooleanValue AFFECT_PLAYERS;
    private static final ModConfigSpec.IntValue MAX_SOURCES_PER_LEVEL;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.translation("config.field_theory.physics").push("physics");
        FORCE_MULTIPLIER = b
                .comment("Global multiplier of every field-induced force (0 disables field forces).")
                .translation("config.field_theory.forceMultiplier")
                .defineInRange("forceMultiplier", 1.0, 0.0, 10.0);
        MAX_ACCELERATION = b
                .comment("Upper bound of the field-induced acceleration of an entity, in blocks/tick² (vanilla gravity is 0.08).")
                .translation("config.field_theory.maxAcceleration")
                .defineInRange("maxAcceleration", 0.5, 0.0, 4.0);
        AFFECT_PLAYERS = b
                .comment("Whether field forces move players.")
                .translation("config.field_theory.affectPlayers")
                .define("affectPlayers", true);
        b.pop();
        b.translation("config.field_theory.limits").push("limits");
        MAX_SOURCES_PER_LEVEL = b
                .comment("Maximum number of point sources per dimension. New sources beyond this are rejected.")
                .translation("config.field_theory.maxSourcesPerLevel")
                .defineInRange("maxSourcesPerLevel", 4096, 0, 1_000_000);
        b.pop();
        SPEC = b.build();
    }

    private FieldTheoryConfig() {
    }

    public static double forceMultiplier() {
        return SPEC.isLoaded() ? FORCE_MULTIPLIER.get() : FORCE_MULTIPLIER.getDefault();
    }

    public static double maxAcceleration() {
        return SPEC.isLoaded() ? MAX_ACCELERATION.get() : MAX_ACCELERATION.getDefault();
    }

    public static boolean affectPlayers() {
        return SPEC.isLoaded() ? AFFECT_PLAYERS.get() : AFFECT_PLAYERS.getDefault();
    }

    public static int maxSourcesPerLevel() {
        return SPEC.isLoaded() ? MAX_SOURCES_PER_LEVEL.get() : MAX_SOURCES_PER_LEVEL.getDefault();
    }
}
