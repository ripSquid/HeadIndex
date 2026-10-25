package us.potatoboy.headindex.gui;

import eu.pb4.sgui.api.ClickType;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SlotBasedGui;
import eu.pb4.sgui.api.gui.layered.Layer;
import eu.pb4.sgui.api.gui.layered.LayeredGui;
import us.potatoboy.headindex.HeadIndex;
import us.potatoboy.headindex.api.Head;
import us.potatoboy.headindex.config.HeadIndexConfig;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Prediction;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class PagedHeadsGui extends LayeredGui {
    public final List<Head> heads;
    public int page = 0;
    final SlotBasedGui parent;
    final Layer contentLayer;
    final Layer navigationLayer;
    private static final int TRADE_SLOT = 8;
    private static final int TRADE_STATUS_SLOT = 2;

    private final boolean tradeMode = HeadIndex.config.economyType == HeadIndexConfig.EconomyType.HEAD;
    private final SimpleContainer tradeIn = new SimpleContainer(1);
    private int shownTradeHeads = -1;

    private static final Style regular = Style.EMPTY.withItalic(false).withColor(ChatFormatting.WHITE);

    private static ItemStack backwardArrow() {
        return new Head(
                "8aa062dc-9852-42b1-ae37-b2f8a3121c0e",
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzEwODI5OGZmMmIyNjk1MWQ2ODNlNWFkZTQ2YTQyZTkwYzJmN2M3ZGQ0MWJhYTkwOGJjNTg1MmY4YzMyZTU4MyJ9fX0="
        ).createStack(Component.translatable("spectatorMenu.previous_page").setStyle(regular));
    }

    private static ItemStack forwardArrow() {
        return new Head(
                "8aa062dc-9852-42b1-ae37-b2f8a3121c0e",
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzg2MTg1YjFkNTE5YWRlNTg1ZjE4NGMzNGYzZjNlMjBiYjY0MWRlYjg3OWU4MTM3OGU0ZWFmMjA5Mjg3In19fQ=="
        ).createStack(Component.translatable("spectatorMenu.next_page").setStyle(regular));
    }

    public PagedHeadsGui(SlotBasedGui parent, List<Head> heads) {
        super(MenuType.GENERIC_9x6, parent.getPlayer(), false);

        this.heads = heads;
        this.parent = parent;

        this.contentLayer = new Layer(5, 9);
        updateContent();
        this.addLayer(contentLayer, 0, 0);

        Layer navigation = new Layer(1, 9);
        this.navigationLayer = navigation;
        navigation.setSlot(0, new GuiElementBuilder(Items.BARRIER)
                .setName(Component.translatable("text.headindex.back"))
                .setCallback((index, type, action, gui) -> parent.open())
        );
        if (tradeMode) {
            navigation.setSlot(TRADE_SLOT, new Slot(tradeIn, 0, 0, 0) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return HeadIndexConfig.isTradeableHead(stack);
                }
            });
        }
        updateNavigation();
        this.addLayer(navigationLayer, 0, 5);
    }

    @Override
    public void setTitle(Component title) {
        var layout = tradeMode ? GuiTitle.Layout.LARGE_TRADE : GuiTitle.Layout.LARGE;
        super.setTitle(GuiTitle.of(getPlayer(), layout, title));
    }

    @Override
    public void onTick() {
        super.onTick();

        if (tradeMode && shownTradeHeads != tradeIn.getItem(0).getCount()) {
            updateTradeStatus();
        }
    }

    @Override
    public void onRemoved() {
        super.onRemoved();
        returnTradeIn();
    }

    /** Gives back whatever is left in the trade-in slot. Can run more than once, so it empties the slot first. */
    private void returnTradeIn() {
        var stack = tradeIn.removeItemNoUpdate(0);
        if (!stack.isEmpty()) {
            getPlayer().getInventory().placeItemBackInInventory(stack, Prediction.SERVER_ONLY);
        }
    }

    private void updateTradeStatus() {
        var traded = tradeIn.getItem(0).getCount();
        this.shownTradeHeads = traded;

        navigationLayer.setSlot(TRADE_STATUS_SLOT, new GuiElementBuilder(Items.PLAYER_HEAD)
                .setName(Component.translatable("text.headindex.trade.status", traded, HeadIndex.config.costAmount).setStyle(regular))
                .addLoreLine(Component.translatable("text.headindex.trade.hint").setStyle(Style.EMPTY.withItalic(false).withColor(ChatFormatting.GRAY)))
        );
    }

    /** Pays for a purchase. With head trade-in the heads come out of the trade-in slot instead of the inventory. */
    private void purchase(int amount, Runnable onPurchase) {
        if (!tradeMode) {
            HeadIndex.tryPurchase(getPlayer(), amount, onPurchase);
            return;
        }

        var needed = amount * HeadIndex.config.costAmount;
        var offered = tradeIn.getItem(0);
        if (offered.getCount() < needed) {
            getPlayer().sendSystemMessage(Component.translatable("text.headindex.trade.missing", needed).withStyle(ChatFormatting.RED));
            getPlayer().playSound(SoundEvents.VILLAGER_NO, 1f, 1f);
            return;
        }

        offered.shrink(needed);
        tradeIn.setChanged();
        onPurchase.run();
    }

    private int getMaxPage() {
        return Math.max(1, (int) Math.ceil((double) this.heads.size() / 45));
    }

    private void updateNavigation() {
        navigationLayer.setSlot(
                3, GuiElementBuilder
                        .from(this.page != 0 ? backwardArrow() : Items.STAINED_GLASS_PANE.black().getDefaultInstance())
                        .setName(Component.translatable("spectatorMenu.previous_page").setStyle(regular))
                        .setCallback((index, type, action, gui) -> {
                            this.page -= 1;
                            if (this.page < 0) {
                                this.page = 0;
                            }

                            updatePage();
                        })
        );

        navigationLayer.setSlot(
                5, GuiElementBuilder
                        .from(this.page + 1 < getMaxPage() ? forwardArrow() : Items.STAINED_GLASS_PANE.black().getDefaultInstance())
                        .setName(Component.translatable("spectatorMenu.next_page").setStyle(regular))
                        .setCallback((index, type, action, gui) -> {
                            this.page += 1;
                            if (this.page >= getMaxPage()) {
                                this.page = getMaxPage() - 1;
                            }

                            updatePage();
                        })
        );

        navigationLayer.setSlot(4, new GuiElementBuilder(Items.PAPER)
                .setName(Component.literal(this.page + 1 + " / " + this.getMaxPage()).setStyle(regular))
        );

        if (tradeMode) {
            updateTradeStatus();
        }
    }

    private void updateContent() {
        for (int i = 0; i < 45; i++) {
            if (heads.size() > i + (this.page * 45)) {
                Head head = heads.get(i + (this.page * 45));
				var builder = GuiElementBuilder.from(head.createStack());
				if (HeadIndex.config.economyType != HeadIndexConfig.EconomyType.FREE) {
                    builder.addLoreLine(Component.empty());
					builder.addLoreLine(Component.translatable("text.headindex.price", HeadIndex.config.getCost(getPlayer().level().getServer())).withStyle(style -> style.withColor(ChatFormatting.RED)));
				}

                contentLayer.setSlot(i, builder.asStack(), (index, type, action, g) -> processHeadClick(head, type));
            } else {
                contentLayer.setSlot(i, Items.AIR.getDefaultInstance());
            }
        }
    }

    public void updatePage() {
        updateNavigation();
        updateContent();
    }

    private void processHeadClick(Head head, ClickType type) {
        processHeadPurchase(head, type);
    }

    private void processHeadPurchase(Head head, ClickType type) {
        var player = getPlayer();

        ItemStack cursorStack = getPlayer().containerMenu.getCarried();
        ItemStack headStack = head.createStack();

        if (cursorStack.isEmpty()) {
            if (type.shift) {
                purchase(1, () -> player.getInventory().add(headStack));
            } else if (type.isMiddle) {
				purchase(headStack.getMaxStackSize(), () -> {
					headStack.setCount(headStack.getMaxStackSize());
					player.containerMenu.setCarried(headStack);
				});
            } else {
				purchase(1, () -> player.containerMenu.setCarried(headStack));
            }
        } else if (cursorStack.getMaxStackSize() <= cursorStack.getCount()) {
			return;
		} else if (ItemStack.isSameItem(headStack, cursorStack)) {
            if (type.isLeft) {
				purchase(1, () -> cursorStack.grow(1));
            } else if (type.isRight) {
				if (HeadIndex.config.economyType == HeadIndexConfig.EconomyType.FREE) cursorStack.shrink(1);
            } else if (type.isMiddle) {
				var amount = headStack.getMaxStackSize() - cursorStack.getCount();
				purchase(amount, () -> {
					headStack.setCount(headStack.getMaxStackSize());
					player.containerMenu.setCarried(headStack);
				});
            }
        } else {
			if (HeadIndex.config.economyType == HeadIndexConfig.EconomyType.FREE) player.containerMenu.setCarried(ItemStack.EMPTY);
        }
    }

//    @Override
//    public void onPlayerClose(boolean success) {
//        parent.open();
//    }
}
