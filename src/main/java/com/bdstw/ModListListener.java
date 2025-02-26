package com.bdstw.velocitymodredirect;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.PlayerChannelRegisterEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import org.slf4j.Logger;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class ModListListener {

    private final ProxyServer server;
    private final Logger logger;

    public static final MinecraftChannelIdentifier MOD_LIST_CHANNEL = MinecraftChannelIdentifier.create("modlist", "get");

    public ModListListener(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
        // 可以在建構子中註冊通道
        //server.getChannelRegistrar().register(MOD_LIST_CHANNEL);
    }

    // Plugin Message 處理
    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().equals(MOD_LIST_CHANNEL)) {
            return;
        }
        if (!(event.getSource() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getSource();

        try {
            byte[] data = event.getData();
            if (data == null) {
                logger.warn("Received null data from {} on channel {}", player.getUsername(), MOD_LIST_CHANNEL.getId());
                return;
            }
            String modListString = new String(data, StandardCharsets.UTF_8);
            logger.info("{}'s mod list: {}", player.getUsername(), modListString);
            // 可以在這裡將 modListString 儲存到資料庫或其他地方

        } catch (Exception e) {
            logger.error("Error processing plugin message from {}: ", player.getUsername(), e);
        }
    }
    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        Player player = event.getPlayer();
        CompletableFuture.runAsync(()->{
            try{
                player.sendPluginMessage(MOD_LIST_CHANNEL, new byte[0]);
            }
            catch(Exception e){
                logger.error("Send message fail: ", e);
            }
        });
    }

     @Subscribe
    public void onPreLogin(PreLoginEvent event) {
        if (event != null && event.getUsername() != null) {
            logger.info(event.getUsername());
        } else {
            logger.warn("PreLoginEvent or username is null!");
        }
    }
    //Velocity 關閉時會觸發
    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event){
        server.getChannelRegistrar().unregister(MOD_LIST_CHANNEL);
    }
}
