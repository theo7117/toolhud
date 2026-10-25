package fr.toolhud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;

/** Dessine le HUD de l'objet tenu en main (durabilité + lore). Utilisé par le HUD en jeu et par l'écran de réglages. */
public final class ToolHudRenderer {
    private static final int PAD = 4;
    private static final int ICON = 16;
    private static final int GAP = 4;
    private static final int LINE = 10;

    private ToolHudRenderer() {}

    /** Lignes à afficher : durabilité d'abord, puis le lore de l'objet. */
    private static List<Component> lines(ItemStack stack, ToolHudConfig cfg) {
        List<Component> lines = new ArrayList<>();
        if (cfg.showDurability && stack.isDamageableItem()) {
            int left = stack.getMaxDamage() - stack.getDamageValue();
            lines.add(Component.literal(left + "/" + stack.getMaxDamage())
                    .withColor(0xFF000000 | stack.getBarColor()));
        }
        if (cfg.showLore) {
            ItemLore lore = stack.get(DataComponents.LORE);
            if (lore != null) {
                lines.addAll(lore.lines());
            }
        }
        return lines;
    }

    /** Taille du HUD (avant mise à l'échelle) : {largeur, hauteur}. */
    public static int[] measure(Font font, ItemStack stack, ToolHudConfig cfg) {
        List<Component> lines = lines(stack, cfg);
        int textW = 0;
        for (Component line : lines) {
            textW = Math.max(textW, font.width(line));
        }
        int w = PAD + ICON + (textW > 0 ? GAP + textW : 0) + PAD;
        int textH = lines.isEmpty() ? 0 : lines.size() * LINE - 1;
        int h = PAD * 2 + Math.max(ICON, textH);
        return new int[] {w, h};
    }

    /** Dessine le HUD à la position et à l'échelle de la config. */
    public static void render(GuiGraphicsExtractor graphics, Font font, ItemStack stack, ToolHudConfig cfg) {
        int[] size = measure(font, stack, cfg);
        int w = size[0];
        int h = size[1];
        List<Component> lines = lines(stack, cfg);

        graphics.pose().pushMatrix();
        graphics.pose().translate((float) cfg.x, (float) cfg.y);
        graphics.pose().scale(cfg.scale, cfg.scale);

        if (cfg.showBackground) {
            graphics.fill(0, 0, w, h, 0x90000000);
        }

        graphics.item(stack, PAD, (h - ICON) / 2);

        int textX = PAD + ICON + GAP;
        int textY = (h - (lines.size() * LINE - 1)) / 2;
        for (Component line : lines) {
            graphics.text(font, line, textX, textY, 0xFFFFFFFF, true);
            textY += LINE;
        }

        // Barre de durabilité en bas du HUD
        if (cfg.showDurability && stack.isBarVisible()) {
            int barW = Math.round((w - 2 * PAD) * (stack.getBarWidth() / 13.0f));
            graphics.fill(PAD, h - 3, w - PAD, h - 2, 0xFF000000);
            graphics.fill(PAD, h - 3, PAD + barW, h - 2, 0xFF000000 | stack.getBarColor());
        }

        graphics.pose().popMatrix();
    }

    /** Rectangle du HUD à l'écran (après mise à l'échelle) : {x, y, largeur, hauteur}. */
    public static int[] bounds(Font font, ItemStack stack, ToolHudConfig cfg) {
        int[] size = measure(font, stack, cfg);
        return new int[] {cfg.x, cfg.y, Math.round(size[0] * cfg.scale), Math.round(size[1] * cfg.scale)};
    }

    public static void clampToScreen(Minecraft mc, Font font, ItemStack stack, ToolHudConfig cfg) {
        int[] b = bounds(font, stack, cfg);
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        cfg.x = Math.max(0, Math.min(sw - b[2], cfg.x));
        cfg.y = Math.max(0, Math.min(sh - b[3], cfg.y));
    }
}
