package com.nanaios.fieldtheory.core;

import com.nanaios.fieldtheory.api.charge.Charges;
import com.nanaios.fieldtheory.api.charge.EntityCharges;
import com.nanaios.fieldtheory.command.FieldCommand;
import com.nanaios.fieldtheory.network.AmbientPayload;
import com.nanaios.fieldtheory.network.EntityChargesPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Game-bus wiring of Field Theory. */
public final class FieldEvents {
    private FieldEvents() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(FieldEvents::onLevelTick);
        bus.addListener(InteractionDriver::onEntityTick);
        // Lowest priority + ignore cancelled: only track entities that really enter the level.
        bus.addListener(EventPriority.LOWEST, false, EntityJoinLevelEvent.class, FieldEvents::onEntityJoin);
        bus.addListener(FieldEvents::onEntityLeave);
        bus.addListener(FieldEvents::onLogin);
        bus.addListener(FieldEvents::onChangedDimension);
        bus.addListener(FieldEvents::onRespawn);
        bus.addListener(FieldEvents::onStartTracking);
        bus.addListener(FieldEvents::onDatapackSync);
        bus.addListener(FieldEvents::onAddReloadListeners);
        bus.addListener(FieldEvents::onRegisterCommands);
        bus.addListener(FieldEvents::onLevelUnload);
        bus.addListener(FieldEvents::onServerStopped);
    }

    private static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            FieldWorlds.get(level).tickServer();
        }
    }

    private static void onEntityJoin(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (Charges.isEmitter(entity)) {
            FieldWorlds.get(event.getLevel()).updateEmitter(entity);
        }
    }

    private static void onEntityLeave(EntityLeaveLevelEvent event) {
        FieldWorlds.get(event.getLevel()).removeEmitter(event.getEntity());
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, new AmbientPayload(AmbientFields.serverEntries()));
            syncPlayer(player);
        }
    }

    private static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncPlayer(player);
        }
    }

    private static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncPlayer(player);
        }
    }

    private static void syncPlayer(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, FieldWorlds.get(player.level()).snapshot());
        PacketDistributor.sendToPlayer(player, new EntityChargesPayload(player.getId(), Charges.induced(player)));
    }

    private static void onStartTracking(PlayerEvent.StartTracking event) {
        EntityCharges charges = Charges.induced(event.getTarget());
        if (!charges.isEmpty() && event.getEntity() instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, new EntityChargesPayload(event.getTarget().getId(), charges));
        }
    }

    private static void onDatapackSync(OnDatapackSyncEvent event) {
        AmbientPayload payload = new AmbientPayload(AmbientFields.serverEntries());
        event.getRelevantPlayers().forEach(player -> PacketDistributor.sendToPlayer(player, payload));
    }

    private static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new AmbientFields.Loader());
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        FieldCommand.register(event.getDispatcher());
    }

    private static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof Level level) {
            FieldWorlds.unload(level);
        }
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        FieldWorlds.clearServer();
    }
}
