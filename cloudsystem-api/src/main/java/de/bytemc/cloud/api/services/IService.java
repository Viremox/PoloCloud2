package de.bytemc.cloud.api.services;

import de.bytemc.cloud.api.CloudAPI;
import de.bytemc.cloud.api.groups.IServiceGroup;
import de.bytemc.cloud.api.services.utils.ServiceState;
import de.bytemc.cloud.api.services.utils.ServiceVisibility;
import de.bytemc.network.packets.IPacket;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public interface IService {

    /**
     * @return the name of the service
     */
    @NotNull String getName();

    /**
     * @return the service id
     */
    int getServiceID();

    /**
     * @return the port of the service
     */
    int getPort();

    /**
     * @return the host name of the service
     */
    @NotNull String getHostName();

    /**
     * @return the group of the service
     */
    @NotNull IServiceGroup getServiceGroup();

    /**
     * sets the service state
     *
     * @param serviceState the state to set
     */
    void setServiceState(@NotNull ServiceState serviceState);

    /**
     * @return the state of the service
     */
    @NotNull ServiceState getServiceState();

    /**
     * @return the max players of the service
     */
    int getMaxPlayers();

    /**
     * sets the max players of the service
     * @param slots the amount to set
     */
    void setMaxPlayers(int slots);

    /**
     * @return the service visibility of the service
     */
    @NotNull ServiceVisibility getServiceVisibility();

    /**
     * sets the service visibility
     * @param serviceVisibility the service visibility to set
     */
    void setServiceVisibility(@NotNull ServiceVisibility serviceVisibility);

    /**
     * @return the online amount of the service
     */
    default int getOnlinePlayers() {
        return (int) CloudAPI.getInstance().getCloudPlayerManager().getAllCachedCloudPlayers()
            .stream()
            .filter(it -> {
                IService service = getServiceGroup().getGameServerVersion().isProxy() ? it.getProxyServer() : it.getServer();
                return service != null && service.equals(this);
            }).count();
    }

    /**
     * @return if the service is full
     */
    default boolean isFull() {
        return this.getOnlinePlayers() >= this.getMaxPlayers();
    }

    /**
     * edits the properties of the service and update then
     * @param serviceConsumer the consumer to change the properties
     */
    void edit(@NotNull Consumer<IService> serviceConsumer);

    String getMotd();

    void setMotd(String motd);

    void sendPacket(IPacket packet);

    /**
     * updates the properties of the service
     */
    void update();

}
