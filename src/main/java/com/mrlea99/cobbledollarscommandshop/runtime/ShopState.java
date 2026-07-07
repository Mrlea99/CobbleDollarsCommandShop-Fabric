package com.mrlea99.cobbledollarscommandshop.runtime;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ShopState {
    public double globalBank = 0.0;
    public Map<String, ShopRecord> shops = new HashMap<>();

    public static class ShopRecord {
        public String id;
        public String owner;
        public String visibility = "public";
        public double price;
        public int stock;
        public double bank;
        public Set<String> allowedPlayers = new HashSet<>();
        public Map<String, Integer> playerStock = new HashMap<>();
    }
}
