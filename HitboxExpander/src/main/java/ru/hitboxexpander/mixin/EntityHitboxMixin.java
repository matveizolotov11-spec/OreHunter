package ru.hitboxexpander.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.hitboxexpander.HitboxConfig;

@Mixin(Entity.class)
public abstract class EntityHitboxMixin {
    @Inject(method = "getBoundingBox", at = @At("RETURN"), cancellable = true)
    private void hitboxexpander$expandLocalTargetBox(CallbackInfoReturnable<AABB> cir) {
        if (!HitboxConfig.enabled) return;

        Minecraft mc = Minecraft.getInstance();
        Entity entity = (Entity) (Object) this;
        if (mc.player == null || entity == mc.player || entity.level() == null
                || !entity.level().isClientSide()) return;

        boolean targetPlayer = entity instanceof Player;
        boolean targetMob = entity instanceof Mob;
        if ((!targetPlayer || !HitboxConfig.applyToPlayers)
                && (!targetMob || !HitboxConfig.applyToMobs)) return;

        AABB box = cir.getReturnValue();
        if (box == null) return;

        // Expand only width along X; preserve vanilla depth (Z) and height (Y).
        double centerX = (box.minX + box.maxX) * 0.5;
        double halfWidth = (box.maxX - box.minX) * 0.5
                * HitboxConfig.multiplier * HitboxConfig.widthMultiplier;

        cir.setReturnValue(new AABB(
                centerX - halfWidth, box.minY, box.minZ,
                centerX + halfWidth, box.maxY, box.maxZ
        ));
    }
}
