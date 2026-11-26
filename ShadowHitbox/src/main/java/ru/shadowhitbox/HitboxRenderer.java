package ru.shadowhitbox;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
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
        PoseStack.Pose pose = matrices.last();
        Vec3 camera = minecraft.gameRenderer.getMainCamera().position();
        VertexConsumer lines = context.consumers().getBuffer(RenderTypes.LINES);

        for (Player player : minecraft.level.players()) {
            if (!player.isAlive() || (player == minecraft.player && !HitboxConfig.showSelf)) continue;

            AABB box = player.getBoundingBox();
            if (HitboxConfig.expandEnabled && player != minecraft.player) {
                double cx = (box.minX + box.maxX) * 0.5;
                double cz = (box.minZ + box.maxZ) * 0.5;
                double half = HitboxConfig.width * 0.5;
                box = new AABB(cx - half, box.minY, cz - half,
                        cx + half, box.minY + HitboxConfig.height, cz + half);
            }

            AABB b = box.move(-camera.x, -camera.y, -camera.z);
            drawBox(lines, pose, b, HitboxConfig.red, HitboxConfig.green, HitboxConfig.blue, HitboxConfig.alpha);
        }
    }

    private static void drawBox(VertexConsumer out, PoseStack.Pose pose, AABB b,
                                float red, float green, float blue, float alpha) {
        double x0=b.minX, y0=b.minY, z0=b.minZ, x1=b.maxX, y1=b.maxY, z1=b.maxZ;
        line(out, pose, x0,y0,z0, x1,y0,z0, red,green,blue,alpha);
        line(out, pose, x1,y0,z0, x1,y0,z1, red,green,blue,alpha);
        line(out, pose, x1,y0,z1, x0,y0,z1, red,green,blue,alpha);
        line(out, pose, x0,y0,z1, x0,y0,z0, red,green,blue,alpha);
        line(out, pose, x0,y1,z0, x1,y1,z0, red,green,blue,alpha);
        line(out, pose, x1,y1,z0, x1,y1,z1, red,green,blue,alpha);
        line(out, pose, x1,y1,z1, x0,y1,z1, red,green,blue,alpha);
        line(out, pose, x0,y1,z1, x0,y1,z0, red,green,blue,alpha);
        line(out, pose, x0,y0,z0, x0,y1,z0, red,green,blue,alpha);
        line(out, pose, x1,y0,z0, x1,y1,z0, red,green,blue,alpha);
        line(out, pose, x1,y0,z1, x1,y1,z1, red,green,blue,alpha);
        line(out, pose, x0,y0,z1, x0,y1,z1, red,green,blue,alpha);
    }

    private static void line(VertexConsumer out, PoseStack.Pose pose,
                             double x0, double y0, double z0, double x1, double y1, double z1,
                             float red, float green, float blue, float alpha) {
        out.addVertex(pose, (float)x0, (float)y0, (float)z0).setColor(red, green, blue, alpha);
        out.addVertex(pose, (float)x1, (float)y1, (float)z1).setColor(red, green, blue, alpha);
    }
}
