package com.bdstw.velocitymodredirect;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

@Plugin(id = "velocitymodredirect", name = "VelocityModRedirect", version = "1.0-SNAPSHOT",
        url = "https://github.com/Stevebell-sp/velocitymodredirect", authors = {"Stevebell-sp"})
public class ModList {

    private final ProxyServer server;
    private final Logger logger;

    @Inject
    public ModList(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        // 註冊 ModListListener 作為事件監聽器
        server.getEventManager().register(this, new ModListListener(server, logger));
        server.getChannelRegistrar().register(ModListListener.MOD_LIST_CHANNEL);
    }
}
