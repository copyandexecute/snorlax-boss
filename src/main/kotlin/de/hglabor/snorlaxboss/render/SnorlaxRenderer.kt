package de.hglabor.snorlaxboss.render

import de.hglabor.snorlaxboss.entity.Snorlax
import de.hglabor.snorlaxboss.render.model.SnorlaxModel
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.entity.EntityRendererFactory
import net.minecraft.client.util.math.MatrixStack
import software.bernie.geckolib.cache.`object`.BakedGeoModel
import software.bernie.geckolib.renderer.GeoEntityRenderer

class SnorlaxRenderer(renderManager: EntityRendererFactory.Context) :
    GeoEntityRenderer<Snorlax>(renderManager, SnorlaxModel()) {

    override fun preRender(
        poseStack: MatrixStack,
        animatable: Snorlax,
        model: BakedGeoModel,
        bufferSource: VertexConsumerProvider?,
        buffer: VertexConsumer?,
        isReRender: Boolean,
        partialTick: Float,
        packedLight: Int,
        packedOverlay: Int,
        red: Float,
        green: Float,
        blue: Float,
        alpha: Float
    ) {
        val scale = 3.5f
        poseStack.scale(scale,scale,scale)
        super.preRender(
            poseStack,
            animatable,
            model,
            bufferSource,
            buffer,
            isReRender,
            partialTick,
            packedLight,
            packedOverlay,
            red,
            green,
            blue,
            alpha
        )
    }
}
