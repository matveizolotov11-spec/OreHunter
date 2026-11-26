package ru.hitboxexpander;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;

import java.nio.file.Files;
import java.nio.file.Path;

public final class HitboxConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = Minecraft.getInstance().gameDirectory.toPath()
            .resolve("config").resolve("hitboxexpander.json");

    public static boolean enabled = false;
    public static float multiplier = 1.5f;
    public static float widthMultiplier = 1.5f;
    public static float depthMultiplier = 1.5f;
    public static float heightMultiplier = 1.0f;
    public static boolean applyToPlayers = true;
    public static boolean applyToMobs = true;

    private HitboxConfig() {}

    public static void load() {
        try {
            if (!Files.exists(FILE)) {
                save();
                return;
            }
            Data d = GSON.fromJson(Files.readString(FILE), Data.class);
            if (d == null) return;
            enabled = d.enabled;
            multiplier = clamp(d.multiplier, 1.0f, 5.0f);
            widthMultiplier = clamp(d.widthMultiplier, 1.0f, 5.0f);
            depthMultiplier = clamp(d.depthMultiplier, 1.0f, 5.0f);
            heightMultiplier = clamp(d.heightMultiplier, 1.0f, 5.0f);
            applyToPlayers = d.applyToPlayers;
            applyToMobs = d.applyToMobs;
        } catch (Exception ex) {
            System.err.println("[HitboxExpander] Config load failed: " + ex.getMessage());
        }
    }

    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Data d = new Data();
            d.enabled = enabled;
            d.multiplier = clamp(multiplier, 1.0f, 5.0f);
            d.widthMultiplier = clamp(widthMultiplier, 1.0f, 5.0f);
            d.depthMultiplier = clamp(depthMultiplier, 1.0f, 5.0f);
            d.heightMultiplier = clamp(heightMultiplier, 1.0f, 5.0f);
            d.applyToPlayers = applyToPlayers;
            d.applyToMobs = applyToMobs;
            Files.writeString(FILE, GSON.toJson(d));
        } catch (Exception ex) {
            System.err.println("[HitboxExpander] Config save failed: " + ex.getMessage());
        }
    }

    public static void reset() {
        enabled = false;
        multiplier = 1.5f;
        widthMultiplier = 1.5f;
        depthMultiplier = 1.5f;
        heightMultiplier = 1.0f;
        applyToPlayers = true;
        applyToMobs = true;
        save();
    }

    public static float clamp(float v, float min, float max) {
        if (!Float.isFinite(v)) return min;
        return Math.max(min, Math.min(max, v));
    }

    private static final class Data {
        boolean enabled = false;
        float multiplier = 1.5f;
        float widthMultiplier = 1.5f;
        float depthMultiplier = 1.5f;
        float heightMultiplier = 1.0f;
        boolean applyToPlayers = true;
        boolean applyToMobs = true;
    }
}
