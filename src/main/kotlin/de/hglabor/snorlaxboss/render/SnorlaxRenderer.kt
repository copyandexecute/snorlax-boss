package de.hglabor.snorlaxboss.render

import de.hglabor.snorlaxboss.entity.Snorlax
import de.hglabor.snorlaxboss.render.model.SnorlaxModel
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.entity.EntityRendererFactory
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.util.math.RotationAxis
import software.bernie.geckolib.cache.`object`.GeoBone
import software.bernie.geckolib.renderer.GeoEntityRenderer
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer

class SnorlaxRenderer(renderManager: EntityRendererFactory.Context) :
    GeoEntityRenderer<Snorlax>(renderManager, SnorlaxModel()) {

    init {
        withScale(3.5f)
        addRenderLayer(SnorlaxItemAndBlockRenderer(this))
    }

    private class SnorlaxItemAndBlockRenderer(renderer: SnorlaxRenderer) : BlockAndItemGeoLayer<Snorlax>(renderer) {
        override fun getStackForBone(bone: GeoBone, animatable: Snorlax): ItemStack? {
            return when (bone.name.lowercase()) {
                "left_fingers" -> animatable.mainHandStack
                else -> null
            }
        }

        override fun renderStackForBone(
            poseStack: MatrixStack,
            bone: GeoBone,
            stack: ItemStack,
            animatable: Snorlax,
            bufferSource: VertexConsumerProvider,
            partialTick: Float,
            packedLight: Int,
            packedOverlay: Int
        ) {
            if (stack.isFood) {
                val scale = 0.5f
                poseStack.scale(scale, scale, scale)
                poseStack.translate(0f, -0.1f, 0f)
                poseStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90f))
            }
            super.renderStackForBone(
                poseStack,
                bone,
                stack,
                animatable,
                bufferSource,
                partialTick,
                packedLight,
                packedOverlay
            )
        }
    }
}
