package com.nanaios.fieldtheory.api;

/**
 * How a field exerts force on charged matter.
 *
 * <p>The acceleration of an entity with charge {@code Q} and inertial mass {@code m} is
 * {@code a = sign · forceScale · (Q/m) · E}, where {@code E} points away from positive sources.</p>
 */
public enum Coupling {
    /** Like charges repel, opposite charges attract (electrostatics). */
    LIKE_REPELS(1.0),
    /** Like charges attract (gravity, binding forces). */
    LIKE_ATTRACTS(-1.0),
    /** The field carries no mechanical force; it only acts through interactions. */
    NONE(0.0);

    private final double sign;

    Coupling(double sign) {
        this.sign = sign;
    }

    public double sign() {
        return sign;
    }
}
