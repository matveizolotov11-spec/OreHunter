package com.astrum.visuals.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.scoreboard.ReadableScoreboardScore;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import net.minecraft.client.world.ClientWorld;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class MaceDetector {
    private static final String[] EXPLOSIVE_KEYWORDS = {
            "взрыв", "explosive", "tnt", "динамит", "детонатор"
    };

    private static final Map<UUID, MaceType> SEEN_MACES = new HashMap<>();
    private static ClientWorld trackedWorld;

    private MaceDetector() {
    }

    /**
     * Remembers mace equipment as soon as the client receives it.
     * The record is kept for the current loaded world/session.
     */
    public static void tick(MinecraftClient client) {
        if (client.world == null) {
            SEEN_MACES.clear();
            trackedWorld = null;
            return;
        }

        if (trackedWorld != client.world) {
            SEEN_MACES.clear();
            trackedWorld = client.world;
        }

        for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
            rememberHeldMace(player);
        }
    }

    private static void rememberHeldMace(AbstractClientPlayerEntity player) {
        MaceType mainHand = checkStack(player.getMainHandStack());
        MaceType offHand = checkStack(player.getOffHandStack());
        MaceType found = mainHand == MaceType.EXPLOSIVE || offHand == MaceType.EXPLOSIVE
                ? MaceType.EXPLOSIVE
                : mainHand != MaceType.NONE ? mainHand : offHand;

        if (found != MaceType.NONE) {
            SEEN_MACES.put(player.getUuid(), found);
        }
    }

    public enum MaceType {
        NONE,
        NORMAL,
        EXPLOSIVE
    }

    public static MaceType getPlayerMace(AbstractClientPlayerEntity player) {
        if (player == null) {
            return MaceType.NONE;
        }

        // First catch the currently visible equipment, then use the
        // remembered result even after the player switches items.
        rememberHeldMace(player);
        return SEEN_MACES.getOrDefault(player.getUuid(), MaceType.NONE);
    }

    private static MaceType checkStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return MaceType.NONE;
        }

        boolean isMace = stack.isOf(Items.MACE);
        StringBuilder searchableText = new StringBuilder();

        Text customName = stack.get(DataComponentTypes.CUSTOM_NAME);
        if (customName != null) {
            String name = normalize(customName.getString());
            searchableText.append(name).append(' ');
            if (name.contains("булава") || name.contains("mace")) {
                isMace = true;
            }
        }

        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore != null) {
            List<Text> lines = lore.lines();
            for (Text line : lines) {
                searchableText.append(normalize(line.getString())).append(' ');
            }
        }

        if (!isMace) {
            return MaceType.NONE;
        }

        String fullText = searchableText.toString();
        for (String keyword : EXPLOSIVE_KEYWORDS) {
            if (fullText.contains(keyword)) {
                return MaceType.EXPLOSIVE;
            }
        }

        return MaceType.NORMAL;
    }

    private static String normalize(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }

        String stripped = Formatting.strip(value);
        return (stripped == null ? value : stripped).toLowerCase(Locale.ROOT);
    }

    public static float getActualHealth(AbstractClientPlayerEntity player) {
        if (player == null) {
            return 0.0f;
        }

        float health = player.getHealth() + player.getAbsorptionAmount();
        if (health == 20.0f) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world != null) {
                var scoreboard = client.world.getScoreboard();
                ScoreboardObjective objective =
                        scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.BELOW_NAME);

                if (objective != null) {
                    ReadableScoreboardScore score = scoreboard.getScore(player, objective);
                    if (score != null) {
                        health = score.getScore();
                    }
                }
            }
        }

        return Math.max(0.0f, health);
    }

    public static int getHealthColor(float health) {
        if (health > 14.0f) {
            return 0xFF55FF55;
        }
        if (health >= 7.0f) {
            return 0xFFFFFF55;
        }
        return 0xFFFF5555;
    }

    public static String formatHealth(float health) {
        int fullHearts = Math.max(0, Math.min(10, (int) Math.ceil(health / 2.0f)));
        return "❤".repeat(fullHearts);
    }
}
