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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
        cleanup(helper); // a failed earlier test must not leak its fields into this one
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
        cleanup(helper); // a failed earlier test must not leak its fields into this one
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
        cleanup(helper); // a failed earlier test must not leak its fields into this one
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
        cleanup(helper); // a failed earlier test must not leak its fields into this one
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
        cleanup(helper); // a failed earlier test must not leak its fields into this one
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
        FieldSource source = new FieldSource(UUID.randomUUID(), FieldTypes.NEXUS.get(), new Vec3(1.5, -2.0, 3.25), -4.5, 1234L, ORIGIN,
                FieldSource.frequencyForPeriod(16), 0.75);
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
        cleanup(helper); // a failed earlier test must not leak its fields into this one
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
        cleanup(helper); // a failed earlier test must not leak its fields into this one
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

    private static FieldSource wave(GameTestHelper helper, FieldType type, Vec3 relative, double charge, int period, double phase) {
        return new FieldSource(UUID.randomUUID(), type, helper.absoluteVec(relative), charge, helper.getLevel().getGameTime() + 400, ORIGIN,
                FieldSource.frequencyForPeriod(period), phase);
    }

    /** An oscillating source is retarded: φ = Q·P(r)·cos(ωt − kr + φ0), and E is exactly −∇φ. */
    @GameTest(template = LAB, batch = "ft_wave_retarded")
    public static void oscillatingSourceIsRetarded(GameTestHelper helper) {
        cleanup(helper); // a failed earlier test must not leak its fields into this one
        FieldType fulgur = FieldTypes.FULGUR.get();
        FieldWorld world = world(helper);
        FieldSource source = wave(helper, fulgur, new Vec3(4.5, 4.0, 7.5), 5.0, 12, 0.3);
        world.putSource(source);
        Vec3 probe = helper.absoluteVec(new Vec3(7.5, 4.0, 7.5));
        double r = 3.0;
        double k = source.frequency() / fulgur.waveSpeed();
        double expected = fulgur.potential(5.0, r) * Math.cos(source.frequency() * helper.getLevel().getGameTime() - k * r + 0.3);
        FieldSample sample = world.sample(fulgur, probe);
        helper.assertTrue(Math.abs(sample.sourcePotential() - expected) < 1.0E-9, "retarded φ " + sample.sourcePotential() + " != " + expected);

        // Test positions are ~10^7 blocks out, where doubles resolve ~2e-9: keep the step coarse.
        // A wrong retardation term (k·P·sin θ ≈ 1.7 here) would still be off by orders of magnitude.
        double h = 1.0E-3;
        double plus = world.sample(fulgur, probe.add(h, 0, 0)).potential();
        double minus = world.sample(fulgur, probe.add(-h, 0, 0)).potential();
        double numeric = -(plus - minus) / (2 * h);
        helper.assertTrue(Math.abs(sample.vector().x - numeric) < 1.0E-4, "E_x " + sample.vector().x + " != -dφ/dx " + numeric);
        cleanup(helper);
        helper.succeed();
    }

    /** Two coherent in-phase sources: amplitudes add as phasors (doubling on the mid plane). */
    @GameTest(template = LAB, batch = "ft_wave_inphase")
    public static void inPhaseSourcesInterfere(GameTestHelper helper) {
        cleanup(helper); // a failed earlier test must not leak its fields into this one
        FieldType fulgur = FieldTypes.FULGUR.get();
        FieldWorld world = world(helper);
        int period = 12;
        double k = FieldSource.frequencyForPeriod(period) / fulgur.waveSpeed();
        world.putSource(wave(helper, fulgur, new Vec3(4.5, 4.0, 7.5), 5.0, period, 0.0));
        world.putSource(wave(helper, fulgur, new Vec3(10.5, 4.0, 7.5), 5.0, period, 0.0));

        double mid = world.amplitude(fulgur, helper.absoluteVec(new Vec3(7.5, 4.0, 7.5)));
        double expectedMid = 2 * fulgur.potential(5.0, 3.0);
        helper.assertTrue(Math.abs(mid - expectedMid) < 1.0E-9, "mid-plane amplitude " + mid + " != " + expectedMid);

        // λ = 6: a quarter wavelength off the mid point the paths differ by λ/2.
        double r1 = 4.5;
        double r2 = 1.5;
        double re = fulgur.potential(5.0, r1) * Math.cos(-k * r1) + fulgur.potential(5.0, r2) * Math.cos(-k * r2);
        double im = fulgur.potential(5.0, r1) * Math.sin(-k * r1) + fulgur.potential(5.0, r2) * Math.sin(-k * r2);
        double off = world.amplitude(fulgur, helper.absoluteVec(new Vec3(9.0, 4.0, 7.5)));
        helper.assertTrue(Math.abs(off - Math.hypot(re, im)) < 1.0E-9, "phasor sum " + off + " != " + Math.hypot(re, im));
        cleanup(helper);
        helper.succeed();
    }

    /** Two coherent sources in antiphase cancel exactly on their mid plane. */
    @GameTest(template = LAB, batch = "ft_wave_antiphase")
    public static void antiPhaseSourcesCancel(GameTestHelper helper) {
        cleanup(helper); // a failed earlier test must not leak its fields into this one
        FieldType fulgur = FieldTypes.FULGUR.get();
        FieldWorld world = world(helper);
        world.putSource(wave(helper, fulgur, new Vec3(4.5, 4.0, 7.5), 5.0, 16, 0.0));
        world.putSource(wave(helper, fulgur, new Vec3(10.5, 4.0, 7.5), 5.0, 16, Math.PI));
        Vec3 mid = helper.absoluteVec(new Vec3(7.5, 6.0, 9.5));
        helper.assertTrue(world.amplitude(fulgur, mid) < 1.0E-9, "antiphase amplitude on the mid plane should vanish");
        helper.assertTrue(Math.abs(world.sample(fulgur, mid).sourcePotential()) < 1.0E-9, "instantaneous φ on the mid plane should vanish");
        helper.assertTrue(world.amplitude(fulgur, helper.absoluteVec(new Vec3(5.5, 4.0, 7.5))) > 0.5, "off the mid plane there is signal");
        cleanup(helper);
        helper.succeed();
    }

    /** Regions are found through the column index; the gain must equal the product over all regions. */
    @GameTest(template = LAB, batch = "ft_region_index")
    public static void regionIndexMatchesBruteForce(GameTestHelper helper) {
        cleanup(helper); // a failed earlier test must not leak its fields into this one
        FieldWorld world = world(helper);
        FieldType nexus = FieldTypes.NEXUS.get();
        Vec3 origin = helper.absoluteVec(new Vec3(7.5, 4.0, 7.5));
        Random random = new Random(7);
        for (int i = 0; i < 60; i++) {
            Vec3 c = origin.add((random.nextDouble() - 0.5) * 80, (random.nextDouble() - 0.5) * 10, (random.nextDouble() - 0.5) * 80);
            Optional<FieldType> type = random.nextBoolean() ? Optional.empty() : Optional.of(nexus);
            world.putRegion(FieldRegion.temporary(helper.getLevel(), type, c, 2 + random.nextDouble() * 20, random.nextDouble() * 3, 400, ORIGIN));
        }
        for (int i = 0; i < 40; i++) {
            Vec3 p = origin.add((random.nextDouble() - 0.5) * 60, (random.nextDouble() - 0.5) * 8, (random.nextDouble() - 0.5) * 60);
            double expected = 1.0;
            for (FieldRegion r : world.regions()) {
                if (r.affects(nexus)) {
                    expected *= r.gainAt(p);
                }
            }
            double actual = world.gain(nexus, p);
            helper.assertTrue(Math.abs(actual - expected) < 1.0E-12, "indexed gain " + actual + " != brute force " + expected + " at " + p);
        }
        cleanup(helper);
        helper.succeed();
    }

    /**
     * Dielectrophoresis: a dropped iron ingot drifts into a strong field even though the source is
     * negative (the force follows ∇|E|², not the sign); dirt is not a conductor and stays put.
     */
    @GameTest(template = LAB, batch = "ft_polarization", timeoutTicks = 100)
    public static void conductorsArePulledIntoStrongFields(GameTestHelper helper) {
        cleanup(helper); // a failed earlier test must not leak its fields into this one
        put(helper, FieldTypes.FULGUR.get(), new Vec3(7.5, 4.0, 7.5), -12.0);
        ItemEntity iron = floatingItem(helper, Items.IRON_INGOT, new Vec3(4.5, 4.0, 7.5));
        ItemEntity dirt = floatingItem(helper, Items.DIRT, new Vec3(10.5, 4.0, 7.5));
        Vec3 source = helper.absoluteVec(new Vec3(7.5, 4.0, 7.5));
        Vec3 dirtStart = dirt.position();
        // In mid-air nothing damps the motion: the ingot falls into the field maximum and swings
        // through it. Judge by its closest approach rather than its final position.
        double[] closest = {iron.position().distanceTo(source)};
        helper.onEachTick(() -> closest[0] = Math.min(closest[0], iron.position().distanceTo(source)));
        helper.runAfterDelay(40, () -> {
            double dirtMoved = dirt.position().distanceTo(dirtStart);
            iron.discard();
            dirt.discard();
            cleanup(helper);
            helper.assertTrue(closest[0] < 1.0, "iron should be drawn into the strong field, closest approach " + closest[0]);
            helper.assertTrue(dirtMoved < 0.01, "dirt is no conductor and must not move, moved " + dirtMoved);
            helper.succeed();
        });
    }

    private static ItemEntity floatingItem(GameTestHelper helper, net.minecraft.world.item.Item item, Vec3 relative) {
        Vec3 at = helper.absoluteVec(relative);
        ItemEntity entity = new ItemEntity(helper.getLevel(), at.x, at.y, at.z, new ItemStack(item));
        entity.setNoGravity(true);
        entity.setDeltaMovement(Vec3.ZERO);
        entity.setNeverPickUp();
        entity.setUnlimitedLifetime();
        helper.getLevel().addFreshEntity(entity);
        return entity;
    }
}
