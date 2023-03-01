package de.hglabor.snorlaxboss.mixin.render.entity;

import de.hglabor.snorlaxboss.entity.player.ModifiedPlayer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin extends LivingEntityRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
    public PlayerEntityRendererMixin(EntityRendererFactory.Context ctx, PlayerEntityModel<AbstractClientPlayerEntity> model, float shadowRadius) {
        super(ctx, model, shadowRadius);
    }

    @Override
    public void scale(AbstractClientPlayerEntity entity, MatrixStack matrices, float amount) {
        float g = 0.9375F;
        if (!((ModifiedPlayer) entity).isFlat()) {
            matrices.scale(g, g, g);
        } else {
            matrices.scale(g * 2, 0.1f, g * 2);
        }
    }

    @Inject(method = "setupTransforms(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/client/util/math/MatrixStack;FFF)V", at = @At("HEAD"))
    private void setupTransformsInjection(AbstractClientPlayerEntity entity, MatrixStack matrices, float animationProgress, float bodyYaw, float tickDelta, CallbackInfo ci) {
        if (((ModifiedPlayer) entity).isShaky()) {
            matrices.translate(0.0, Math.sin(tickDelta) / 2, 0.0);
        }
    }
}
