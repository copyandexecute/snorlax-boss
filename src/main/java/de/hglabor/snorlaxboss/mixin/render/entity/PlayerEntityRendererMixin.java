package de.hglabor.snorlaxboss.mixin.render.entity;

import de.hglabor.snorlaxboss.entity.ModifiedPlayer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;

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
}
