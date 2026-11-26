package com.nanaios.fieldtheory.api;

import net.minecraft.world.phys.Vec3;

/**
 * Value of one field at one point.
 *
 * @param potential total scalar potential φ (ambient included, region gain applied)
 * @param ambient   the part of {@code potential} that comes from the ambient background
 * @param vector    field vector E = -∇φ produced by sources (ambient is uniform and has no gradient)
 * @param gain      product of all region gains at this point (1 = untouched, 0 = fully shielded)
 */
public record FieldSample(double potential, double ambient, Vec3 vector, double gain) {
    public static final FieldSample ZERO = new FieldSample(0.0, 0.0, Vec3.ZERO, 1.0);

    /** |E|. */
    public double intensity() {
        return vector.length();
    }

    /** Potential produced by sources only (ambient removed). */
    public double sourcePotential() {
        return potential - ambient;
    }
}
