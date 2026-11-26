package com.nanaios.fieldtheory.gametest;

import com.nanaios.fieldtheory.api.FieldRegion;
import com.nanaios.fieldtheory.api.FieldSample;
import com.nanaios.fieldtheory.api.FieldSource;
import com.nanaios.fieldtheory.api.FieldTheoryAPI;
import com.nanaios.fieldtheory.api.FieldType;
import com.nanaios.fieldtheory.api.FieldTypes;
import com.nanaios.fieldtheory.api.FieldWorld;
import com.nanaios.fieldtheory.api.charge.Charges;
import com.nanaios.fieldtheory.api.charge.EntityCharges;
import com.nanaios.fieldtheory.core.FieldTheoryConfig;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * In-world tests of the field physics. Every test lives in its own batch so that long-range
 * fields of one test never leak into another.
 *
 * <p>Run with {@code ./gradlew runGameTestServer}.</p>
 */
@GameTestHolder(FieldTheoryAPI.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FieldTheoryGameTests {
    private static final String LAB = "field_lab";
    private static final ResourceLocation ORIGIN = FieldTheoryAPI.id("gametest");
    private static final double EPS = 1.0E-9;
    // The structure is placed one block above the structure block: the floor's top surface is at y = 2.

    private FieldTheoryGameTests() {
    }

    private static FieldWorld world(GameTestHelper helper) {
        return FieldTheoryAPI.get(helper.getLevel());
    }

    private static void cleanup(GameTestHelper helper) {
        world(helper).removeSources(s -> s.origin().equals(ORIGIN));
        world(helper).removeRegions(r -> r.origin().equals(ORIGIN));
    }

    private static UUID put(GameTestHelper helper, FieldType type, Vec3 relative, double charge) {
        FieldSource source = FieldSource.temporary(helper.getLevel(), type, helper.absoluteVec(relative), charge, 400, ORIGIN);
        world(helper).putSource(source);
        return source.id();
    }

    /** Equal charges: E cancels and φ doubles at the midpoint. Opposite charges: the reverse. */
    @GameTest(template = LAB, batch = "ft_superposition")
    public static void superposition(GameTestHelper helper) {
        FieldType fulgur = FieldTypes.FULGUR.get();
        FieldWorld world = world(helper);
        Vec3 mid = helper.absoluteVec(new Vec3(7.5, 3.0, 7.5));
        put(helper, fulgur, new Vec3(4.5, 3.0, 7.5), 5.0);
        UUID right = put(helper, fulgur, new Vec3(10.5, 3.0, 7.5), 5.0);

        FieldSample same = world.sample(fulgur, mid);
        double single = fulgur.potential(5.0, 3.0);
        helper.assertTrue(same.intensity() < EPS, "E of two equal charges should cancel, got " + same.vector());
        helper.assertTrue(Math.abs(same.sourcePotential() - 2 * single) < EPS, "φ should double, got " + same.sourcePotential());

        world.putSource(world.source(right).orElseThrow().withCharge(-5.0));
        FieldSample dipole = world.sample(fulgur, mid);
        helper.assertTrue(Math.abs(dipole.sourcePotential()) < EPS, "φ of a dipole should vanish on the mid plane, got " + dipole.sourcePotential());
        helper.assertTrue(Math.abs(dipole.vector().x - 2 * fulgur.field(5.0, 3.0)) < EPS, "E of a dipole should point from + to -, got " + dipole.vector());
        cleanup(helper);
        helper.succeed();
    }

    /** Gravis pulls every body (charge = mass) toward a positive source. */
    @GameTest(template = LAB, batch = "ft_gravis", timeoutTicks = 100)
    public static void gravisAttracts(GameTestHelper helper) {
        ArmorStand stand = helper.spawn(EntityType.ARMOR_STAND, new Vec3(4.5, 2.0, 7.5));
        double startX = stand.getX();
        put(helper, FieldTypes.GRAVIS.get(), new Vec3(8.5, 3.0, 7.5), 30.0);
        helper.succeedWhen(() -> {
            helper.assertTrue(stand.getX() > startX + 1.0, "stand should fall toward the gravis well, x=" + stand.getX());
            cleanup(helper);
        });
    }

    /** Like Fulgur charges repel; neutral matter does not feel Fulgur. */
    @GameTest(template = LAB, batch = "ft_fulgur", timeoutTicks = 100)
    public static void fulgurRepelsLikeCharges(GameTestHelper helper) {
        ArmorStand charged = helper.spawn(EntityType.ARMOR_STAND, new Vec3(9.5, 2.0, 7.5));
        ArmorStand neutral = helper.spawn(EntityType.ARMOR_STAND, new Vec3(5.5, 2.0, 7.5));
        Charges.induce(charged, FieldTypes.FULGUR.get(), 3.0, 200);
        double chargedX = charged.getX();
        double neutralX = neutral.getX();
        put(helper, FieldTypes.FULGUR.get(), new Vec3(7.5, 3.0, 7.5), 10.0);
        helper.succeedWhen(() -> {
            helper.assertTrue(charged.getX() > chargedX + 1.0, "charged stand should be repelled, x=" + charged.getX());
            helper.assertTrue(Math.abs(neutral.getX() - neutralX) < 0.01, "neutral stand should not move, x=" + neutral.getX());
            cleanup(helper);
        });
    }

    /** A region with gain 0 shields everything inside it. */
    @GameTest(template = LAB, batch = "ft_shield", timeoutTicks = 100)
    public static void shieldBlocksField(GameTestHelper helper) {
        ArmorStand stand = helper.spawn(EntityType.ARMOR_STAND, new Vec3(4.5, 2.0, 7.5));
        double startX = stand.getX();
        world(helper).putRegion(FieldRegion.temporary(helper.getLevel(), Optional.empty(),
                helper.absoluteVec(new Vec3(4.5, 3.0, 7.5)), 2.0, 0.0, 200, ORIGIN));
        put(helper, FieldTypes.GRAVIS.get(), new Vec3(8.5, 3.0, 7.5), 30.0);
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(Math.abs(stand.getX() - startX) < 0.01, "shielded stand should not move, x=" + stand.getX());
            cleanup(helper);
            helper.succeed();
        });
    }

    /** A charged entity is itself a source: it pulls its neighbour (entity-entity interaction). */
    @GameTest(template = LAB, batch = "ft_emitter", timeoutTicks = 100)
    public static void chargedEntityIsASource(GameTestHelper helper) {
        ArmorStand heavy = helper.spawn(EntityType.ARMOR_STAND, new Vec3(9.5, 2.0, 7.5));
        ArmorStand light = helper.spawn(EntityType.ARMOR_STAND, new Vec3(5.5, 2.0, 7.5));
        Charges.induce(heavy, FieldTypes.GRAVIS.get(), 40.0, 200);
        double startX = light.getX();
        helper.succeedWhen(() -> {
            helper.assertTrue(world(helper).emitters(FieldTypes.GRAVIS.get()).stream().anyMatch(e -> e.entity() == heavy), "heavy stand should be an emitter");
            helper.assertTrue(light.getX() > startX + 1.0, "light stand should be pulled toward the heavy one, x=" + light.getX());
            Charges.clearInduced(heavy, null);
        });
    }

    /** Induced charges expire and the entity stops being an emitter. */
    @GameTest(template = LAB, batch = "ft_expiry", timeoutTicks = 100)
    public static void chargesExpire(GameTestHelper helper) {
        ArmorStand stand = helper.spawn(EntityType.ARMOR_STAND, new Vec3(7.5, 2.0, 7.5));
        Charges.induce(stand, FieldTypes.FULGUR.get(), 2.0, 20);
        helper.assertTrue(Charges.charge(stand, FieldTypes.FULGUR.get()) == 2.0, "charge should be induced");
        helper.runAfterDelay(45, () -> {
            helper.assertTrue(Charges.induced(stand).isEmpty(), "charge should have expired");
            helper.assertTrue(world(helper).emitters(FieldTypes.FULGUR.get()).isEmpty(), "no Fulgur emitter should remain");
            helper.succeed();
        });
    }

    /** The overworld has a Gravis vacuum value of 1 (from the bundled data pack). */
    @GameTest(template = LAB, batch = "ft_ambient")
    public static void ambientFromDataPack(GameTestHelper helper) {
        double gravis = world(helper).ambient(FieldTypes.GRAVIS.get(), helper.absoluteVec(new Vec3(7.5, 2.0, 7.5)));
        helper.assertTrue(Math.abs(gravis - 1.0) < EPS, "overworld Gravis ambient should be 1.0, got " + gravis);
        helper.succeed();
    }

    /** Sources and charges survive an NBT round trip. */
    @GameTest(template = LAB, batch = "ft_codec")
    public static void codecsRoundTrip(GameTestHelper helper) {
        FieldSource source = new FieldSource(UUID.randomUUID(), FieldTypes.NEXUS.get(), new Vec3(1.5, -2.0, 3.25), -4.5, 1234L, ORIGIN);
        Tag tag = FieldSource.CODEC.encodeStart(NbtOps.INSTANCE, source).getOrThrow();
        helper.assertTrue(source.equals(FieldSource.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow()), "FieldSource NBT round trip failed");

        EntityCharges charges = EntityCharges.EMPTY.with(FieldTypes.MUTATIO.get(), new EntityCharges.Charge(1.25, -1L));
        Tag chargeTag = EntityCharges.CODEC.encodeStart(NbtOps.INSTANCE, charges).getOrThrow();
        helper.assertTrue(charges.equals(EntityCharges.CODEC.parse(NbtOps.INSTANCE, chargeTag).getOrThrow()), "EntityCharges NBT round trip failed");
        helper.succeed();
    }

    /** With more than 32 sources the column grid is used; it must agree with a brute-force sum. */
    @GameTest(template = LAB, batch = "ft_grid")
    public static void gridIndexMatchesBruteForce(GameTestHelper helper) {
        FieldType fulgur = FieldTypes.FULGUR.get();
        FieldWorld world = world(helper);
        Vec3 probe = helper.absoluteVec(new Vec3(7.5, 4.0, 7.5));
        Random random = new Random(42);
        for (int i = 0; i < 120; i++) {
            Vec3 at = probe.add((random.nextDouble() - 0.5) * 100, (random.nextDouble() - 0.5) * 20, (random.nextDouble() - 0.5) * 100);
            world.putSource(FieldSource.temporary(helper.getLevel(), fulgur, at, random.nextDouble() * 10 - 5, 400, ORIGIN));
        }
        double phi = 0;
        double ex = 0;
        for (FieldSource s : world.sources()) {
            if (s.type() != fulgur) {
                continue;
            }
            Vec3 d = probe.subtract(s.position());
            double r = d.length();
            if (r > fulgur.range()) {
                continue;
            }
            phi += fulgur.potential(s.charge(), r);
            ex += fulgur.field(s.charge(), r) * d.x / r;
        }
        FieldSample sample = world.sample(fulgur, probe);
        helper.assertTrue(Math.abs(sample.sourcePotential() - phi) < 1.0E-9, "grid φ " + sample.sourcePotential() + " != brute force " + phi);
        helper.assertTrue(Math.abs(sample.vector().x - ex) < 1.0E-9, "grid Ex " + sample.vector().x + " != brute force " + ex);
        cleanup(helper);
        helper.succeed();
    }

    /** New sources beyond the configured maximum per dimension are rejected. */
    @GameTest(template = LAB, batch = "ft_limit")
    public static void sourceLimitIsEnforced(GameTestHelper helper) {
        FieldWorld world = world(helper);
        int room = FieldTheoryConfig.maxSourcesPerLevel() - world.sources().size();
        Vec3 at = helper.absoluteVec(new Vec3(7.5, 4.0, 7.5));
        for (int i = 0; i < room; i++) {
            helper.assertTrue(world.putSource(FieldSource.temporary(helper.getLevel(), FieldTypes.MUTATIO.get(), at, 0.001, 400, ORIGIN)),
                    "source " + i + " should fit under the limit");
        }
        FieldSource extra = FieldSource.temporary(helper.getLevel(), FieldTypes.MUTATIO.get(), at, 0.001, 400, ORIGIN);
        helper.assertTrue(!world.putSource(extra), "a source beyond the limit must be rejected");
        cleanup(helper);
        helper.assertTrue(world.putSource(extra), "after cleanup there is room again");
        cleanup(helper);
        helper.succeed();
    }
}
