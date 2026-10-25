package fr.toolhud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Configuration du HUD, sauvegardée dans config/toolhud.json. */
public class ToolHudConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("toolhud.json");

    public static final float MIN_SCALE = 0.5f;
    public static final float MAX_SCALE = 4.0f;

    public boolean enabled = true;
    public int x = 10;
    public int y = 10;
    public float scale = 1.0f;
    public boolean showLore = true;
    public boolean showDurability = true;
    public boolean showBackground = true;

    private static ToolHudConfig instance;

    public static ToolHudConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    private static ToolHudConfig load() {
        if (Files.exists(FILE)) {
            try {
                ToolHudConfig cfg = GSON.fromJson(Files.readString(FILE), ToolHudConfig.class);
                if (cfg != null) {
                    cfg.clampScale();
                    return cfg;
                }
            } catch (Exception e) {
                ToolHudClient.LOGGER.warn("Impossible de lire la config, valeurs par défaut utilisées", e);
            }
        }
        return new ToolHudConfig();
    }

    public void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(this));
        } catch (IOException e) {
            ToolHudClient.LOGGER.warn("Impossible de sauvegarder la config", e);
        }
    }

    public void clampScale() {
        scale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale));
    }

    public void reset() {
        enabled = true;
        x = 10;
        y = 10;
        scale = 1.0f;
        showLore = true;
        showDurability = true;
        showBackground = true;
    }
}
