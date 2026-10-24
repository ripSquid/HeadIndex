package us.potatoboy.headindex.gui;

import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import us.potatoboy.headindex.HeadIndex;

/**
 * Builds container titles that draw a textured background behind the slots.
 * <p>
 * The background is a bitmap glyph from {@code assets/headindex/font/gui.json}. Titles are drawn from the container's
 * top left corner (offset by 8 pixels), so the glyph is pulled back by that offset and the real title is moved back to
 * where vanilla would draw it. Players without the resource pack get the plain title instead of missing glyphs.
 */
public final class GuiTitle {
    private static final Style FONT = Style.EMPTY
            .withFont(new FontDescription.Resource(HeadIndex.id("gui")))
            .withColor(0xFFFFFF)
            .withItalic(false);

    private static final String SHIFT_TO_EDGE = "";
    private static final String SHIFT_TO_TITLE = "";

    private GuiTitle() {
    }

    public enum Layout {
        SMALL(""),
        LARGE("");

        private final String glyph;

        Layout(String glyph) {
            this.glyph = glyph;
        }
    }

    public static Component of(ServerPlayer player, Layout layout, Component title) {
        if (!PolymerResourcePackUtils.hasMainPack(player.connection)) {
            return title;
        }

        return Component.empty()
                .append(Component.literal(SHIFT_TO_EDGE + layout.glyph + SHIFT_TO_TITLE).setStyle(FONT))
                .append(title.copy().withStyle(style -> style.getColor() == null ? style.withColor(0xFFFFFF) : style));
    }
}
