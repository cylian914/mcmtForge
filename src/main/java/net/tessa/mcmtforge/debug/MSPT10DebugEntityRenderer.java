package net.tessa.mcmtforge.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class MSPT10DebugEntityRenderer extends EntityRenderer<MSPT10DebugEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("mcmtfabric", "textures/entity/mspt10_debug_entity.png");

    public MSPT10DebugEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(MSPT10DebugEntity entity, float yaw, float tickDelta, PoseStack matrices,
                       MultiBufferSource vertexConsumers, int light) {
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);

        // Draw a debug hitbox-style outline
        AABB box = entity.getBoundingBox().move(
                -entity.getX(),
                -entity.getY(),
                -entity.getZ()
        );

        // Get the vertex consumer for debug lines
        Vec3 cameraPos = this.entityRenderDispatcher.camera.getPosition();
        matrices.pushPose();
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        // Draw debug outline using the built-in debug renderer
        DebugRenderer.renderFilledBox(
                matrices,
                vertexConsumers,
                box.move(entity.getX(), entity.getY(), entity.getZ()),
                1.0f, 0.0f, 1.0f, 1.0f  // Purple color (RGBA)
        );

        matrices.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(MSPT10DebugEntity entity) {
        return TEXTURE;
    }
}