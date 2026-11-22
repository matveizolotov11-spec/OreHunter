package ru.orehunter;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

public final class OreHunterClient implements ClientModInitializer {
    private static final int SEARCH_RADIUS = 16;
    private static final int SEARCH_INTERVAL = 10;
    private static KeyMapping toggleKey;
    private static boolean enabled = false;
    private static BlockPos target = null;
    private static BlockPos miningTarget = null;
    private static int scanCooldown = 0;
    private static int exploreTicks = 0;

    @Override
    public void onInitializeClient() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.orehunter.toggle",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_J,
                KeyMapping.Category.MISC
        ));
        ClientTickEvents.END_CLIENT_TICK.register(OreHunterClient::tick);
    }

    private static void tick(Minecraft client) {
        while (toggleKey.consumeClick()) {
            enabled = !enabled;
            target = null;
            scanCooldown = 0;
            if (!enabled) restorePhysicalMovement(client);
            if (client.player != null) {
                client.player.displayClientMessage(Component.literal(enabled ? "OreHunter: ON" : "OreHunter: OFF"), true);
            }
        }
        if (!enabled || client.player == null || client.level == null || client.gameMode == null) return;
        if (scanCooldown-- <= 0 || !isOre(client, target)) {
            target = findNearestOre(client);
            scanCooldown = SEARCH_INTERVAL;
        }
        if (target == null) {
            // No ore in the current scan radius: keep exploring instead of standing still.
            exploreTicks++;
            if (exploreTicks >= 80) {
                client.player.setYRot(client.player.getYRot() + 90.0F);
                client.player.setYHeadRot(client.player.getYRot());
                exploreTicks = 0;
            }
            client.options.keyUp.setDown(true);
            client.options.keyDown.setDown(false);
            client.options.keyLeft.setDown(false);
            client.options.keyRight.setDown(false);
            if (scanCooldown == SEARCH_INTERVAL - 1) {
                client.player.displayClientMessage(Component.literal("OreHunter: searching for ores..."), true);
            }
            return;
        }
        exploreTicks = 0;
        client.player.displayClientMessage(Component.literal("OreHunter: target " + target.getX() + " " + target.getY() + " " + target.getZ()), true);

        Vec3 eye = client.player.getEyePosition();
        Vec3 delta = Vec3.atCenterOf(target).subtract(eye);
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float yaw = (float) (Mth.atan2(delta.z, delta.x) * (180.0 / Math.PI)) - 90.0F;
        float pitch = (float) (-(Mth.atan2(delta.y, horizontal) * (180.0 / Math.PI)));
        client.player.setYRot(yaw);
        client.player.setYHeadRot(yaw);
        client.player.setXRot(pitch);

        double distance = client.player.position().distanceTo(Vec3.atCenterOf(target));
        if (distance > 4.2) {
            client.options.keyUp.setDown(true);
            client.options.keyDown.setDown(false);
            client.options.keyLeft.setDown(false);
            client.options.keyRight.setDown(false);
        } else {
            restorePhysicalMovement(client);
            if (client.hitResult instanceof BlockHitResult hit && hit.getBlockPos().equals(target)) {
                if (!target.equals(miningTarget)) {
                    miningTarget = target.immutable();
                    client.gameMode.startDestroyBlock(target, hit.getDirection());
                } else {
                    client.gameMode.continueDestroyBlock(target, hit.getDirection());
                    client.player.swing(InteractionHand.MAIN_HAND);
                }
            } else {
                miningTarget = null;
            }
        }
    }

    private static BlockPos findNearestOre(Minecraft client) {
        BlockPos origin = client.player.blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int x = -SEARCH_RADIUS; x <= SEARCH_RADIUS; x++) {
            for (int y = -SEARCH_RADIUS; y <= SEARCH_RADIUS; y++) {
                for (int z = -SEARCH_RADIUS; z <= SEARCH_RADIUS; z++) {
                    BlockPos pos = origin.offset(x, y, z);
                    if (!isOre(client, pos)) continue;
                    double distance = client.player.position().distanceToSqr(Vec3.atCenterOf(pos));
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = pos.immutable();
                    }
                }
            }
        }
        return best;
    }

    private static boolean isOre(Minecraft client, BlockPos pos) {
        if (pos == null || client.level == null) return false;
        BlockState state = client.level.getBlockState(pos);
        return state.is(Blocks.DIAMOND_ORE)
                || state.is(Blocks.DEEPSLATE_DIAMOND_ORE)
                || state.is(Blocks.ANCIENT_DEBRIS);
    }

    private static void restorePhysicalMovement(Minecraft client) {
        if (client.options == null || client.getWindow() == null) return;
        long window = client.getWindow().getWindow();
        client.options.keyUp.setDown(isPhysicallyPressed(window, client.options.keyUp));
        client.options.keyDown.setDown(isPhysicallyPressed(window, client.options.keyDown));
        client.options.keyLeft.setDown(isPhysicallyPressed(window, client.options.keyLeft));
        client.options.keyRight.setDown(isPhysicallyPressed(window, client.options.keyRight));
    }

    private static boolean isPhysicallyPressed(long window, KeyMapping mapping) {
        int key = mapping.getKey().getValue();
        return key >= 0 && GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
    }
}
