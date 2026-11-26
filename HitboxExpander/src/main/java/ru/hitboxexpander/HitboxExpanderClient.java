package ru.hitboxexpander;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class HitboxExpanderClient implements ClientModInitializer {
    private static KeyMapping openMenu;
    private static KeyMapping toggleMod;

    @Override
    public void onInitializeClient() {
        HitboxConfig.load();
        openMenu = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.hitboxexpander.open_menu", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT, KeyMapping.Category.MISC));
        toggleMod = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.hitboxexpander.toggle", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_H, KeyMapping.Category.MISC));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenu.consumeClick()) {
                if (client.screen == null) client.setScreen(new HitboxExpanderScreen());
            }
            while (toggleMod.consumeClick()) {
                HitboxConfig.enabled = !HitboxConfig.enabled;
                HitboxConfig.save();
                if (client.player != null) {
                    client.player.displayClientMessage(Component.literal(
                            "Hitbox Expander " + (HitboxConfig.enabled ? "§aвключён" : "§cвыключен")), true);
                }
            }
        });
    }
}
