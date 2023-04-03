package de.hglabor.snorlaxboss.mixin.render.entity;

import de.hglabor.snorlaxboss.entity.BiggerFallingBlock;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.FallingBlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.FallingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FallingBlockEntityRenderer.class)
public abstract class FallingBlockEntityRendererMixin extends EntityRenderer<FallingBlockEntity> {
    protected FallingBlockEntityRendererMixin(EntityRendererFactory.Context ctx) {
        super(ctx);
    }

    @Inject(method = "render(Lnet/minecraft/entity/FallingBlockEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;translate(DDD)V"))
    private void renderInjection(FallingBlockEntity fallingBlockEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
        float scale = 1.0F + 0.15F * ((BiggerFallingBlock) fallingBlockEntity).getScaleSize();
        matrixStack.scale(scale, scale, scale);
    }
}
