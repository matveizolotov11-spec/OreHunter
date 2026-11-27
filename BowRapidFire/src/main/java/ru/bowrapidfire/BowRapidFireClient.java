package ru.bowrapidfire;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;

public final class BowRapidFireClient implements ClientModInitializer {
    private static KeyMapping fireKey;
    private static KeyMapping menuKey;
    private static int chargeTicks;
    private static int cooldown;
    private static int previousSlot = -1;
    private static boolean wasUsing;
    private static int swappedInventorySlot = -1;
    private static boolean enabled = true;
    private static final int MIN_CHARGE_TICKS = 1;

    @Override
    public void onInitializeClient() {
        fireKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.bowrapidfire.fire", InputConstants.Type.MOUSE,
                GLFW.GLFW_MOUSE_BUTTON_4, KeyMapping.Category.MISC));
        menuKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.bowrapidfire.menu", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT, KeyMapping.Category.MISC));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (menuKey.consumeClick()) {
                if (client.screen == null) client.setScreen(new BowRapidFireScreen());
            }
            tick(client);
        });
    }

    private static void tick(Minecraft client) {
        Player player = client.player;
        if (player == null || client.gameMode == null || client.screen != null || !enabled) {
            stop(client);
            return;
        }

        boolean held = fireKey.isDown();
        if (!held) {
            stop(client);
            return;
        }

        if (cooldown > 0) cooldown--;
        if (cooldown > 0) return;

        if (!player.isUsingItem()) {
            int bowSlot = findBow(player.getInventory());
            if (bowSlot < 0) return;

            Inventory inv = player.getInventory();
            if (bowSlot < 9) {
                if (previousSlot < 0) previousSlot = inv.getSelectedSlot();
                inv.setSelectedSlot(bowSlot);
            } else {
                // Move the inventory bow into the selected hotbar slot.
                ItemStack hand = inv.getSelectedItem();
                ItemStack bow = inv.getItem(bowSlot);
                swappedInventorySlot = bowSlot;
                inv.setItem(bowSlot, hand);
                inv.setItem(inv.getSelectedSlot(), bow);
                if (previousSlot < 0) previousSlot = inv.getSelectedSlot();
            }

            InteractionHand hand = player.getMainHandItem().getItem() instanceof BowItem
                    ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            if (hand == InteractionHand.OFF_HAND && !(player.getOffhandItem().getItem() instanceof BowItem)) return;
            client.gameMode.useItem(player, hand);
            chargeTicks = 0;
            wasUsing = true;
            return;
        }

        if (player.getUseItem().getItem() instanceof BowItem) {
            chargeTicks++;
            if (chargeTicks >= MIN_CHARGE_TICKS) {
                player.releaseUsingItem();
                cooldown = 1;
                chargeTicks = 0;
            }
        } else {
            stop(client);
        }
    }

    private static int findBow(Player player) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < 9; i++) {
            if (inv.getItem(i).getItem() instanceof BowItem) return i;
        }
        for (int i = 9; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).getItem() instanceof BowItem) return i;
        }
        return -1;
    }

    private static void stop(Minecraft client) {
        if (client.player != null && client.player.isUsingItem()
                && client.player.getUseItem().getItem() instanceof BowItem) {
            client.player.releaseUsingItem();
        }
        if (client.player != null && previousSlot >= 0 && previousSlot < 9) {
            Inventory inv = client.player.getInventory();
            if (swappedInventorySlot >= 9 && swappedInventorySlot < inv.getContainerSize()) {
                ItemStack held = inv.getItem(previousSlot);
                ItemStack stored = inv.getItem(swappedInventorySlot);
                inv.setItem(previousSlot, stored);
                inv.setItem(swappedInventorySlot, held);
            }
            inv.setSelectedSlot(previousSlot);
        }
        swappedInventorySlot = -1;
        previousSlot = -1;
        chargeTicks = 0;
        cooldown = 0;
        wasUsing = false;
    }

    static boolean isEnabled() { return enabled; }
    static void setEnabled(boolean value) { enabled = value; }
}
