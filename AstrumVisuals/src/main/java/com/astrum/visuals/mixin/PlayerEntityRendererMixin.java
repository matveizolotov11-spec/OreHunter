package com.astrum.visuals.mixin;

import com.astrum.visuals.AstrumVisualsClient;
import com.astrum.visuals.util.MaceDetector;
import com.astrum.visuals.util.MaceDetector.MaceType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {
    @Unique
    private AbstractClientPlayerEntity astrum$getPlayer(PlayerEntityRenderState state) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return null;
        }

        if (client.world.getEntityById(state.id) instanceof AbstractClientPlayerEntity player) {
            return player;
        }

        return null;
    }

    @Inject(method = "renderLabelIfPresent", at = @At("TAIL"))
    private void astrum$renderIndicators(
            PlayerEntityRenderState state,
            MatrixStack matrices,
            OrderedRenderCommandQueue queue,
            CameraRenderState cameraState,
            CallbackInfo ci
    ) {
        if (!AstrumVisualsClient.canRender() || state.invisible) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        AbstractClientPlayerEntity player = astrum$getPlayer(state);
        if (player == null || player == client.player) {
            return;
        }

        float health = MaceDetector.getActualHealth(player);
        String healthText = MaceDetector.formatHealth(health);
        int healthColor = MaceDetector.getHealthColor(health);
        MaceType maceType = MaceDetector.getPlayerMace(player);

        matrices.push();
        try {
            matrices.translate(0.0, state.height + 0.28, 0.0);
            matrices.multiply(cameraState.orientation);
            matrices.scale(-0.025f, -0.025f, 0.025f);

            Text hpLabel = Text.literal(healthText);
            float hpWidth = client.textRenderer.getWidth(hpLabel);
            queue.submitText(
                    matrices,
                    -hpWidth / 2.0f,
                    0.0f,
                    hpLabel.asOrderedText(),
                    true,
                    TextRenderer.TextLayerType.NORMAL,
                    state.light,
                    healthColor,
                    0,
                    0
            );
        } finally {
            matrices.pop();
        }

        if (maceType == MaceType.NONE) {
            return;
        }

        matrices.push();
        try {
            matrices.translate(0.0, state.height + 0.83, 0.0);
            matrices.multiply(cameraState.orientation);
            matrices.scale(0.35f, 0.35f, 0.35f);

            ItemRenderState maceRenderState = new ItemRenderState();
            ItemStack maceStack = new ItemStack(Items.MACE);
            client.getItemModelManager().updateForLivingEntity(
                    maceRenderState,
                    maceStack,
                    ItemDisplayContext.GROUND,
                    player
            );
            maceRenderState.render(
                    matrices,
                    queue,
                    state.light,
                    OverlayTexture.DEFAULT_UV,
                    0
            );
        } finally {
            matrices.pop();
        }

        if (maceType == MaceType.EXPLOSIVE) {
            matrices.push();
            try {
                matrices.translate(0.0, state.height + 0.69, 0.0);
                matrices.multiply(cameraState.orientation);
                matrices.scale(-0.015f, -0.015f, 0.015f);

                Text explosiveLabel = Text.literal("[ВЗРЫВ]");
                float labelWidth = client.textRenderer.getWidth(explosiveLabel);
                queue.submitText(
                        matrices,
                        -labelWidth / 2.0f,
                        0.0f,
                        explosiveLabel.asOrderedText(),
                        true,
                        TextRenderer.TextLayerType.NORMAL,
                        state.light,
                        0xFFFF3333,
                        0,
                        0
                );
            } finally {
                matrices.pop();
            }
        }
    }
}
