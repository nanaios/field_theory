package com.nanaios.fieldtheory.api;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The four "standard model" fields shipped with Field Theory, modelled on the four fundamental
 * interactions of physics. Mods are encouraged to share these instead of inventing parallel ones,
 * so that their effects interfere with each other.
 *
 * <table>
 *   <tr><th>field</th><th>analogue</th><th>propagator</th><th>coupling</th></tr>
 *   <tr><td>Gravis</td><td>gravity</td><td>1/r, long range</td><td>like attracts, charge = mass</td></tr>
 *   <tr><td>Fulgur</td><td>electromagnetism</td><td>1/r, long range</td><td>like repels</td></tr>
 *   <tr><td>Nexus</td><td>strong interaction</td><td>Yukawa, λ = 3</td><td>like attracts, very strong</td></tr>
 *   <tr><td>Mutatio</td><td>weak interaction</td><td>Gaussian, σ = 2</td><td>no force: changes identity</td></tr>
 * </table>
 */
public final class FieldTypes {
    /** Internal: registered by Field Theory itself. */
    public static final DeferredRegister<FieldType> REGISTER = DeferredRegister.create(FieldTheoryAPI.FIELD_TYPE_REGISTRY, FieldTheoryAPI.MOD_ID);

    /** 重場 — universal attraction. Every entity carries Gravis charge equal to its mass. */
    public static final DeferredHolder<FieldType, FieldType> GRAVIS = REGISTER.register("gravis", () -> FieldType.builder(Propagator.inverseDistance(0.75))
            .coupling(Coupling.LIKE_ATTRACTS)
            .strength(1.0)
            .range(48.0)
            .forceScale(0.08)
            .color(0x9B6BFF)
            .equivalence()
            .build());

    /** 雷場 — signed charges, like repels like. */
    public static final DeferredHolder<FieldType, FieldType> FULGUR = REGISTER.register("fulgur", () -> FieldType.builder(Propagator.inverseDistance(0.5))
            .coupling(Coupling.LIKE_REPELS)
            .strength(1.0)
            .range(32.0)
            .forceScale(0.06)
            .color(0x4FD8FF)
            .build());

    /** 結場 — short ranged, strong binding. */
    public static final DeferredHolder<FieldType, FieldType> NEXUS = REGISTER.register("nexus", () -> FieldType.builder(Propagator.screened(3.0, 0.5))
            .coupling(Coupling.LIKE_ATTRACTS)
            .strength(2.0)
            .range(16.0)
            .forceScale(0.1)
            .color(0xFF7A3D)
            .build());

    /** 転場 — contact field that changes what matter <em>is</em> rather than where it goes. */
    public static final DeferredHolder<FieldType, FieldType> MUTATIO = REGISTER.register("mutatio", () -> FieldType.builder(Propagator.gaussian(2.0))
            .coupling(Coupling.NONE)
            .strength(1.0)
            .range(8.0)
            .forceScale(0.0)
            .color(0x7CFF6B)
            .build());

    private FieldTypes() {
    }
}
