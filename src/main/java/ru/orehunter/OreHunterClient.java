package ru.orehunter;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
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
    private static int scanCooldown = 0;

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
            releaseMovement(client);
        }
        if (!enabled || client.player == null || client.level == null || client.gameMode == null) {
            releaseMovement(client);
            return;
        }
        if (scanCooldown-- <= 0 || !isOre(client, target)) {
            target = findNearestOre(client);
            scanCooldown = SEARCH_INTERVAL;
        }
        if (target == null) {
            releaseMovement(client);
            return;
        }

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
            releaseMovement(client);
            if (client.hitResult instanceof BlockHitResult hit && hit.getBlockPos().equals(target)) {
                client.gameMode.startDestroyBlock(target, hit.getDirection());
                client.gameMode.continueDestroyBlock(target, hit.getDirection());
                client.player.swing(InteractionHand.MAIN_HAND);
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

    private static void releaseMovement(Minecraft client) {
        if (client.options == null) return;
        client.options.keyUp.setDown(false);
        client.options.keyDown.setDown(false);
        client.options.keyLeft.setDown(false);
        client.options.keyRight.setDown(false);
    }
}
