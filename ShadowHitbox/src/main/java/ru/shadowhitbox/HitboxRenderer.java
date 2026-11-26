package ru.shadowhitbox;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class HitboxRenderer {
    private HitboxRenderer() {}

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(HitboxRenderer::render);
    }

    private static void render(WorldRenderContext context) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!HitboxConfig.espEnabled || minecraft.player == null || minecraft.level == null
                || context.matrices() == null || context.consumers() == null) return;

        PoseStack matrices = context.matrices();
        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
        matrices.pushPose();
        VertexConsumer lines = context.consumers().getBuffer(RenderType.lines());

        for (Player player : minecraft.level.players()) {
            if (!player.isAlive() || (player == minecraft.player && !HitboxConfig.showSelf)) continue;

            AABB box = player.getBoundingBox();
            if (HitboxConfig.expandEnabled && minecraft.hasSingleplayerServer() && player != minecraft.player) {
                double cx = (box.minX + box.maxX) * 0.5;
                double cz = (box.minZ + box.maxZ) * 0.5;
                double half = HitboxConfig.width * 0.5;
                box = new AABB(cx - half, box.minY, cz - half,
                        cx + half, box.minY + HitboxConfig.height, cz + half);
            }

            AABB relative = box.move(-camera.x, -camera.y, -camera.z);
            LevelRenderer.renderLineBox(matrices, lines, relative,
                    HitboxConfig.red, HitboxConfig.green, HitboxConfig.blue, HitboxConfig.alpha);
        }

        matrices.popPose();
    }
}
