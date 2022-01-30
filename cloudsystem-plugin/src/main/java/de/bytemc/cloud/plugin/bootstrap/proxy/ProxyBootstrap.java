package de.bytemc.cloud.plugin.bootstrap.proxy;

import de.bytemc.cloud.api.CloudAPI;
import de.bytemc.cloud.api.network.packets.services.ServiceAddPacket;
import de.bytemc.cloud.api.network.packets.services.ServiceCacheUpdatePacket;
import de.bytemc.cloud.api.network.packets.services.ServiceRemovePacket;
import de.bytemc.cloud.api.services.IService;
import de.bytemc.cloud.plugin.CloudPlugin;
import de.bytemc.cloud.plugin.IPlugin;
import de.bytemc.cloud.plugin.console.DefaultProxyCommandSender;
import de.bytemc.cloud.plugin.events.proxy.ProxyEvents;
import de.bytemc.network.NetworkManager;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

public class ProxyBootstrap extends Plugin implements IPlugin {

    @Override
    public void onLoad() {
        CloudPlugin.setCommandSender(new DefaultProxyCommandSender());

        NetworkManager.registerPacketListener(ServiceAddPacket.class, (ctx, packet) -> {
            if (!packet.getService().getServiceGroup().getGameServerVersion().isProxy())
                this.registerService(packet.getService());
        });
        NetworkManager.registerPacketListener(ServiceRemovePacket.class, (ctx, packet) ->
            this.unregisterService(CloudAPI.getInstance().getServiceManager().getServiceByNameOrNull(packet.getService())));

        new CloudPlugin(this);
    }

    @Override
    public void onEnable() {
        for (IService allCachedService : CloudAPI.getInstance().getServiceManager().getAllCachedServices()) {

            System.out.println("service save: " + allCachedService.getName());

            if (!allCachedService.getServiceGroup().getGameServerVersion().isProxy()) registerService(allCachedService);
        }
        System.out.println("chache update packet3123");

        NetworkManager.registerPacketListener(ServiceCacheUpdatePacket.class, (ctx, packet) -> {

            System.out.println("chache update packet");

            ProxyServer.getInstance().getServers().clear();

            for (IService allCachedService : packet.getAllCachedServices()) {

                System.out.println(allCachedService.getName());

                if (!allCachedService.getServiceGroup().getGameServerVersion().isProxy())
                    registerService(allCachedService);
            }
        });

        this.getProxy().getPluginManager().registerListener(this, new ProxyEvents());
    }

    @Override
    public void onDisable() {

    }

    @Override
    public void shutdown() {
        this.getProxy().getScheduler().schedule(this, this.getProxy()::stop, 0, TimeUnit.MILLISECONDS);
    }

    public void registerService(IService service) {
        ProxyServer.getInstance().getServers().put(service.getName(), ProxyServer.getInstance()
            .constructServerInfo(service.getName(), new InetSocketAddress(service.getHostName(), service.getPort()), "PoloCloud", false));
    }

    public void unregisterService(IService service) {
        ProxyServer.getInstance().getServers().remove(service.getName());
    }

}
