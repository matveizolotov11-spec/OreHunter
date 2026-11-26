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
    public static float width = 0.90f;
    public static float height = 2.10f;
    public static float red = 0.25f;
    public static float green = 0.85f;
    public static float blue = 1.00f;
    public static float alpha = 0.85f;

    private HitboxConfig() {}

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                Data data = GSON.fromJson(Files.readString(FILE), Data.class);
                if (data != null) {
                    expandEnabled = data.expandEnabled;
                    espEnabled = data.espEnabled;
                    showSelf = data.showSelf;
                    width = clamp(data.width, 0.6f, 2.5f);
                    height = clamp(data.height, 1.0f, 3.5f);
                    red = clamp(data.red, 0f, 1f);
                    green = clamp(data.green, 0f, 1f);
                    blue = clamp(data.blue, 0f, 1f);
                    alpha = clamp(data.alpha, 0.05f, 1f);
                }
            }
        } catch (Exception ignored) {
            // Keep safe defaults if the config is damaged.
        }
    }

    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Data data = new Data();
            data.expandEnabled = expandEnabled;
            data.espEnabled = espEnabled;
            data.showSelf = showSelf;
            data.width = width;
            data.height = height;
            data.red = red;
            data.green = green;
            data.blue = blue;
            data.alpha = alpha;
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
        float width = 0.90f;
        float height = 2.10f;
        float red = 0.25f;
        float green = 0.85f;
        float blue = 1.00f;
        float alpha = 0.85f;
    }
}
