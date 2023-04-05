package de.hglabor.snorlaxboss.render

import de.hglabor.snorlaxboss.entity.Snorlax
import de.hglabor.snorlaxboss.render.model.SnorlaxModel
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.entity.EntityRendererFactory
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.item.BlockItem
import net.minecraft.item.ItemStack
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

    override fun getDeathMaxRotation(animatable: Snorlax) = 0f

    private class SnorlaxItemAndBlockRenderer(renderer: SnorlaxRenderer) : BlockAndItemGeoLayer<Snorlax>(renderer) {
        override fun getStackForBone(bone: GeoBone, animatable: Snorlax): ItemStack? {
            //TODO tbh kinda scuffed aber it does its job wa
            val mainHandStack = animatable.mainHandStack
            if (animatable.attack == Snorlax.Attack.PICKUP_AND_THROW_BLOCK && bone.name.equals("between_arm", true)) {
                return mainHandStack
            }

            return when (bone.name.lowercase()) {
                "left_fingers" -> if (mainHandStack.isFood) animatable.mainHandStack else null
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
            } else if (stack.item is BlockItem) {
                val scale = 1f
                poseStack.scale(scale, scale, scale)
                poseStack.translate(-0f, -0.3f, 0f)
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
