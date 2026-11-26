package ru.shadowhitbox;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

public final class ShadowHitboxClient implements ClientModInitializer {
    private static KeyMapping openMenu;
    private static KeyMapping toggleAim;
    private static KeyMapping toggleTrigger;
    private static KeyMapping toggleEsp;
    private static KeyMapping toggleExpand;
    private static KeyMapping toggleSelf;

    @Override
    public void onInitializeClient() {
        HitboxConfig.load();
        HitboxRenderer.register();

        openMenu = register("key.shadowhitbox.open", GLFW.GLFW_KEY_RIGHT_SHIFT);
        toggleAim = register("key.shadowhitbox.toggle_aim", GLFW.GLFW_KEY_G);
        toggleTrigger = register("key.shadowhitbox.toggle_trigger", GLFW.GLFW_KEY_H);
        toggleEsp = register("key.shadowhitbox.toggle_esp", GLFW.GLFW_KEY_J);
        toggleExpand = register("key.shadowhitbox.toggle_expand", GLFW.GLFW_KEY_K);
        toggleSelf = register("key.shadowhitbox.toggle_self", GLFW.GLFW_KEY_L);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenu.consumeClick()) {
                if (client.screen == null) client.setScreen(new ShadowHitboxScreen());
            }
            while (toggleAim.consumeClick()) {
                HitboxConfig.aimAssistEnabled = !HitboxConfig.aimAssistEnabled;
                notifyToggle("Aim Assist", HitboxConfig.aimAssistEnabled);
            }
            while (toggleTrigger.consumeClick()) {
                HitboxConfig.triggerBotEnabled = !HitboxConfig.triggerBotEnabled;
                notifyToggle("Trigger Bot", HitboxConfig.triggerBotEnabled);
            }
            while (toggleEsp.consumeClick()) {
                HitboxConfig.espEnabled = !HitboxConfig.espEnabled;
                notifyToggle("Hitbox ESP", HitboxConfig.espEnabled);
            }
            while (toggleExpand.consumeClick()) {
                HitboxConfig.expandEnabled = !HitboxConfig.expandEnabled;
                notifyToggle("Expand ESP", HitboxConfig.expandEnabled);
            }
            while (toggleSelf.consumeClick()) {
                HitboxConfig.showSelf = !HitboxConfig.showSelf;
                notifyToggle("Show Self", HitboxConfig.showSelf);
            }
            CombatAssist.tick(client);
        });
    }

    private static KeyMapping register(String translationKey, int defaultKey) {
        return KeyBindingHelper.registerKeyBinding(new KeyMapping(
                translationKey,
                InputConstants.Type.KEYSYM,
                defaultKey,
                KeyMapping.Category.MISC
        ));
    }

    private static void notifyToggle(String feature, boolean enabled) {
        HitboxConfig.save();
        notify(feature + (enabled ? " включён" : " выключен"));
    }

    public static void notify(String message) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) client.player.displayClientMessage(Component.literal(message), true);
    }
}
