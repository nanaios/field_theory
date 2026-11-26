package com.nanaios.fieldtheory.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.nanaios.fieldtheory.api.FieldRegion;
import com.nanaios.fieldtheory.api.FieldSample;
import com.nanaios.fieldtheory.api.FieldSource;
import com.nanaios.fieldtheory.api.FieldTheoryAPI;
import com.nanaios.fieldtheory.api.FieldType;
import com.nanaios.fieldtheory.api.FieldWorld;
import com.nanaios.fieldtheory.api.charge.Charges;
import java.util.Collection;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * {@code /fieldtheory} — inspect and perturb fields by hand.
 *
 * <pre>
 * /fieldtheory sample &lt;field&gt; [pos]
 * /fieldtheory source add &lt;field&gt; &lt;pos&gt; &lt;charge&gt; [seconds]
 * /fieldtheory source list
 * /fieldtheory source clear [field]
 * /fieldtheory region add &lt;pos&gt; &lt;radius&gt; &lt;gain&gt; [seconds]
 * /fieldtheory region clear
 * /fieldtheory charge &lt;targets&gt; &lt;field&gt; &lt;amount&gt; [seconds]
 * /fieldtheory charge &lt;targets&gt; clear
 * </pre>
 */
public final class FieldCommand {
    private static final ResourceLocation ORIGIN = FieldTheoryAPI.id("command");
    private static final DynamicCommandExceptionType UNKNOWN_FIELD =
            new DynamicCommandExceptionType(id -> Component.translatable("commands.field_theory.unknown_field", String.valueOf(id)));
    private static final SuggestionProvider<CommandSourceStack> FIELDS =
            (context, builder) -> SharedSuggestionProvider.suggestResource(FieldTheoryAPI.fieldTypes().keySet(), builder);

    private FieldCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("fieldtheory")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("sample")
                        .then(Commands.argument("field", ResourceLocationArgument.id()).suggests(FIELDS)
                                .executes(c -> sample(c, c.getSource().getPosition()))
                                .then(Commands.argument("pos", Vec3Argument.vec3())
                                        .executes(c -> sample(c, Vec3Argument.getVec3(c, "pos"))))))
                .then(Commands.literal("source")
                        .then(Commands.literal("add")
                                .then(Commands.argument("field", ResourceLocationArgument.id()).suggests(FIELDS)
                                        .then(Commands.argument("pos", Vec3Argument.vec3())
                                                .then(Commands.argument("charge", DoubleArgumentType.doubleArg(-1000, 1000))
                                                        .executes(c -> addSource(c, -1))
                                                        .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 86400))
                                                                .executes(c -> addSource(c, IntegerArgumentType.getInteger(c, "seconds") * 20)))))))
                        .then(Commands.literal("list").executes(FieldCommand::listSources))
                        .then(Commands.literal("clear")
                                .executes(c -> clearSources(c, null))
                                .then(Commands.argument("field", ResourceLocationArgument.id()).suggests(FIELDS)
                                        .executes(c -> clearSources(c, field(c))))))
                .then(Commands.literal("region")
                        .then(Commands.literal("add")
                                .then(Commands.argument("pos", Vec3Argument.vec3())
                                        .then(Commands.argument("radius", DoubleArgumentType.doubleArg(0.5, 64))
                                                .then(Commands.argument("gain", DoubleArgumentType.doubleArg(0, 16))
                                                        .executes(c -> addRegion(c, -1))
                                                        .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 86400))
                                                                .executes(c -> addRegion(c, IntegerArgumentType.getInteger(c, "seconds") * 20)))))))
                        .then(Commands.literal("clear").executes(FieldCommand::clearRegions)))
                .then(Commands.literal("charge")
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .then(Commands.literal("clear").executes(FieldCommand::clearCharges))
                                .then(Commands.argument("field", ResourceLocationArgument.id()).suggests(FIELDS)
                                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(-1000, 1000))
                                                .executes(c -> charge(c, -1))
                                                .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 86400))
                                                        .executes(c -> charge(c, IntegerArgumentType.getInteger(c, "seconds") * 20))))))));
    }

    private static FieldType field(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ResourceLocation id = ResourceLocationArgument.getId(c, "field");
        FieldType type = FieldTheoryAPI.fieldTypes().get(id);
        if (type == null) {
            throw UNKNOWN_FIELD.create(id);
        }
        return type;
    }

    private static FieldWorld world(CommandContext<CommandSourceStack> c) {
        return FieldTheoryAPI.get(c.getSource().getLevel());
    }

    private static String fmt(double v) {
        return String.format(Locale.ROOT, "%.3f", v);
    }

    private static String fmt(Vec3 v) {
        return String.format(Locale.ROOT, "(%.2f, %.2f, %.2f)", v.x, v.y, v.z);
    }

    private static int sample(CommandContext<CommandSourceStack> c, Vec3 pos) throws CommandSyntaxException {
        FieldType type = field(c);
        FieldSample s = world(c).sample(type, pos);
        c.getSource().sendSuccess(() -> Component.translatable("commands.field_theory.sample",
                type.displayName(), fmt(pos), fmt(s.potential()), fmt(s.ambient()), fmt(s.intensity()), fmt(s.vector()), fmt(s.gain())), false);
        return (int) Math.round(s.intensity() * 1000);
    }

    private static int addSource(CommandContext<CommandSourceStack> c, int durationTicks) throws CommandSyntaxException {
        FieldType type = field(c);
        Vec3 pos = Vec3Argument.getVec3(c, "pos");
        double charge = DoubleArgumentType.getDouble(c, "charge");
        FieldSource source = durationTicks < 0
                ? FieldSource.permanent(UUID.randomUUID(), type, pos, charge, ORIGIN)
                : FieldSource.temporary(c.getSource().getLevel(), type, pos, charge, durationTicks, ORIGIN);
        if (!world(c).putSource(source)) {
            c.getSource().sendFailure(Component.translatable("commands.field_theory.source.cancelled"));
            return 0;
        }
        c.getSource().sendSuccess(() -> Component.translatable("commands.field_theory.source.added", type.displayName(), fmt(charge), fmt(pos)), true);
        return 1;
    }

    private static int listSources(CommandContext<CommandSourceStack> c) {
        Collection<FieldSource> sources = world(c).sources();
        c.getSource().sendSuccess(() -> Component.translatable("commands.field_theory.source.list", sources.size()), false);
        for (FieldSource s : sources) {
            c.getSource().sendSuccess(() -> Component.literal(" - ").append(s.type().displayName())
                    .append(Component.literal(" q=" + fmt(s.charge()) + " @ " + fmt(s.position()) + " [" + s.origin() + "]")), false);
        }
        return sources.size();
    }

    private static int clearSources(CommandContext<CommandSourceStack> c, @Nullable FieldType type) {
        int n = world(c).removeSources(s -> type == null || s.type() == type);
        c.getSource().sendSuccess(() -> Component.translatable("commands.field_theory.source.cleared", n), true);
        return n;
    }

    private static int addRegion(CommandContext<CommandSourceStack> c, int durationTicks) {
        Vec3 pos = Vec3Argument.getVec3(c, "pos");
        double radius = DoubleArgumentType.getDouble(c, "radius");
        double gain = DoubleArgumentType.getDouble(c, "gain");
        long expiresAt = durationTicks < 0 ? -1L : c.getSource().getLevel().getGameTime() + durationTicks;
        world(c).putRegion(new FieldRegion(UUID.randomUUID(), Optional.empty(), pos, radius, gain, expiresAt, ORIGIN));
        c.getSource().sendSuccess(() -> Component.translatable("commands.field_theory.region.added", fmt(gain), fmt(radius), fmt(pos)), true);
        return 1;
    }

    private static int clearRegions(CommandContext<CommandSourceStack> c) {
        int n = world(c).removeRegions(r -> true);
        c.getSource().sendSuccess(() -> Component.translatable("commands.field_theory.region.cleared", n), true);
        return n;
    }

    private static int charge(CommandContext<CommandSourceStack> c, int durationTicks) throws CommandSyntaxException {
        FieldType type = field(c);
        double amount = DoubleArgumentType.getDouble(c, "amount");
        Collection<? extends Entity> targets = EntityArgument.getEntities(c, "targets");
        for (Entity entity : targets) {
            Charges.induce(entity, type, amount, durationTicks);
        }
        c.getSource().sendSuccess(() -> Component.translatable("commands.field_theory.charge.added", type.displayName(), fmt(amount), targets.size()), true);
        return targets.size();
    }

    private static int clearCharges(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Collection<? extends Entity> targets = EntityArgument.getEntities(c, "targets");
        for (Entity entity : targets) {
            Charges.clearInduced(entity, null);
        }
        c.getSource().sendSuccess(() -> Component.translatable("commands.field_theory.charge.cleared", targets.size()), true);
        return targets.size();
    }
}
