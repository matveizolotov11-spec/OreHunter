package ru.shadowhitbox.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.shadowhitbox.HitboxConfig;

@Mixin(Entity.class)
public abstract class EntityHitboxMixin {
    @Inject(method = "getBoundingBox", at = @At("RETURN"), cancellable = true)
    private void shadowhitbox$expandPlayerBox(CallbackInfoReturnable<AABB> cir) {
        Entity entity = (Entity) (Object) this;
        Minecraft minecraft = Minecraft.getInstance();

        if (!HitboxConfig.expandEnabled || minecraft.player == null
                || !(entity instanceof Player) || entity == minecraft.player
                || !entity.level().isClientSide()) {
            return;
        }

        AABB original = cir.getReturnValue();
        double centerX = (original.minX + original.maxX) * 0.5;
        double centerZ = (original.minZ + original.maxZ) * 0.5;
        double halfWidth = HitboxConfig.width * 0.5;
        double newHeight = HitboxConfig.height;

        cir.setReturnValue(new AABB(
                centerX - halfWidth, original.minY, centerZ - halfWidth,
                centerX + halfWidth, original.minY + newHeight, centerZ + halfWidth
        ));
    }
}
