package com.nanaios.fieldtheory.api;

/**
 * The radial Green's function of a field: how the influence of a unit charge falls off with
 * distance {@code r}.
 *
 * <p>{@link #potential(double)} returns φ(r) per unit charge and {@link #field(double)} returns
 * the radial field strength {@code -dφ/dr}. A positive value of {@code field} points away from a
 * positive charge.</p>
 */
public interface Propagator {

    double potential(double r);

    double field(double r);

    /**
     * Coulomb / Newton like long range field, φ = 1/r. The singularity at the origin is softened
     * (Plummer softening) so that φ = 1/sqrt(r² + ε²).
     */
    static Propagator inverseDistance(double softening) {
        return new InverseDistance(softening);
    }

    /**
     * Yukawa (screened) field, φ = e^(-r/λ)/r: behaves like 1/r close to the source and dies off
     * exponentially beyond the screening length λ. Models short range "binding" forces.
     */
    static Propagator screened(double screeningLength, double softening) {
        return new Screened(screeningLength, softening);
    }

    /** Contact interaction, φ = exp(-r²/2σ²). Only matters in the immediate vicinity of a source. */
    static Propagator gaussian(double width) {
        return new Gaussian(width);
    }

    record InverseDistance(double softening) implements Propagator {
        @Override
        public double potential(double r) {
            return 1.0 / Math.sqrt(r * r + softening * softening);
        }

        @Override
        public double field(double r) {
            double d2 = r * r + softening * softening;
            return r / (d2 * Math.sqrt(d2));
        }
    }

    record Screened(double screeningLength, double softening) implements Propagator {
        @Override
        public double potential(double r) {
            return Math.exp(-r / screeningLength) / Math.sqrt(r * r + softening * softening);
        }

        @Override
        public double field(double r) {
            double d2 = r * r + softening * softening;
            double d = Math.sqrt(d2);
            return Math.exp(-r / screeningLength) * (r / (d2 * d) + 1.0 / (screeningLength * d));
        }
    }

    record Gaussian(double width) implements Propagator {
        @Override
        public double potential(double r) {
            return Math.exp(-r * r / (2.0 * width * width));
        }

        @Override
        public double field(double r) {
            return r / (width * width) * Math.exp(-r * r / (2.0 * width * width));
        }
    }
}
