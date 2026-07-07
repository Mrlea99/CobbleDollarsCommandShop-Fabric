package com.mrlea99.cobbledollarscommandshop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mrlea99.cobbledollarscommandshop.command.ShopCommands;
import com.mrlea99.cobbledollarscommandshop.config.ModConfig;
import com.mrlea99.cobbledollarscommandshop.runtime.AuditLog;
import com.mrlea99.cobbledollarscommandshop.runtime.ShopService;
import com.mrlea99.cobbledollarscommandshop.runtime.ShopState;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class CobbleDollarsCommandShopFabric implements ModInitializer {
    public static final String MOD_ID = "cobbledollarscommandshop";
    public static final Identifier SHOP_STATE_PACKET_ID = Identifier.of(MOD_ID, "shop_state");

    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static CobbleDollarsCommandShopFabric INSTANCE;

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private Path configDirectory;
    private Path configFile;
    private Path stateFile;

    private ModConfig config;
    private ShopService shopService;
    private AuditLog auditLog;

    @Override
    public void onInitialize() {
        INSTANCE = this;

        ServerLifecycleEvents.SERVER_STARTED.register(server -> loadAll());
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> saveState());

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                new ShopCommands(this).register(dispatcher));

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sendShopState(handler.player));

        LOGGER.info("CobbleDollars Command Shop Fabric initialized");
    }

    public static CobbleDollarsCommandShopFabric getInstance() {
        return INSTANCE;
    }

    private void ensurePaths() {
        if (configDirectory != null) {
            return;
        }
        configDirectory = FabricLoader.getInstance().getConfigDir().resolve("cobbledollarscommandshops");
        configFile = configDirectory.resolve("config.json");
        stateFile = configDirectory.resolve("state.json");
        auditLog = new AuditLog(configDirectory.resolve("audit.log"));
    }

    private void loadAll() {
        ensurePaths();
        try {
            Files.createDirectories(configDirectory);

            if (!Files.exists(configFile)) {
                ModConfig defaults = new ModConfig().withDefaults();
                Files.writeString(configFile, gson.toJson(defaults), StandardCharsets.UTF_8);
            }

            config = gson.fromJson(Files.readString(configFile, StandardCharsets.UTF_8), ModConfig.class);
            if (config == null) {
                config = new ModConfig();
            }
            config.withDefaults();

            ShopState state;
            if (Files.exists(stateFile)) {
                state = gson.fromJson(Files.readString(stateFile, StandardCharsets.UTF_8), ShopState.class);
                if (state == null) {
                    state = new ShopState();
                }
            } else {
                state = new ShopState();
            }

            if (shopService == null) {
                shopService = new ShopService(gson, config, state);
            } else {
                shopService.replaceConfig(config);
                shopService.replaceState(state);
            }

            saveState();
        } catch (IOException exception) {
            LOGGER.error("Failed to load CobbleDollars config/state", exception);
            if (config == null) {
                config = new ModConfig().withDefaults();
            }
            if (shopService == null) {
                shopService = new ShopService(gson, config, new ShopState());
            }
        }
    }

    public void reload() {
        loadAll();
    }

    public void saveState() {
        ensurePaths();
        if (shopService == null) {
            return;
        }
        try {
            Files.createDirectories(configDirectory);
            Files.writeString(stateFile, gson.toJson(shopService.getState()), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            LOGGER.error("Failed to save CobbleDollars state", exception);
        }
    }

    public void audit(String actor, String action, String details) {
        if (auditLog != null) {
            auditLog.write(actor, action, details);
        }
    }

    public ShopService service() {
        if (shopService == null) {
            loadAll();
        }
        return shopService;
    }

    private void sendShopState(ServerPlayerEntity player) {
        if (shopService == null || !ServerPlayNetworking.canSend(player, SHOP_STATE_PACKET_ID)) {
            return;
        }

        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(shopService.stateAsJson());
        ServerPlayNetworking.send(player, SHOP_STATE_PACKET_ID, buf);
    }
}
