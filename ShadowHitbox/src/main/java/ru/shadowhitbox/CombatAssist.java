package ru.shadowhitbox;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public final class CombatAssist {
    private enum SlamState { IDLE, PREPARE_AXE, WAIT_FOR_SHIELD, WAIT_FOR_CRIT, RETURN_WEAPON }

    private static SlamState slamState = SlamState.IDLE;
    private static UUID slamTarget;
    private static int previousSlot = -1;
    private static int axeSlot = -1;
    private static int maceSlot = -1;
    private static int stateTicks;
    private static int triggerCooldown;

    private CombatAssist() {}

    public static void tick(Minecraft client) {
        if (client.player == null || client.level == null || client.gameMode == null
                || client.screen != null || client.player.isSpectator()) {
            resetSlam(client);
            return;
        }

        Player player = client.player;
        if (triggerCooldown > 0) triggerCooldown--;

        if (HitboxConfig.aimAssistEnabled) {
            Player target = findTarget(client, player);
            if (target != null) applyAim(player, target);
        }

        if (HitboxConfig.autoStunSlamEnabled) {
            tickStunSlam(client, player);
            if (slamState != SlamState.IDLE) return;
        } else if (slamState != SlamState.IDLE) {
            resetSlam(client);
        }

        if (HitboxConfig.triggerBotEnabled && triggerCooldown == 0
                && client.hitResult instanceof EntityHitResult hit
                && hit.getEntity() instanceof Player target && isValidTarget(client, player, target)
                && player.distanceTo(target) <= HitboxConfig.combatRange
                && player.hasLineOfSight(target)
                && canCriticalHit(player)
                && player.getAttackStrengthScale(0.5F) >= 0.99F) {
            queueVanillaAttack(client);
            triggerCooldown = 8;
        }
    }

    private static void tickStunSlam(Minecraft client, Player player) {
        if (slamState == SlamState.IDLE) {
            if (!(client.hitResult instanceof EntityHitResult hit)
                    || !(hit.getEntity() instanceof Player target)
                    || !isValidTarget(client, player, target)
                    || !target.isBlocking()
                    || player.distanceTo(target) > HitboxConfig.combatRange
                    || !player.hasLineOfSight(target)) return;

            int axe = findAxe(player);
            int mace = findMace(player);
            if (axe < 0 || mace < 0) return;

            previousSlot = player.getInventory().selected;
            axeSlot = axe;
            maceSlot = mace;
            slamTarget = target.getUUID();
            player.getInventory().selected = axeSlot;
            slamState = SlamState.PREPARE_AXE;
            stateTicks = 0;
            ShadowHitboxClient.notify("Stun Slam: топор выбран");
            return;
        }

        stateTicks++;
        Player target = findSlamTarget(client);
        if (target == null || player.distanceTo(target) > HitboxConfig.combatRange
                || stateTicks > 50) {
            resetSlam(client);
            return;
        }

        switch (slamState) {
            case PREPARE_AXE -> {
                if (stateTicks >= 1 && player.getAttackStrengthScale(0.5F) >= 0.95F) {
                    player.getInventory().selected = axeSlot;
                    queueVanillaAttack(client);
                    slamState = SlamState.WAIT_FOR_SHIELD;
                    stateTicks = 0;
                    ShadowHitboxClient.notify("Stun Slam: удар топором");
                }
            }
            case WAIT_FOR_SHIELD -> {
                if (!target.isBlocking()) {
                    player.getInventory().selected = maceSlot;
                    slamState = SlamState.WAIT_FOR_CRIT;
                    stateTicks = 0;
                    ShadowHitboxClient.notify("Stun Slam: щит сбит, булава выбрана");
                } else if (stateTicks >= 12) {
                    resetSlam(client);
                }
            }
            case WAIT_FOR_CRIT -> {
                player.getInventory().selected = maceSlot;
                if (client.hitResult instanceof EntityHitResult hit && hit.getEntity() == target
                        && player.hasLineOfSight(target)
                        && canCriticalHit(player)
                        && player.getAttackStrengthScale(0.5F) >= 0.99F) {
                    queueVanillaAttack(client);
                    slamState = SlamState.RETURN_WEAPON;
                    stateTicks = 0;
                    triggerCooldown = 8;
                    ShadowHitboxClient.notify("Stun Slam: удар булавой");
                }
            }
            case RETURN_WEAPON -> {
                if (stateTicks >= 2) resetSlam(client);
            }
            case IDLE -> { }
        }
    }

    private static void queueVanillaAttack(Minecraft client) {
        // Queue the configured vanilla attack binding. This goes through the normal
        // client attack path so other client mods listening for a real attack can react.
        KeyMapping.click(InputConstants.Type.MOUSE.getOrCreate(0));
    }

    private static Player findSlamTarget(Minecraft client) {
        if (slamTarget == null || client.level == null) return null;
        for (Player candidate : client.level.players()) {
            if (candidate.getUUID().equals(slamTarget) && candidate.isAlive()
                    && !candidate.isRemoved() && !candidate.isSpectator()) return candidate;
        }
        return null;
    }

    private static int findAxe(Player player) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().items.get(i);
            if (stack.getItem() instanceof AxeItem) return i;
        }
        return -1;
    }

    private static int findMace(Player player) {
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().items.get(i).is(Items.MACE)) return i;
        }
        return -1;
    }

    private static void resetSlam(Minecraft client) {
        if (client.player != null && previousSlot >= 0 && previousSlot < 9) {
            client.player.getInventory().selected = previousSlot;
        }
        slamState = SlamState.IDLE;
        slamTarget = null;
        previousSlot = -1;
        axeSlot = -1;
        maceSlot = -1;
        stateTicks = 0;
    }

    private static Player findTarget(Minecraft client, Player player) {
        Player best = null;
        float bestAngle = Float.MAX_VALUE;
        for (Player candidate : client.level.players()) {
            if (!isValidTarget(client, player, candidate)
                    || player.distanceTo(candidate) > HitboxConfig.combatRange
                    || !player.hasLineOfSight(candidate)) continue;

            Vec3 delta = candidate.getBoundingBox().getCenter().subtract(player.getEyePosition());
            double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
            float targetYaw = (float) (Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0);
            float targetPitch = (float) -Math.toDegrees(Math.atan2(delta.y, horizontal));
            float yawDifference = net.minecraft.util.Mth.wrapDegrees(targetYaw - player.getYRot());
            float pitchDifference = net.minecraft.util.Mth.wrapDegrees(targetPitch - player.getXRot());
            float angle = (float) Math.sqrt(yawDifference * yawDifference + pitchDifference * pitchDifference);

            if (angle <= HitboxConfig.aimFov && angle < bestAngle) {
                bestAngle = angle;
                best = candidate;
            }
        }
        return best;
    }

    private static boolean isValidTarget(Minecraft client, Player player, Player target) {
        return target != player && target.isAlive() && !target.isSpectator()
                && !target.isRemoved() && client.level != null;
    }

    private static void applyAim(Player player, Player target) {
        Vec3 delta = target.getBoundingBox().getCenter().subtract(player.getEyePosition());
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float targetYaw = (float) (Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0);
        float targetPitch = (float) -Math.toDegrees(Math.atan2(delta.y, horizontal));

        float yawDifference = net.minecraft.util.Mth.wrapDegrees(targetYaw - player.getYRot());
        float pitchDifference = net.minecraft.util.Mth.wrapDegrees(targetPitch - player.getXRot());
        float factor = 1.0F - (float) Math.exp(-HitboxConfig.aimSpeed * 0.38F);
        float yawStep = net.minecraft.util.Mth.clamp(yawDifference * factor, -22.0F, 22.0F);
        float pitchStep = net.minecraft.util.Mth.clamp(pitchDifference * factor, -16.0F, 16.0F);
        player.setYRot(player.getYRot() + yawStep);
        player.setXRot(net.minecraft.util.Mth.clamp(player.getXRot() + pitchStep, -90.0F, 90.0F));
    }

    private static boolean canCriticalHit(Player player) {
        return !player.onGround() && player.fallDistance > 0.0F
                && !player.isInWater() && !player.isInLava()
                && !player.onClimbable() && !player.isPassenger()
                && !player.isSprinting() && !player.isFallFlying()
                && !player.getAbilities().flying;
    }
}
