package com.nanaios.fieldtheory.core;

import com.nanaios.fieldtheory.api.FieldSample;
import com.nanaios.fieldtheory.api.FieldType;
import com.nanaios.fieldtheory.api.FieldWorld;
import com.nanaios.fieldtheory.api.charge.Charges;
import com.nanaios.fieldtheory.api.interaction.FieldContext;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Reusable, thread-confined context (one instance per logical side). */
final class FieldContextImpl implements FieldContext {
    private final Map<FieldType, FieldSample> samples = new IdentityHashMap<>();
    private FieldWorldImpl world;
    private Entity entity;
    private boolean serverSide;
    private boolean controlsMotion;
    private Vec3 center;
    private double mass;
    private double ax;
    private double ay;
    private double az;
    private double damping;

    void reset(FieldWorldImpl world, Entity entity, boolean serverSide, boolean controlsMotion) {
        this.world = world;
        this.entity = entity;
        this.serverSide = serverSide;
        this.controlsMotion = controlsMotion;
        this.center = Charges.center(entity);
        this.mass = Double.NaN;
        this.samples.clear();
        this.ax = 0.0;
        this.ay = 0.0;
        this.az = 0.0;
        this.damping = 1.0;
    }

    void release() {
        this.world = null;
        this.entity = null;
        this.samples.clear();
    }

    double damping() {
        return damping;
    }

    @Override
    public Entity entity() {
        return entity;
    }

    @Override
    public Level level() {
        return entity.level();
    }

    @Override
    public FieldWorld world() {
        return world;
    }

    @Override
    public boolean isServerSide() {
        return serverSide;
    }

    @Override
    public boolean controlsMotion() {
        return controlsMotion;
    }

    @Override
    public Vec3 center() {
        return center;
    }

    @Override
    public double mass() {
        if (Double.isNaN(mass)) {
            mass = Charges.mass(entity);
        }
        return mass;
    }

    @Override
    public double charge(FieldType type) {
        return Charges.charge(entity, type);
    }

    @Override
    public FieldSample sample(FieldType type) {
        FieldSample sample = samples.get(type);
        if (sample == null) {
            sample = world.sample(type, center, entity);
            samples.put(type, sample);
        }
        return sample;
    }

    @Override
    public void accelerate(Vec3 acceleration) {
        ax += acceleration.x;
        ay += acceleration.y;
        az += acceleration.z;
    }

    @Override
    public void damp(double factor) {
        damping *= Math.max(0.0, Math.min(1.0, factor));
    }

    @Override
    public Vec3 acceleration() {
        return new Vec3(ax, ay, az);
    }
}
