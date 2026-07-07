package com.mrlea99.cobbledollarscommandshop.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mrlea99.cobbledollarscommandshop.CobbleDollarsCommandShopFabric;
import com.mrlea99.cobbledollarscommandshop.runtime.ShopService;
import com.mrlea99.cobbledollarscommandshop.runtime.ShopState;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class ShopCommands {
    private final CobbleDollarsCommandShopFabric mod;

    public ShopCommands(CobbleDollarsCommandShopFabric mod) {
        this.mod = mod;
    }

    public void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("cdshop")
                        .then(CommandManager.literal("reload")
                                .requires(source -> source.hasPermissionLevel(2))
                                .executes(context -> {
                                    mod.reload();
                                    context.getSource().sendFeedback(() -> Text.literal("CobbleDollars config reloaded."), false);
                                    return 1;
                                }))
                        .then(CommandManager.literal("bank")
                                .then(CommandManager.literal("balance")
                                        .executes(context -> {
                                            context.getSource().sendFeedback(() -> Text.literal(
                                                    "Global bank balance: " + mod.service().getGlobalBankBalance()), false);
                                            return 1;
                                        })
                                        .then(CommandManager.argument("shopId", StringArgumentType.word())
                                                .executes(context -> {
                                                    String shopId = StringArgumentType.getString(context, "shopId");
                                                    double balance = mod.service().getShopBankBalance(shopId);
                                                    if (Double.isNaN(balance)) {
                                                        context.getSource().sendError(Text.literal("Unknown shop."));
                                                        return 0;
                                                    }
                                                    context.getSource().sendFeedback(() -> Text.literal(
                                                            "Shop bank balance: " + balance), false);
                                                    return 1;
                                                })))
                                .then(CommandManager.literal("deposit")
                                        .requires(source -> source.hasPermissionLevel(2))
                                        .then(CommandManager.argument("amount", DoubleArgumentType.doubleArg(0.0))
                                                .executes(context -> {
                                                    double amount = DoubleArgumentType.getDouble(context, "amount");
                                                    mod.service().depositGlobal(amount);
                                                    mod.saveState();
                                                    context.getSource().sendFeedback(() -> Text.literal("Deposited into global bank."), false);
                                                    return 1;
                                                })
                                                .then(CommandManager.argument("shopId", StringArgumentType.word())
                                                        .executes(context -> {
                                                            String shopId = StringArgumentType.getString(context, "shopId");
                                                            double amount = DoubleArgumentType.getDouble(context, "amount");
                                                            if (mod.service().getShop(shopId) == null) {
                                                                context.getSource().sendError(Text.literal("Unknown shop."));
                                                                return 0;
                                                            }
                                                            mod.service().depositShop(shopId, amount);
                                                            mod.saveState();
                                                            context.getSource().sendFeedback(() -> Text.literal("Deposited into shop bank."), false);
                                                            return 1;
                                                        }))))
                                .then(CommandManager.literal("withdraw")
                                        .requires(source -> source.hasPermissionLevel(2))
                                        .then(CommandManager.argument("amount", DoubleArgumentType.doubleArg(0.0))
                                                .executes(context -> {
                                                    double amount = DoubleArgumentType.getDouble(context, "amount");
                                                    mod.service().withdrawGlobal(amount);
                                                    mod.saveState();
                                                    context.getSource().sendFeedback(() -> Text.literal("Withdrew from global bank."), false);
                                                    return 1;
                                                })
                                                .then(CommandManager.argument("shopId", StringArgumentType.word())
                                                        .executes(context -> {
                                                            String shopId = StringArgumentType.getString(context, "shopId");
                                                            double amount = DoubleArgumentType.getDouble(context, "amount");
                                                            if (mod.service().getShop(shopId) == null) {
                                                                context.getSource().sendError(Text.literal("Unknown shop."));
                                                                return 0;
                                                            }
                                                            mod.service().withdrawShop(shopId, amount);
                                                            mod.saveState();
                                                            context.getSource().sendFeedback(() -> Text.literal("Withdrew from shop bank."), false);
                                                            return 1;
                                                        }))))
                        )
                        .then(CommandManager.literal("shop")
                                .then(CommandManager.literal("create")
                                        .requires(source -> source.hasPermissionLevel(2))
                                        .then(CommandManager.argument("shopId", StringArgumentType.word())
                                                .executes(context -> {
                                                    String shopId = StringArgumentType.getString(context, "shopId");
                                                    String owner = "server";
                                                    if (context.getSource().getEntity() instanceof ServerPlayerEntity player) {
                                                        owner = player.getNameForScoreboard();
                                                    }
                                                    if (!mod.service().createShop(shopId, owner)) {
                                                        context.getSource().sendError(Text.literal("Shop already exists."));
                                                        return 0;
                                                    }
                                                    mod.saveState();
                                                    context.getSource().sendFeedback(() -> Text.literal("Shop created: " + shopId), false);
                                                    mod.audit(context.getSource().getName(), "shop_create", shopId);
                                                    return 1;
                                                })))
                                .then(CommandManager.literal("setprice")
                                        .requires(source -> source.hasPermissionLevel(2))
                                        .then(CommandManager.argument("shopId", StringArgumentType.word())
                                                .then(CommandManager.argument("price", DoubleArgumentType.doubleArg(0.0))
                                                        .executes(context -> {
                                                            String shopId = StringArgumentType.getString(context, "shopId");
                                                            if (mod.service().getShop(shopId) == null) {
                                                                context.getSource().sendError(Text.literal("Unknown shop."));
                                                                return 0;
                                                            }
                                                            double price = DoubleArgumentType.getDouble(context, "price");
                                                            mod.service().setPrice(shopId, price);
                                                            mod.saveState();
                                                            mod.audit(context.getSource().getName(), "shop_setprice", shopId + "=" + price);
                                                            context.getSource().sendFeedback(() -> Text.literal("Price updated."), false);
                                                            return 1;
                                                        }))))
                                .then(CommandManager.literal("visibility")
                                        .requires(source -> source.hasPermissionLevel(2))
                                        .then(CommandManager.argument("shopId", StringArgumentType.word())
                                                .then(CommandManager.argument("mode", StringArgumentType.word())
                                                        .executes(context -> {
                                                            String shopId = StringArgumentType.getString(context, "shopId");
                                                            if (mod.service().getShop(shopId) == null) {
                                                                context.getSource().sendError(Text.literal("Unknown shop."));
                                                                return 0;
                                                            }
                                                            String mode = StringArgumentType.getString(context, "mode").toLowerCase();
                                                            if (!mode.equals("public") && !mode.equals("private")) {
                                                                context.getSource().sendError(Text.literal("Mode must be public/private."));
                                                                return 0;
                                                            }
                                                            mod.service().setVisibility(shopId, mode);
                                                            mod.saveState();
                                                            mod.audit(context.getSource().getName(), "shop_visibility", shopId + "=" + mode);
                                                            context.getSource().sendFeedback(() -> Text.literal("Visibility updated."), false);
                                                            return 1;
                                                        }))))
                                .then(CommandManager.literal("allow")
                                        .requires(source -> source.hasPermissionLevel(2))
                                        .then(CommandManager.argument("shopId", StringArgumentType.word())
                                                .then(CommandManager.argument("player", StringArgumentType.word())
                                                        .executes(context -> {
                                                            String shopId = StringArgumentType.getString(context, "shopId");
                                                            if (mod.service().getShop(shopId) == null) {
                                                                context.getSource().sendError(Text.literal("Unknown shop."));
                                                                return 0;
                                                            }
                                                            String playerName = StringArgumentType.getString(context, "player");
                                                            mod.service().allowPlayer(shopId, playerName);
                                                            mod.saveState();
                                                            mod.audit(context.getSource().getName(), "shop_allow", shopId + "->" + playerName);
                                                            context.getSource().sendFeedback(() -> Text.literal("Player allowed for shop."), false);
                                                            return 1;
                                                        }))))
                                .then(CommandManager.literal("buy")
                                        .then(CommandManager.argument("shopId", StringArgumentType.word())
                                                .then(CommandManager.argument("quantity", IntegerArgumentType.integer(1))
                                                        .executes(context -> {
                                                            String shopId = StringArgumentType.getString(context, "shopId");
                                                            int quantity = IntegerArgumentType.getInteger(context, "quantity");
                                                            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();

                                                            ShopService service = mod.service();
                                                            String result = service.buy(
                                                                    shopId,
                                                                    player.getNameForScoreboard(),
                                                                    quantity,
                                                                    context.getSource().hasPermissionLevel(2)
                                                            );

                                                            if (result.startsWith("Purchased")) {
                                                                mod.saveState();
                                                                mod.audit(player.getNameForScoreboard(), "shop_buy", shopId + " x" + quantity);
                                                                context.getSource().sendFeedback(() -> Text.literal(result), false);
                                                                return 1;
                                                            }
                                                            context.getSource().sendError(Text.literal(result));
                                                            return 0;
                                                        }))))
                                .then(CommandManager.literal("stock")
                                        .then(CommandManager.argument("shopId", StringArgumentType.word())
                                                .executes(context -> {
                                                    String shopId = StringArgumentType.getString(context, "shopId");
                                                    ShopState.ShopRecord shop = mod.service().getShop(shopId);
                                                    if (shop == null) {
                                                        context.getSource().sendError(Text.literal("Unknown shop."));
                                                        return 0;
                                                    }
                                                    context.getSource().sendFeedback(() -> Text.literal(
                                                            "Shop '" + shopId + "' stock=" + shop.stock + ", price=" + shop.price), false);
                                                    return 1;
                                                })))
                        )
        );
    }
}
