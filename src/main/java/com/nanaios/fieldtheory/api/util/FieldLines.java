package com.nanaios.fieldtheory.api.util;

import com.nanaios.fieldtheory.api.FieldType;
import com.nanaios.fieldtheory.api.FieldWorld;
import java.util.function.Predicate;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Geometry helpers for visualising fields. */
public final class FieldLines {
    private FieldLines() {
    }

    /**
     * Traces a field line (streamline of E) with a midpoint (RK2) integrator.
     *
     * @param forward {@code true} to follow E, {@code false} to follow -E
     * @param visitor receives each new point; return {@code false} to stop
     * @return number of steps taken
     */
    public static int trace(FieldWorld world, FieldType type, Vec3 start, boolean forward, double step, int maxSteps, Predicate<Vec3> visitor) {
        double sign = forward ? 1.0 : -1.0;
        Vec3 pos = start;
        for (int i = 0; i < maxSteps; i++) {
            Vec3 e1 = world.sample(type, pos).vector();
            double l1 = e1.length();
            if (l1 < 1.0E-6) {
                return i;
            }
            Vec3 mid = pos.add(e1.scale(sign * step * 0.5 / l1));
            Vec3 e2 = world.sample(type, mid).vector();
            double l2 = e2.length();
            if (l2 < 1.0E-6) {
                return i;
            }
            pos = pos.add(e2.scale(sign * step / l2));
            if (!visitor.test(pos)) {
                return i + 1;
            }
        }
        return maxSteps;
    }

    /** {@code n} roughly uniformly distributed unit vectors (Fibonacci sphere). */
    public static Vec3[] fibonacciSphere(int n) {
        Vec3[] out = new Vec3[n];
        double golden = Math.PI * (3.0 - Math.sqrt(5.0));
        for (int i = 0; i < n; i++) {
            double y = 1.0 - (i + 0.5) * 2.0 / n;
            double r = Math.sqrt(Math.max(0.0, 1.0 - y * y));
            double theta = golden * i;
            out[i] = new Vec3(Mth.cos((float) theta) * r, y, Mth.sin((float) theta) * r);
        }
        return out;
    }
}
