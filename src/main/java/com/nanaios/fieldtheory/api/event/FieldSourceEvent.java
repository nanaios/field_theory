package com.nanaios.fieldtheory.api.event;

import com.nanaios.fieldtheory.api.FieldSource;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/** Fired on {@code NeoForge.EVENT_BUS} (logical server only) when sources appear or vanish. */
public abstract class FieldSourceEvent extends Event {
    private final Level level;
    private final FieldSource source;

    protected FieldSourceEvent(Level level, FieldSource source) {
        this.level = level;
        this.source = source;
    }

    public Level getLevel() {
        return level;
    }

    public FieldSource getSource() {
        return source;
    }

    /** A new source is about to be added. Cancel to forbid it (e.g. an anti-magic zone). */
    public static class Added extends FieldSourceEvent implements ICancellableEvent {
        public Added(Level level, FieldSource source) {
            super(level, source);
        }
    }

    /** A source was removed or expired. */
    public static class Removed extends FieldSourceEvent {
        public Removed(Level level, FieldSource source) {
            super(level, source);
        }
    }
}
