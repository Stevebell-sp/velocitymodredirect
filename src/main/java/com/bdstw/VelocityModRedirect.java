package com.bdstw.velocitymodredirect;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.util.ModInfo;
import org.slf4j.Logger;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.loader.ConfigurationLoader;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Optional;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Plugin(id = "velocitymodredirect", name = "VelocityModRedirect", version = "1.0", authors = {"小誠"})
public class VelocityModRedirect {
    private final ProxyServer server;
    private final Logger logger;
    private ConfigurationNode config;   
            
    @Inject
    public VelocityModRedirect(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
        this.config = loadConfig();
        logger.info("BDSTW 模組包轉向器已啟動!");
    }

    @ConfigSerializable
    public static class RedirectConfig {
        @Setting(value = "default-server")
        private String defaultServer = "vanilla_server";
    
        @Setting(value = "redirects")
        private Map<String, String> redirects = new HashMap<>();

        public String getDefaultServer() {
            return defaultServer;
        }

        public Map<String, String> getRedirects() {
            return redirects;
        }
    }

    private ConfigurationNode loadConfig() {
        Path configPath = Paths.get("plugins/VelocityModRedirect/config.yml");  // 確保擴展名.yml對應資源文件
        try {
            if (!Files.exists(configPath)) {
                Files.createDirectories(configPath.getParent());
                try (InputStream in = getClass().getResourceAsStream("/config.yml")) {
                    if (in == null) {
                        logger.error("資源文件 config.yml 不存在!");
                        return null;
                    }
                    Files.copy(in, configPath);
                    logger.info("配置文件已創建：{}", configPath);
                }
            }

            // 正確地生成 YamlConfigurationLoader 並加載
            YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
            .path(configPath) 
            .build();

            return loader.load();
        } catch (IOException e) {
            logger.error("無法加載配置文件", e);
            return null;
        }
    }

    @Subscribe
    public void onPlayerLogin(LoginEvent event) {
        Player player = event.getPlayer();
        Optional<ModInfo> modInfo = player.getModInfo();
        //logger.info(modInfo.get().getMods());

        Set<String> modList = modInfo.map(info ->
                info.getMods().stream()
                    .map(ModInfo.Mod::getId)
                    .collect(Collectors.toSet())
        ).orElse(Set.of());

        logger.info("玩家 {} 已加入，檢測到的模組: {}", player.getUsername(), modInfo.get().getMods());

        String targetServer = getTargetServer(modList);

        server.getScheduler().buildTask(this, () -> {
            Optional<Player> playerOpt = server.getPlayer(player.getUniqueId());
            playerOpt.ifPresent(currentPlayer -> {
                Optional<RegisteredServer> serverOpt = server.getServer(targetServer);
                serverOpt.ifPresentOrElse(
                    regServer -> currentPlayer.createConnectionRequest(regServer).fireAndForget(),
                    () -> logger.warn("目標伺服器 {} 不存在，無法轉向玩家 {}!", targetServer, currentPlayer.getUsername())
                );
            });
        }).delay(1, TimeUnit.SECONDS).schedule();
    }
    
    private String getTargetServer(Set<String> modList) {
        try {
            RedirectConfig redirectConfig = config.get(RedirectConfig.class);

            for (String mod : modList) {
                String target = redirectConfig.getRedirects().get(mod);
                if (target != null) {
                    return target;
                }
            }
            return redirectConfig.getDefaultServer();
        } catch (org.spongepowered.configurate.serialize.SerializationException e) {
            logger.error("設定檔序列化錯誤", e);
            // 這裡可以選擇一個備用伺服器，或者其他錯誤處理方式
            return "lobby"; // 例如，如果出錯就導向 lobby
        }
    }

    @Subscribe
    public void onPlayerChooseInitialServer(PlayerChooseInitialServerEvent event) {
        Player player = event.getPlayer();
        logger.info("玩家 {} 正在選擇初始伺服器。", player.getUsername());

        String targetServerName = getTargetServer(player.getModInfo().map(ModInfo::getMods).map(mods ->
                mods.stream().map(ModInfo.Mod::getId).collect(Collectors.toSet())).orElse(Set.of()));

        Optional<RegisteredServer> targetServer = server.getServer(targetServerName);
        if (targetServer.isPresent()) {
            event.setInitialServer(targetServer.get());
            logger.info("將玩家 {} 重定向到 {}。", player.getUsername(), targetServerName);
        } else {
            logger.warn("目標伺服器 {} 不存在!", targetServerName);
        }
    }
}
