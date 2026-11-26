package com.nanaios.fieldtheory.api.interaction;

/**
 * A rule describing how fields act on an entity — the "相互作用" between a field and matter.
 *
 * <p>Interactions are registered in {@link com.nanaios.fieldtheory.api.FieldTheoryAPI#INTERACTION_REGISTRY}
 * and evaluated once per tick for every entity:</p>
 * <ul>
 *   <li>on the logical server for every entity ({@link FieldContext#isServerSide()} is true);</li>
 *   <li>on the client for the local player only, because players simulate their own motion.</li>
 * </ul>
 *
 * <p>Change motion only through {@link FieldContext#accelerate} — the driver applies the summed
 * acceleration on the side that owns the entity's motion. Do world-changing work (damage, blocks)
 * only when {@link FieldContext#isServerSide()}.</p>
 */
@FunctionalInterface
public interface FieldInteraction {
    void apply(FieldContext context);
}
