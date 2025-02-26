// ModListListener.java
package com.bdstw.velocitymodredirect;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import java.util.concurrent.CompletableFuture;

public class ModListListener {

    private final ProxyServer server;
    private final Logger logger;

    public static final MinecraftChannelIdentifier MOD_LIST_CHANNEL = MinecraftChannelIdentifier.create("modlist", "get");

    public ModListListener(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
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
        // 假設客戶端回傳的資料是一個逗號分隔的字串
        String modListString = new String(event.getData());

        logger.info("{}'s mod list: {}", player.getUsername(), modListString);
        // 可以在這裡將 modListString 儲存到資料庫或其他地方
    }

     @Subscribe
    public void onPreLogin(PreLoginEvent event) {
        if (event != null && event.getUsername() != null) {
            logger.info(event.getUsername());
            CompletableFuture.runAsync(() -> {
            // 延遲一下, 等客戶端連接完成, 確保通道註冊成功
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //發送空的Plugin Message去觸發客戶端回傳
                event.getPlayer().sendPluginMessage(MOD_LIST_CHANNEL, new byte[0]);
            });
        } else {
            logger.warn("PreLoginEvent or username is null!");
        }
    }
}
