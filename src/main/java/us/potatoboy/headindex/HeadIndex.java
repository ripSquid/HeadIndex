package us.potatoboy.headindex;

import eu.pb4.common.economy.api.CommonEconomy;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.PlayerInventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import us.potatoboy.headindex.api.Category;
import us.potatoboy.headindex.api.Head;
import us.potatoboy.headindex.api.HeadDatabaseAPI;
import us.potatoboy.headindex.commands.HeadCommand;
import us.potatoboy.headindex.config.HeadIndexConfig;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public class HeadIndex implements ModInitializer {
    public static final String MOD_ID = "headindex";
    public static final Logger LOGGER = LogManager.getLogger();
    public static HeadIndexConfig config;
    public static final HeadDatabaseAPI HEAD_DATABASE = new HeadDatabaseAPI();
    public static Map<Category, List<Head>> heads = new HashMap<>();

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> new HeadCommand(dispatcher));
        
        config = HeadIndexConfig.loadConfig(new File(FabricLoader.getInstance().getConfigDir() + "/head-index.json"));
        
        PolymerResourcePackUtils.addModAssets(MOD_ID);

        CompletableFuture.runAsync(() -> heads = HEAD_DATABASE.getHeads());
    }

    public static void tryPurchase(ServerPlayer player, int amount, Runnable onPurchase) {
        var trueAmount = amount * HeadIndex.config.costAmount;

        switch (HeadIndex.config.economyType) {
            case FREE -> onPurchase.run();
            case TAG -> extractAndRun(player, trueAmount, stack -> stack.is(HeadIndex.config.getCostTag()), onPurchase);
            case HEAD -> extractAndRun(player, trueAmount, HeadIndexConfig::isTradeableHead, onPurchase);
            case ITEM -> {
                try (Transaction transaction = Transaction.openOuter()) {
                    long extracted = PlayerInventoryStorage.of(player).extract(ItemVariant.of(HeadIndex.config.getCostItem()), trueAmount, transaction);
                    if (extracted == trueAmount) {
                        transaction.commit();
                        onPurchase.run();
                    }
                }
            }
            case ECONOMY -> {
                var account = CommonEconomy.getAccounts(player, HeadIndex.config.getCostCurrency(player.level().getServer())).stream().min(Comparator.comparing(x -> x.balance().negate())).orElse(null);

                if (account != null) {
                    var transaction = account.decreaseBalance(trueAmount);
                    if (transaction.isSuccessful()) {
                        onPurchase.run();
                    }
                }
            }
            case LEVEL -> {
                // Cost in experience levels
                if (player.experienceLevel >= trueAmount) {
                    player.giveExperienceLevels(-trueAmount);
                    onPurchase.run();
                }
            }
            case LEVELPOINTS -> {
                // Cost in raw experience points
                if (player.totalExperience >= trueAmount) {
                    player.giveExperiencePoints(-trueAmount);
                    onPurchase.run();
                }
            }
        }
    }
    
    /** Counts the heads in the player's inventory that can be traded in when economyType is HEAD */
    public static int countTradeableHeads(ServerPlayer player) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack slotStack = player.getInventory().getItem(i);
            if (HeadIndexConfig.isTradeableHead(slotStack)) {
                count += slotStack.getCount();
            }
        }
        return count;
    }

    private static void extractAndRun(ServerPlayer player, int amount, Predicate<ItemStack> matches, Runnable onPurchase) {
        var variants = new HashSet<ItemVariant>();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack slotStack = player.getInventory().getItem(i);
            if (matches.test(slotStack)) {
                variants.add(ItemVariant.of(slotStack));
            }
        }
        try (Transaction transaction = Transaction.openOuter()) {
            long extracted = 0;
            for (ItemVariant item : variants) {
                extracted += PlayerInventoryStorage.of(player).extract(item, amount - extracted, transaction);
                if (extracted >= amount) {
                    break;
                }
            }
            if (extracted == amount) {
                transaction.commit();
                onPurchase.run();
            }
        }
    }

    public static Component licenseWarn() {
        try {
            return Component.translatable("text.headindex.config.license",  Component.literal("https://minecraft-heads.com/wiki/minecraft-heads/api-v2-for-users").setStyle(
                    Style.EMPTY.withColor(ChatFormatting.BLUE).withClickEvent(new ClickEvent.OpenUrl(new URI("https://minecraft-heads.com/wiki/minecraft-heads/api-v2-for-users")))
            ));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(HeadIndex.MOD_ID, path);
    }
}
