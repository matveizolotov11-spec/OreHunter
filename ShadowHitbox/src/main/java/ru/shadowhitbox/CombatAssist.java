package ru.shadowhitbox;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public final class CombatAssist {
    private CombatAssist() {}

    public static void tick(Minecraft client) {
        if (client.player == null || client.level == null || client.gameMode == null
                || client.screen != null || client.player.isSpectator()) return;

        Player player = client.player;

        if (HitboxConfig.aimAssistEnabled) {
            Player target = findTarget(client, player);
            if (target != null) applyAim(player, target);
        }

        if (HitboxConfig.triggerBotEnabled && client.hitResult instanceof EntityHitResult hit
                && hit.getEntity() instanceof Player target && isValidTarget(client, player, target)
                && player.distanceTo(target) <= HitboxConfig.combatRange
                && player.hasLineOfSight(target)
                && canCriticalHit(player)
                && player.getAttackStrengthScale(0.5F) >= 0.99F) {
            client.gameMode.attack(player, target);
            player.swing(InteractionHand.MAIN_HAND);
        }
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
            float yawDifference = Mth.wrapDegrees(targetYaw - player.getYRot());
            float pitchDifference = Mth.wrapDegrees(targetPitch - player.getXRot());
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

        float yawDifference = Mth.wrapDegrees(targetYaw - player.getYRot());
        float pitchDifference = Mth.wrapDegrees(targetPitch - player.getXRot());

        // Exponential easing gives quick acquisition without snapping the camera.
        float speed = HitboxConfig.aimSpeed;
        float factor = 1.0F - (float) Math.exp(-speed * 0.38F);
        float yawStep = Mth.clamp(yawDifference * factor, -22.0F, 22.0F);
        float pitchStep = Mth.clamp(pitchDifference * factor, -16.0F, 16.0F);

        player.setYRot(player.getYRot() + yawStep);
        player.setXRot(Mth.clamp(player.getXRot() + pitchStep, -90.0F, 90.0F));
    }

    private static boolean canCriticalHit(Player player) {
        return !player.onGround()
                && player.fallDistance > 0.0F
                && !player.isInWater()
                && !player.isInLava()
                && !player.onClimbable()
                && !player.isPassenger()
                && !player.isSprinting()
                && !player.isFallFlying()
                && !player.getAbilities().flying;
    }
}
