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
                && (!HitboxConfig.criticalOnly || canCriticalHit(player))
                && player.getAttackStrengthScale(0.5F) >= 0.99F) {
            queueVanillaAttack(client);
            // Short debounce prevents duplicate queued clicks; vanilla attack cooldown
            // remains the real rate limiter, so attacks happen as soon as fully charged.
            triggerCooldown = 2;
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

            previousSlot = player.getInventory().getSelectedSlot();
            axeSlot = axe;
            maceSlot = mace;
            slamTarget = target.getUUID();
            player.getInventory().setSelectedSlot(axeSlot);
            slamState = SlamState.PREPARE_AXE;
            stateTicks = 0;
            ShadowHitboxClient.notify("Stun Slam: топор выбран");
            return;
        }

        stateTicks++;
        Player target = findSlamTarget(client);
        if (target == null || player.distanceTo(target) > HitboxConfig.combatRange
                || stateTicks > 100) {
            resetSlam(client);
            return;
        }

        switch (slamState) {
            case PREPARE_AXE -> {
                // Do not waste the axe hit if the crosshair moved off the locked target
                // while the hotbar slot was changing.
                boolean aimedAtTarget = client.hitResult instanceof EntityHitResult hit
                        && hit.getEntity() == target;
                if (aimedAtTarget && player.hasLineOfSight(target)
                        && stateTicks >= 1 && player.getAttackStrengthScale(0.5F) >= 0.95F) {
                    player.getInventory().setSelectedSlot(axeSlot);
                    queueVanillaAttack(client);
                    slamState = SlamState.WAIT_FOR_SHIELD;
                    stateTicks = 0;
                    ShadowHitboxClient.notify("Stun Slam: удар топором");
                } else if (stateTicks >= 20) {
                    resetSlam(client);
                }
            }
            case WAIT_FOR_SHIELD -> {
                if (!target.isBlocking()) {
                    player.getInventory().setSelectedSlot(maceSlot);
                    slamState = SlamState.WAIT_FOR_CRIT;
                    stateTicks = 0;
                    ShadowHitboxClient.notify("Stun Slam: щит сбит, булава выбрана");
                } else if (stateTicks >= 20) {
                    // The shield did not register as disabled; restore the original slot.
                    resetSlam(client);
                }
            }
            case WAIT_FOR_CRIT -> {
                player.getInventory().setSelectedSlot(maceSlot);
                if (client.hitResult instanceof EntityHitResult hit && hit.getEntity() == target
                        && player.distanceTo(target) <= HitboxConfig.combatRange
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
        // Queue the user's actual attack binding (not a hard-coded mouse button).
        // Vanilla consumes this as a normal attack on the next input pass, allowing
        // AutoBreachSwap and other mods hooked into the normal attack path to observe it.
        KeyMapping.click(client.options.keyAttack.getKey());
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
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof AxeItem) return i;
        }
        return -1;
    }

    private static int findMace(Player player) {
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getItem(i).is(Items.MACE)) return i;
        }
        return -1;
    }

    private static void resetSlam(Minecraft client) {
        if (client.player != null && previousSlot >= 0 && previousSlot < 9) {
            client.player.getInventory().setSelectedSlot(previousSlot);
        }
        slamState = SlamState.IDLE;
        slamTarget = null;
        previousSlot = -1;
        axeSlot = -1;
        maceSlot = -1;
        stateTicks = 0;
    }

    private static UUID aimTarget;

    private static Player findTarget(Minecraft client, Player player) {
        // Keep the current target briefly to prevent aim flicker when players overlap.
        Player locked = findPlayer(client, aimTarget);
        if (locked != null && isValidTarget(client, player, locked)
                && player.distanceTo(locked) <= HitboxConfig.combatRange
                && player.hasLineOfSight(locked)) {
            Vec3 lockedPoint = aimPoint(locked).subtract(player.getEyePosition());
            float yaw = (float) (Math.toDegrees(Math.atan2(lockedPoint.z, lockedPoint.x)) - 90.0);
            float pitch = (float) -Math.toDegrees(Math.atan2(lockedPoint.y,
                    Math.sqrt(lockedPoint.x * lockedPoint.x + lockedPoint.z * lockedPoint.z)));
            float yawDiff = net.minecraft.util.Mth.wrapDegrees(yaw - player.getYRot());
            float pitchDiff = net.minecraft.util.Mth.wrapDegrees(pitch - player.getXRot());
            float angle = (float) Math.hypot(yawDiff, pitchDiff);
            if (angle <= HitboxConfig.aimFov + 8.0F) return locked;
        }

        Player best = null;
        float bestAngle = Float.MAX_VALUE;
        for (Player candidate : client.level.players()) {
            if (!isValidTarget(client, player, candidate)
                    || player.distanceTo(candidate) > HitboxConfig.combatRange
                    || !player.hasLineOfSight(candidate)) continue;

            Vec3 delta = aimPoint(candidate).subtract(player.getEyePosition());
            double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
            float targetYaw = (float) (Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0);
            float targetPitch = (float) -Math.toDegrees(Math.atan2(delta.y, horizontal));
            float yawDifference = net.minecraft.util.Mth.wrapDegrees(targetYaw - player.getYRot());
            float pitchDifference = net.minecraft.util.Mth.wrapDegrees(targetPitch - player.getXRot());
            float angle = (float) Math.hypot(yawDifference, pitchDifference);

            if (angle <= HitboxConfig.aimFov && angle < bestAngle) {
                bestAngle = angle;
                best = candidate;
            }
        }

        UUID nextTarget = best == null ? null : best.getUUID();
        aimTarget = nextTarget;
        return best;
    }

    private static Player findPlayer(Minecraft client, UUID uuid) {
        if (uuid == null || client.level == null) return null;
        for (Player candidate : client.level.players()) {
            if (uuid.equals(candidate.getUUID())) return candidate;
        }
        return null;
    }

    // Aim at the center mass of the torso, not the head or feet.
    private static Vec3 aimPoint(Player target) {
        return new Vec3(target.getX(),
                target.getY() + target.getBbHeight() * 0.55,
                target.getZ());
    }

    private static boolean isValidTarget(Minecraft client, Player player, Player target) {
        return target != player && target.isAlive() && !target.isSpectator()
                && !target.isRemoved() && client.level != null;
    }

    private static void applyAim(Player player, Player target) {
        Vec3 delta = aimPoint(target).subtract(player.getEyePosition());
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float targetYaw = (float) (Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0);
        float targetPitch = (float) -Math.toDegrees(Math.atan2(delta.y, horizontal));

        float yawDifference = net.minecraft.util.Mth.wrapDegrees(targetYaw - player.getYRot());
        float pitchDifference = targetPitch - player.getXRot();

        // Ease toward the torso with a bounded step. The step shrinks with the
        // remaining angle, avoiding the old velocity overshoot and micro-jitter.
        float speed = HitboxConfig.aimSpeed;
        float response = net.minecraft.util.Mth.clamp(0.52F + speed * 0.28F, 0.60F, 0.94F);
        float maxYawStep = 10.0F + speed * 14.0F;
        float maxPitchStep = maxYawStep * 0.78F;

        float yawStep = net.minecraft.util.Mth.clamp(yawDifference * response,
                -maxYawStep, maxYawStep);
        float pitchStep = net.minecraft.util.Mth.clamp(pitchDifference * response,
                -maxPitchStep, maxPitchStep);

        if (Math.abs(yawDifference) < 0.035F) yawStep = yawDifference;
        if (Math.abs(pitchDifference) < 0.035F) pitchStep = pitchDifference;

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
