package com.mrlea99.cobbledollarscommandshop.runtime;

import com.google.gson.Gson;
import com.mrlea99.cobbledollarscommandshop.config.ModConfig;

import java.util.Locale;
import java.util.Map;

public class ShopService {
    private final Gson gson;
    private ModConfig config;
    private ShopState state;

    public ShopService(Gson gson, ModConfig config, ShopState state) {
        this.gson = gson;
        this.config = config;
        this.state = state;
        applyConfigDefaults();
    }

    public synchronized void replaceConfig(ModConfig config) {
        this.config = config;
        applyConfigDefaults();
    }

    public synchronized void replaceState(ShopState state) {
        this.state = state;
        applyConfigDefaults();
    }

    private void applyConfigDefaults() {
        if (state.globalBank == 0.0) {
            state.globalBank = config.startingGlobalBank;
        }

        for (Map.Entry<String, ModConfig.ShopTemplate> entry : config.predefinedShops.entrySet()) {
            String id = entry.getKey().toLowerCase(Locale.ROOT);
            if (state.shops.containsKey(id)) {
                continue;
            }

            ModConfig.ShopTemplate template = entry.getValue();
            ShopState.ShopRecord shop = new ShopState.ShopRecord();
            shop.id = id;
            shop.owner = "server";
            shop.price = template.price;
            shop.stock = template.stock;
            shop.bank = template.bank;
            shop.visibility = template.visibility == null ? "public" : template.visibility.toLowerCase(Locale.ROOT);
            state.shops.put(id, shop);
        }
    }

    public synchronized boolean createShop(String id, String owner) {
        String normalized = id.toLowerCase(Locale.ROOT);
        if (state.shops.containsKey(normalized)) {
            return false;
        }
        ShopState.ShopRecord record = new ShopState.ShopRecord();
        record.id = normalized;
        record.owner = owner;
        record.price = 100.0;
        record.stock = config.defaultStock;
        record.bank = config.defaultShopBank;
        state.shops.put(normalized, record);
        return true;
    }

    public synchronized ShopState.ShopRecord getShop(String id) {
        return state.shops.get(id.toLowerCase(Locale.ROOT));
    }

    public synchronized void setPrice(String id, double value) {
        getShop(id).price = value;
    }

    public synchronized void setVisibility(String id, String visibility) {
        getShop(id).visibility = visibility.toLowerCase(Locale.ROOT);
    }

    public synchronized void allowPlayer(String id, String playerName) {
        getShop(id).allowedPlayers.add(playerName.toLowerCase(Locale.ROOT));
    }

    public synchronized String buy(String id, String playerName, int quantity, boolean isOperator) {
        ShopState.ShopRecord shop = getShop(id);
        if (shop == null) {
            return "Unknown shop '" + id + "'.";
        }

        if (!canAccess(shop, playerName, isOperator)) {
            return "You do not have access to this shop.";
        }

        if (quantity <= 0) {
            return "Quantity must be greater than zero.";
        }

        if (shop.stock < quantity) {
            return "Not enough stock in shop.";
        }

        int currentPlayerStock = shop.playerStock.getOrDefault(playerName.toLowerCase(Locale.ROOT), 0);
        if (currentPlayerStock + quantity > config.perPlayerStockLimit) {
            return "Per-player stock limit reached.";
        }

        double total = shop.price * quantity;

        if (config.useGlobalBank && state.globalBank < total) {
            return "Global bank has insufficient funds.";
        }

        if (config.usePerShopBank && shop.bank < total) {
            return "Shop bank has insufficient funds.";
        }

        if (config.useGlobalBank) {
            state.globalBank -= total;
        }
        if (config.usePerShopBank) {
            shop.bank -= total;
        }

        shop.stock -= quantity;
        shop.playerStock.put(playerName.toLowerCase(Locale.ROOT), currentPlayerStock + quantity);
        return "Purchased " + quantity + " from '" + id + "' for " + total + ".";
    }

    public synchronized boolean canAccess(ShopState.ShopRecord shop, String playerName, boolean isOperator) {
        if (isOperator) {
            return true;
        }
        if (!"private".equalsIgnoreCase(shop.visibility)) {
            return true;
        }

        String normalized = playerName.toLowerCase(Locale.ROOT);
        String owner = shop.owner == null ? "" : shop.owner.toLowerCase(Locale.ROOT);
        return normalized.equals(owner) || shop.allowedPlayers.contains(normalized);
    }

    public synchronized double getGlobalBankBalance() {
        return state.globalBank;
    }

    public synchronized double getShopBankBalance(String id) {
        ShopState.ShopRecord shop = getShop(id);
        return shop == null ? Double.NaN : shop.bank;
    }

    public synchronized void depositGlobal(double amount) {
        state.globalBank += amount;
    }

    public synchronized void withdrawGlobal(double amount) {
        state.globalBank -= amount;
    }

    public synchronized void depositShop(String id, double amount) {
        getShop(id).bank += amount;
    }

    public synchronized void withdrawShop(String id, double amount) {
        getShop(id).bank -= amount;
    }

    public synchronized String stock(String id) {
        ShopState.ShopRecord shop = getShop(id);
        return shop == null ? "Unknown shop '" + id + "'." : "Shop '" + id + "' stock: " + shop.stock;
    }

    public synchronized String stateAsJson() {
        return gson.toJson(state);
    }

    public synchronized ShopState getState() {
        return state;
    }
}
