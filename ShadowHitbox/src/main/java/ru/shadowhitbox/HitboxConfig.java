package ru.shadowhitbox;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class HitboxConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = Minecraft.getInstance().gameDirectory.toPath()
            .resolve("config").resolve("shadowhitbox.json");

    public static boolean expandEnabled = false;
    public static boolean espEnabled = true;
    public static boolean showSelf = false;
    public static boolean aimAssistEnabled = false;
    public static boolean triggerBotEnabled = false;
    public static boolean autoStunSlamEnabled = false;
    public static boolean criticalOnly = true;
    public static float width = 0.90f;
    public static float height = 2.10f;
    public static float red = 0.25f;
    public static float green = 0.85f;
    public static float blue = 1.00f;
    public static float alpha = 0.85f;
    public static float combatRange = 4.0f;
    public static float aimFov = 45.0f;
    public static float aimSpeed = 0.85f;

    private HitboxConfig() {}

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                Data data = GSON.fromJson(Files.readString(FILE), Data.class);
                if (data != null) {
                    expandEnabled = data.expandEnabled;
                    espEnabled = data.espEnabled;
                    showSelf = data.showSelf;
                    aimAssistEnabled = data.aimAssistEnabled;
                    triggerBotEnabled = data.triggerBotEnabled;
                    autoStunSlamEnabled = data.autoStunSlamEnabled;
                    criticalOnly = data.criticalOnly;
                    width = clamp(data.width, 0.6f, 2.5f);
                    height = clamp(data.height, 1.0f, 3.5f);
                    red = clamp(data.red, 0f, 1f);
                    green = clamp(data.green, 0f, 1f);
                    blue = clamp(data.blue, 0f, 1f);
                    alpha = clamp(data.alpha, 0.05f, 1f);
                    combatRange = clamp(data.combatRange, 2.5f, 6.0f);
                    aimFov = clamp(data.aimFov, 5f, 180f);
                    aimSpeed = data.aimSpeed > 0.0f
                            ? clamp(data.aimSpeed, 0.15f, 1.5f)
                            : clamp(6.8f / Math.max(1.0f, data.aimSmoothness), 0.15f, 1.5f);
                }
            }
        } catch (Exception ignored) {
            // Keep defaults if the config is damaged.
        }
    }

    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Data data = new Data();
            data.expandEnabled = expandEnabled;
            data.espEnabled = espEnabled;
            data.showSelf = showSelf;
            data.aimAssistEnabled = aimAssistEnabled;
            data.triggerBotEnabled = triggerBotEnabled;
            data.autoStunSlamEnabled = autoStunSlamEnabled;
            data.criticalOnly = criticalOnly;
            data.width = width;
            data.height = height;
            data.red = red;
            data.green = green;
            data.blue = blue;
            data.alpha = alpha;
            data.combatRange = combatRange;
            data.aimFov = aimFov;
            data.aimSpeed = aimSpeed;
            Files.writeString(FILE, GSON.toJson(data));
        } catch (IOException exception) {
            System.err.println("[ShadowHitbox] Could not save config: " + exception.getMessage());
        }
    }

    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class Data {
        boolean expandEnabled;
        boolean espEnabled = true;
        boolean showSelf;
        boolean aimAssistEnabled;
        boolean triggerBotEnabled;
        boolean autoStunSlamEnabled;
        boolean criticalOnly = true;
        float width = 0.90f;
        float height = 2.10f;
        float red = 0.25f;
        float green = 0.85f;
        float blue = 1.00f;
        float alpha = 0.85f;
        float combatRange = 4.0f;
        float aimFov = 45.0f;
        float aimSpeed = 0.85f;
        float aimSmoothness = 8.0f;
    }
}
