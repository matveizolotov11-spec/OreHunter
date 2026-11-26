package com.astrum.visuals;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public final class AstrumVisualsClient implements ClientModInitializer {
    public static boolean isEnabled = true;
    public static KeyBinding toggleKey;

    @Override
    public void onInitializeClient() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.astrumvisuals.toggle",
                GLFW.GLFW_KEY_G,
                KeyBinding.Category.create(Identifier.of("astrumvisuals", "general"))
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.wasPressed()) {
                isEnabled = !isEnabled;
                if (client.player != null) {
                    String message = isEnabled
                            ? "§a[AstrumVisuals] Индикаторы: ВКЛ"
                            : "§c[AstrumVisuals] Индикаторы: ВЫКЛ";
                    client.player.sendMessage(Text.literal(message), true);
                }
            }
        });
    }

    public static boolean canRender() {
        return isEnabled && net.minecraft.client.MinecraftClient.getInstance().world != null;
    }
}
