package fr.toolhud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Écran de réglages : on glisse le HUD avec la souris, molette ou boutons +/- pour la taille. */
public class ToolHudSettingsScreen extends Screen {
    private final ToolHudConfig cfg = ToolHudConfig.get();
    private final ItemStack previewStack = new ItemStack(Items.DIAMOND_PICKAXE);

    private boolean dragging = false;
    private int dragOffsetX;
    private int dragOffsetY;

    private Button enabledButton;
    private Button loreButton;
    private Button durabilityButton;
    private Button backgroundButton;
    private Button scaleButton;

    public ToolHudSettingsScreen() {
        super(Component.translatable("toolhud.settings.title"));
        previewStack.setDamageValue(300);
    }

    private ItemStack currentStack() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && !mc.player.getMainHandItem().isEmpty()) {
            return mc.player.getMainHandItem();
        }
        return previewStack;
    }

    private static Component onOff(String key, boolean value) {
        return Component.translatable(key).append(": ")
                .append(Component.translatable(value ? "toolhud.on" : "toolhud.off"));
    }

    private Component scaleLabel() {
        return Component.translatable("toolhud.settings.scale", String.format("%.1f", cfg.scale));
    }

    @Override
    protected void init() {
        int bw = 150;
        int bh = 20;
        int gap = 4;
        int leftX = this.width / 2 - bw - gap / 2;
        int rightX = this.width / 2 + gap / 2;
        int y0 = this.height - 3 * (bh + gap) - 8;

        enabledButton = addRenderableWidget(Button.builder(onOff("toolhud.settings.enabled", cfg.enabled), b -> {
            cfg.enabled = !cfg.enabled;
            b.setMessage(onOff("toolhud.settings.enabled", cfg.enabled));
        }).bounds(leftX, y0, bw, bh).build());

        loreButton = addRenderableWidget(Button.builder(onOff("toolhud.settings.lore", cfg.showLore), b -> {
            cfg.showLore = !cfg.showLore;
            b.setMessage(onOff("toolhud.settings.lore", cfg.showLore));
        }).bounds(rightX, y0, bw, bh).build());

        durabilityButton = addRenderableWidget(
                Button.builder(onOff("toolhud.settings.durability", cfg.showDurability), b -> {
                    cfg.showDurability = !cfg.showDurability;
                    b.setMessage(onOff("toolhud.settings.durability", cfg.showDurability));
                }).bounds(leftX, y0 + bh + gap, bw, bh).build());

        backgroundButton = addRenderableWidget(
                Button.builder(onOff("toolhud.settings.background", cfg.showBackground), b -> {
                    cfg.showBackground = !cfg.showBackground;
                    b.setMessage(onOff("toolhud.settings.background", cfg.showBackground));
                }).bounds(rightX, y0 + bh + gap, bw, bh).build());

        int smallW = 30;
        addRenderableWidget(Button.builder(Component.literal("-"), b -> changeScale(-0.1f))
                .bounds(leftX, y0 + 2 * (bh + gap), smallW, bh).build());
        scaleButton = addRenderableWidget(Button.builder(scaleLabel(), b -> {
            cfg.scale = 1.0f;
            b.setMessage(scaleLabel());
        }).bounds(leftX + smallW + 2, y0 + 2 * (bh + gap), bw - 2 * smallW - 4, bh).build());
        addRenderableWidget(Button.builder(Component.literal("+"), b -> changeScale(0.1f))
                .bounds(leftX + bw - smallW, y0 + 2 * (bh + gap), smallW, bh).build());

        addRenderableWidget(Button.builder(Component.translatable("toolhud.settings.reset"), b -> {
            cfg.reset();
            rebuildWidgets();
        }).bounds(rightX, y0 + 2 * (bh + gap), bw / 2 - 2, bh).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(rightX + bw / 2 + 2, y0 + 2 * (bh + gap), bw / 2 - 2, bh).build());
    }

    private void changeScale(float delta) {
        cfg.scale = Math.round((cfg.scale + delta) * 10f) / 10f;
        cfg.clampScale();
        if (scaleButton != null) {
            scaleButton.setMessage(scaleLabel());
        }
    }

    private boolean overHud(double mx, double my) {
        int[] b = ToolHudRenderer.bounds(this.font, currentStack(), cfg);
        return mx >= b[0] && mx <= b[0] + b[2] && my >= b[1] && my <= b[1] + b[3];
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        ItemStack stack = currentStack();
        ToolHudRenderer.clampToScreen(Minecraft.getInstance(), this.font, stack, cfg);
        ToolHudRenderer.render(graphics, this.font, stack, cfg);

        // Cadre autour du HUD pour montrer qu'on peut le déplacer
        int[] b = ToolHudRenderer.bounds(this.font, stack, cfg);
        int color = (dragging || overHud(mouseX, mouseY)) ? 0xFFFFFF55 : 0xFF888888;
        graphics.fill(b[0] - 1, b[1] - 1, b[0] + b[2] + 1, b[1], color);
        graphics.fill(b[0] - 1, b[1] + b[3], b[0] + b[2] + 1, b[1] + b[3] + 1, color);
        graphics.fill(b[0] - 1, b[1], b[0], b[1] + b[3], color);
        graphics.fill(b[0] + b[2], b[1], b[0] + b[2] + 1, b[1] + b[3], color);

        graphics.centeredText(this.font, this.title, this.width / 2, 10, 0xFFFFFFFF);
        graphics.centeredText(this.font, Component.translatable("toolhud.settings.hint"),
                this.width / 2, 24, 0xFFAAAAAA);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        if (event.button() == 0 && overHud(event.x(), event.y())) {
            dragging = true;
            dragOffsetX = (int) event.x() - cfg.x;
            dragOffsetY = (int) event.y() - cfg.y;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragging) {
            cfg.x = (int) event.x() - dragOffsetX;
            cfg.y = (int) event.y() - dragOffsetY;
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging) {
            dragging = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (overHud(mouseX, mouseY)) {
            changeScale(scrollY > 0 ? 0.1f : -0.1f);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void onClose() {
        cfg.save();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
