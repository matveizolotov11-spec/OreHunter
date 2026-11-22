package ru.orehunter;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

public final class OreHunterClient implements ClientModInitializer {
    private static final int SEARCH_RADIUS = 16;
    private static final int SEARCH_INTERVAL = 10;
    private static final double MINE_DISTANCE = 4.0;
    private static KeyMapping toggleKey;
    private static boolean enabled;
    private static BlockPos target;
    private static BlockPos miningTarget;
    private static int scanCooldown;
    private static int eatCooldown;
    private static int attackCooldown;
    private static int statusCooldown;

    @Override
    public void onInitializeClient() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.orehunter.toggle", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_J, KeyMapping.Category.MISC));
        ClientTickEvents.END_CLIENT_TICK.register(OreHunterClient::tick);
    }

    private static void tick(Minecraft client) {
        while (toggleKey.consumeClick()) {
            enabled = !enabled;
            target = null;
            miningTarget = null;
            scanCooldown = 0;
            if (client.player != null) {
                client.player.displayClientMessage(Component.literal(enabled ? "OreHunter: ON" : "OreHunter: OFF"), true);
            }
        }
        if (!enabled || client.player == null || client.level == null || client.gameMode == null) return;

        // Manual WASD always has priority. The bot yields control immediately.
        if (hasManualMovement(client)) return;

        if (eatCooldown > 0) eatCooldown--;
        if (attackCooldown > 0) attackCooldown--;
        if (statusCooldown-- <= 0) {
            String msg = target == null ? "OreHunter: ищу руду..." :
                    "OreHunter: цель " + target.getX() + " " + target.getY() + " " + target.getZ();
            client.player.displayClientMessage(Component.literal(msg), true);
            statusCooldown = 30;
        }

        // Eat from the hotbar when hunger is low.
        if (client.player.getFoodData().getFoodLevel() <= 14 && eatCooldown == 0) {
            int foodSlot = findFoodSlot(client);
            if (foodSlot >= 0) {
                int oldSlot = client.player.getInventory().getSelectedSlot();
                client.player.getInventory().setSelectedSlot(foodSlot);
                client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
                eatCooldown = 35;
                if (oldSlot != foodSlot) client.player.getInventory().setSelectedSlot(oldSlot);
                stopForward(client);
                return;
            }
        }

        // Attack mobs that are actively targeting the player.
        Mob threat = findThreat(client);
        if (threat != null) {
            stopForward(client);
            face(client, threat.position().add(0, threat.getBbHeight() * 0.65, 0));
            if (attackCooldown == 0) {
                int oldSlot = client.player.getInventory().getSelectedSlot();
                int weapon = findSwordSlot(client);
                if (weapon >= 0) client.player.getInventory().setSelectedSlot(weapon);
                client.gameMode.attack(client.player, threat);
                client.player.swing(InteractionHand.MAIN_HAND);
                client.player.getInventory().setSelectedSlot(oldSlot);
                attackCooldown = 10;
            }
            return;
        }

        // Move toward nearby dropped items so they can be collected.
        ItemEntity drop = findNearbyDrop(client);
        if (drop != null) {
            double d = client.player.position().distanceTo(drop.position());
            face(client, drop.position());
            client.options.keyUp.setDown(d > 1.25);
            return;
        }

        if (scanCooldown-- <= 0 || !isOre(client, target)) {
            target = findNearestOre(client);
            scanCooldown = SEARCH_INTERVAL;
            if (target == null || !isOre(client, target)) miningTarget = null;
        }

        if (target == null) {
            // Basic exploration fallback; no blind digging through unknown blocks.
            client.options.keyUp.setDown(true);
            return;
        }

        face(client, Vec3.atCenterOf(target));
        double distance = client.player.position().distanceTo(Vec3.atCenterOf(target));
        if (distance > MINE_DISTANCE) {
            client.options.keyUp.setDown(true);
            return;
        }

        client.options.keyUp.setDown(false);
        selectBestTool(client, client.level.getBlockState(target));
        if (client.hitResult instanceof BlockHitResult hit && hit.getBlockPos().equals(target)) {
            if (!target.equals(miningTarget)) {
                miningTarget = target.immutable();
                client.gameMode.startDestroyBlock(target, hit.getDirection());
            }
            client.gameMode.continueDestroyBlock(target, hit.getDirection());
            client.player.swing(InteractionHand.MAIN_HAND);
        } else {
            miningTarget = null;
        }
    }

    private static boolean hasManualMovement(Minecraft client) {
        long window = client.getWindow().getWindow();
        boolean manual = pressed(window, client.options.keyUp) || pressed(window, client.options.keyDown)
                || pressed(window, client.options.keyLeft) || pressed(window, client.options.keyRight);
        if (manual) {
            client.options.keyUp.setDown(pressed(window, client.options.keyUp));
            client.options.keyDown.setDown(pressed(window, client.options.keyDown));
            client.options.keyLeft.setDown(pressed(window, client.options.keyLeft));
            client.options.keyRight.setDown(pressed(window, client.options.keyRight));
        }
        return manual;
    }

    private static void stopForward(Minecraft client) {
        long window = client.getWindow().getWindow();
        client.options.keyUp.setDown(pressed(window, client.options.keyUp));
    }

    private static Mob findThreat(Minecraft client) {
        return client.level.getEntitiesOfClass(Mob.class, client.player.getBoundingBox().inflate(4.5),
                mob -> mob.isAlive() && mob.getTarget() == client.player)
                .stream().min((a, b) -> Double.compare(a.distanceToSqr(client.player),
                        b.distanceToSqr(client.player))).orElse(null);
    }

    private static ItemEntity findNearbyDrop(Minecraft client) {
        return client.level.getEntitiesOfClass(ItemEntity.class, client.player.getBoundingBox().inflate(8.0),
                ItemEntity::isAlive).stream()
                .min((a, b) -> Double.compare(a.distanceToSqr(client.player), b.distanceToSqr(client.player)))
                .orElse(null);
    }

    private static int findFoodSlot(Minecraft client) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = client.player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.isEdible()) return i;
        }
        return -1;
    }

    private static int findSwordSlot(Minecraft client) {
        int best = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = client.player.getInventory().getItem(i);
            if (stack.getItem() instanceof SwordItem && !stack.isEmpty()
                    && stack.getDamageValue() < stack.getMaxDamage() - 1) best = i;
        }
        return best;
    }

    private static void selectBestTool(Minecraft client, BlockState state) {
        int bestSlot = client.player.getInventory().getSelectedSlot();
        float bestSpeed = -1.0F;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = client.player.getInventory().getItem(i);
            if (stack.isEmpty() || stack.getDamageValue() >= stack.getMaxDamage() - 1) continue;
            float speed = stack.getDestroySpeed(state);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = i;
            }
        }
        client.player.getInventory().setSelectedSlot(bestSlot);
    }

    private static void face(Minecraft client, Vec3 point) {
        Vec3 delta = point.subtract(client.player.getEyePosition());
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float yaw = (float)(Mth.atan2(delta.z, delta.x) * (180.0 / Math.PI)) - 90.0F;
        float pitch = (float)(-(Mth.atan2(delta.y, horizontal) * (180.0 / Math.PI)));
        client.player.setYRot(yaw);
        client.player.setYHeadRot(yaw);
        client.player.setXRot(pitch);
    }

    private static BlockPos findNearestOre(Minecraft client) {
        BlockPos origin = client.player.blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int x = -SEARCH_RADIUS; x <= SEARCH_RADIUS; x++)
            for (int y = -SEARCH_RADIUS; y <= SEARCH_RADIUS; y++)
                for (int z = -SEARCH_RADIUS; z <= SEARCH_RADIUS; z++) {
                    BlockPos pos = origin.offset(x, y, z);
                    if (!isOre(client, pos)) continue;
                    double distance = client.player.position().distanceToSqr(Vec3.atCenterOf(pos));
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = pos.immutable();
                    }
                }
        return best;
    }

    private static boolean isOre(Minecraft client, BlockPos pos) {
        if (pos == null || client.level == null) return false;
        BlockState state = client.level.getBlockState(pos);
        return state.is(Blocks.DIAMOND_ORE) || state.is(Blocks.DEEPSLATE_DIAMOND_ORE)
                || state.is(Blocks.ANCIENT_DEBRIS);
    }

    private static boolean pressed(long window, KeyMapping mapping) {
        int key = mapping.getKey().getValue();
        return key >= 0 && GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
    }
}
