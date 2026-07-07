package com.mrlea99.cobbledollarscommandshop.config;

import java.util.HashMap;
import java.util.Map;

public class ModConfig {
    public boolean useGlobalBank = true;
    public boolean usePerShopBank = true;
    public double startingGlobalBank = 10_000.0;
    public double defaultShopBank = 1_000.0;
    public int defaultStock = 64;
    public int perPlayerStockLimit = 16;
    public Map<String, ShopTemplate> predefinedShops = new HashMap<>();

    public ModConfig withDefaults() {
        if (predefinedShops.isEmpty()) {
            ShopTemplate template = new ShopTemplate();
            template.price = 100.0;
            template.stock = defaultStock;
            template.visibility = "public";
            template.bank = defaultShopBank;
            predefinedShops.put("example", template);
        }
        return this;
    }

    public static class ShopTemplate {
        public double price = 100.0;
        public int stock = 64;
        public String visibility = "public";
        public double bank = 1_000.0;
    }
}
