package de.bytemc.cloud.api.player.impl;

import de.bytemc.cloud.api.CloudAPI;
import de.bytemc.cloud.api.events.IEventHandler;
import de.bytemc.cloud.api.events.events.CloudPlayerDisconnectEvent;
import de.bytemc.cloud.api.events.events.CloudPlayerLoginEvent;
import de.bytemc.cloud.api.events.events.CloudPlayerUpdateEvent;
import de.bytemc.cloud.api.events.events.CloudServiceRemoveEvent;
import de.bytemc.cloud.api.network.INetworkHandler;
import de.bytemc.cloud.api.network.packets.player.CloudPlayerDisconnectPacket;
import de.bytemc.cloud.api.network.packets.player.CloudPlayerLoginPacket;
import de.bytemc.cloud.api.network.packets.player.CloudPlayerUpdatePacket;
import de.bytemc.cloud.api.player.ICloudPlayer;
import de.bytemc.cloud.api.player.ICloudPlayerManager;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractPlayerManager implements ICloudPlayerManager {

    protected Map<UUID, ICloudPlayer> cachedCloudPlayers = new ConcurrentHashMap<>();

    public AbstractPlayerManager() {

        final INetworkHandler networkHandler = CloudAPI.getInstance().getNetworkHandler();
        final IEventHandler eventHandler = CloudAPI.getInstance().getEventHandler();

        networkHandler.registerPacketListener(CloudPlayerUpdatePacket.class, (ctx, packet) -> {
            this.getCloudPlayer(packet.getUuid()).ifPresent(cloudPlayer -> {
                cloudPlayer.setProxyServer(packet.getProxyServer());
                cloudPlayer.setServer(packet.getServer());
                eventHandler.call(new CloudPlayerUpdateEvent(cloudPlayer));
            });
        });

        networkHandler.registerPacketListener(CloudPlayerLoginPacket.class, (ctx, packet) -> {
            final ICloudPlayer cloudPlayer = new SimpleCloudPlayer(packet.getUuid(), packet.getUsername());
            this.cachedCloudPlayers.put(packet.getUuid(), cloudPlayer);
            eventHandler.call(new CloudPlayerLoginEvent(cloudPlayer));
        });

        networkHandler.registerPacketListener(CloudPlayerDisconnectPacket.class, (ctx, packet) ->
            this.getCloudPlayer(packet.getUuid()).ifPresent(cloudPlayer -> {
                this.cachedCloudPlayers.remove(cloudPlayer.getUniqueId());
                eventHandler.call(new CloudPlayerDisconnectEvent(cloudPlayer));
            }));

        eventHandler.registerEvent(CloudServiceRemoveEvent.class, event ->
            this.cachedCloudPlayers.values().forEach(player -> {
                if (player.getProxyServer().getName().equals(event.getService()))
                    this.cachedCloudPlayers.remove(player.getUniqueId());
            })
        );

    }

    public void setCachedCloudPlayers(final Map<UUID, ICloudPlayer> cachedCloudPlayers) {
        this.cachedCloudPlayers = cachedCloudPlayers;
    }

    public abstract void registerCloudPlayer(@NotNull UUID uniqueID, @NotNull String username);

    public abstract void unregisterCloudPlayer(@NotNull UUID uuid, @NotNull String name);

    public abstract void updateCloudPlayer(@NotNull ICloudPlayer cloudPlayer);

    @Override
    public @NotNull List<ICloudPlayer> getAllCachedCloudPlayers() {
        return Arrays.asList(this.cachedCloudPlayers.values().toArray(new ICloudPlayer[0]));
    }

    @Override
    public @NotNull Optional<ICloudPlayer> getCloudPlayer(final @NotNull UUID uniqueId) {
        return Optional.ofNullable(this.cachedCloudPlayers.get(uniqueId));
    }

    @Override
    public @NotNull Optional<ICloudPlayer> getCloudPlayer(final @NotNull String username) {
        return this.cachedCloudPlayers.values().stream().filter(it -> it.getUsername().equalsIgnoreCase(username)).findAny();
    }

    @Override
    public ICloudPlayer getCloudPlayerByNameOrNull(@NotNull String username) {
        return this.getCloudPlayer(username).orElse(null);
    }

    @Override
    public ICloudPlayer getCloudPlayerByUniqueIdOrNull(@NotNull UUID uniqueId) {
        return this.getCloudPlayer(uniqueId).orElse(null);
    }

}
