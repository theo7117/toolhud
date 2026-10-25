package fr.toolhud;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ToolHudClient implements ClientModInitializer {
    public static final String MOD_ID = "toolhud";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));

    private static KeyMapping openSettingsKey;
    private static KeyMapping toggleKey;

    @Override
    public void onInitializeClient() {
        ToolHudConfig cfg = ToolHudConfig.get();

        openSettingsKey = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.toolhud.settings", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, CATEGORY));
        toggleKey = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.toolhud.toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (openSettingsKey.consumeClick()) {
                if (mc.player != null && mc.gui.screen() == null) {
                    mc.gui.setScreen(new ToolHudSettingsScreen());
                }
            }
            while (toggleKey.consumeClick()) {
                cfg.enabled = !cfg.enabled;
                cfg.save();
            }
        });

        HudElementRegistry.attachElementAfter(
                VanillaHudElements.HOTBAR,
                Identifier.fromNamespaceAndPath(MOD_ID, "tool_hud"),
                (graphics, deltaTracker) -> {
                    Minecraft mc = Minecraft.getInstance();
                    if (!cfg.enabled || mc.player == null || mc.gui.hud.isHidden()
                            || mc.gui.screen() instanceof ToolHudSettingsScreen) {
                        return;
                    }
                    ItemStack stack = mc.player.getMainHandItem();
                    if (stack.isEmpty()) {
                        return;
                    }
                    ToolHudRenderer.clampToScreen(mc, mc.font, stack, cfg);
                    ToolHudRenderer.render(graphics, mc.font, stack, cfg);
                });
    }
}
