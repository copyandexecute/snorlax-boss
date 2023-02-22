package de.hglabor.snorlaxboss.render

import de.hglabor.snorlaxboss.entity.Snorlax
import de.hglabor.snorlaxboss.extension.toId
import de.hglabor.snorlaxboss.render.model.SnorlaxModel
import net.minecraft.client.render.*
import net.minecraft.client.render.entity.EntityRendererFactory
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.LivingEntity
import net.minecraft.util.Identifier
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.RotationAxis
import net.minecraft.util.math.Vec3d
import org.joml.Matrix3f
import org.joml.Matrix4f
import software.bernie.geckolib.cache.`object`.BakedGeoModel
import software.bernie.geckolib.renderer.GeoEntityRenderer

class SnorlaxRenderer(renderManager: EntityRendererFactory.Context) :
    GeoEntityRenderer<Snorlax>(renderManager, SnorlaxModel()) {

    companion object {
        private val EXPLOSION_BEAM_TEXTURE = "textures/beam.png".toId()
        private val LAYER: RenderLayer = RenderLayer.getEntityCutoutNoCull(EXPLOSION_BEAM_TEXTURE);
    }

    override fun preRender(
        matrixStack: MatrixStack,
        snorlax: Snorlax,
        model: BakedGeoModel,
        vertexConsumerProvider: VertexConsumerProvider?,
        consumer: VertexConsumer?,
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
        matrixStack.scale(scale, scale, scale)
        super.preRender(
            matrixStack,
            snorlax,
            model,
            vertexConsumerProvider,
            consumer,
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

    override fun postRender(
        matrixStack: MatrixStack,
        snorlax: Snorlax,
        model: BakedGeoModel?,
        vertexConsumerProvider: VertexConsumerProvider,
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
       /* val livingEntity: LivingEntity? = snorlax.target
        if (livingEntity != null) {
            //val h: Float = snorlax.getBeamProgress(partialTick)
            val h: Float = 2f
            val j: Float = snorlax.world.time.toFloat() + partialTick
            val k = j * 0.5f % 1.0f
            val l: Float = snorlax.standingEyeHeight - 0.4f
            matrixStack.push()
            matrixStack.translate(0.0f, l, 0.0f)
            val vec3d: Vec3d = this.fromLerpedPosition(livingEntity, livingEntity.height.toDouble() * 0.5, partialTick)
            val vec3d2: Vec3d = this.fromLerpedPosition(snorlax, l.toDouble(), partialTick)
            var vec3d3 = vec3d.subtract(vec3d2)
            val m = (vec3d3.length() + 1.0).toFloat()
            vec3d3 = vec3d3.normalize()
            val n = Math.acos(vec3d3.y).toFloat()
            val o = Math.atan2(vec3d3.z, vec3d3.x).toFloat()
            matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((1.5707964f - o) * 57.295776f))
            matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(n * 57.295776f))
            val q = j * 0.05f * -1.5f
            val r = h * h
            val s = 64 + (r * 191.0f).toInt()
            val t = 32 + (r * 191.0f).toInt()
            val u = 128 - (r * 64.0f).toInt()
            val v = 0.2f
            val w = 0.282f
            val x = MathHelper.cos(q + 2.3561945f) * 0.282f
            val y = MathHelper.sin(q + 2.3561945f) * 0.282f
            val z = MathHelper.cos(q + 0.7853982f) * 0.282f
            val aa = MathHelper.sin(q + 0.7853982f) * 0.282f
            val ab = MathHelper.cos(q + 3.926991f) * 0.282f
            val ac = MathHelper.sin(q + 3.926991f) * 0.282f
            val ad = MathHelper.cos(q + 5.4977875f) * 0.282f
            val ae = MathHelper.sin(q + 5.4977875f) * 0.282f
            val af = MathHelper.cos(q + 3.1415927f) * 0.2f
            val ag = MathHelper.sin(q + 3.1415927f) * 0.2f
            val ah = MathHelper.cos(q + 0.0f) * 0.2f
            val ai = MathHelper.sin(q + 0.0f) * 0.2f
            val aj = MathHelper.cos(q + 1.5707964f) * 0.2f
            val ak = MathHelper.sin(q + 1.5707964f) * 0.2f
            val al = MathHelper.cos(q + 4.712389f) * 0.2f
            val am = MathHelper.sin(q + 4.712389f) * 0.2f
            val ao = 0.0f
            val ap = 0.4999f
            val aq = -1.0f + k
            val ar = m * 2.5f + aq
            val vertexConsumer: VertexConsumer = vertexConsumerProvider.getBuffer(LAYER)
            val entry: MatrixStack.Entry = matrixStack.peek()
            val matrix4f = entry.positionMatrix
            val matrix3f = entry.normalMatrix
            vertex(vertexConsumer, matrix4f, matrix3f, af, m, ag, s, t, u, 0.4999f, ar)
            vertex(vertexConsumer, matrix4f, matrix3f, af, 0.0f, ag, s, t, u, 0.4999f, aq)
            vertex(vertexConsumer, matrix4f, matrix3f, ah, 0.0f, ai, s, t, u, 0.0f, aq)
            vertex(vertexConsumer, matrix4f, matrix3f, ah, m, ai, s, t, u, 0.0f, ar)
            vertex(vertexConsumer, matrix4f, matrix3f, aj, m, ak, s, t, u, 0.4999f, ar)
            vertex(vertexConsumer, matrix4f, matrix3f, aj, 0.0f, ak, s, t, u, 0.4999f, aq)
            vertex(vertexConsumer, matrix4f, matrix3f, al, 0.0f, am, s, t, u, 0.0f, aq)
            vertex(vertexConsumer, matrix4f, matrix3f, al, m, am, s, t, u, 0.0f, ar)
            var `as` = 0.0f
            if (snorlax.age % 2 == 0) {
                `as` = 0.5f
            }
            vertex(vertexConsumer, matrix4f, matrix3f, x, m, y, s, t, u, 0.5f, `as` + 0.5f)
            vertex(vertexConsumer, matrix4f, matrix3f, z, m, aa, s, t, u, 1.0f, `as` + 0.5f)
            vertex(vertexConsumer, matrix4f, matrix3f, ad, m, ae, s, t, u, 1.0f, `as`)
            vertex(vertexConsumer, matrix4f, matrix3f, ab, m, ac, s, t, u, 0.5f, `as`)
            matrixStack.pop()
        } */

        super.postRender(
            matrixStack,
            snorlax,
            model,
            vertexConsumerProvider,
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

    private fun fromLerpedPosition(entity: LivingEntity, yOffset: Double, delta: Float): Vec3d {
        val d = MathHelper.lerp(delta.toDouble(), entity.lastRenderX, entity.x)
        val e = MathHelper.lerp(delta.toDouble(), entity.lastRenderY, entity.y) + yOffset
        val f = MathHelper.lerp(delta.toDouble(), entity.lastRenderZ, entity.z)
        return Vec3d(d, e, f)
    }

    private fun vertex(
        vertexConsumer: VertexConsumer,
        positionMatrix: Matrix4f,
        normalMatrix: Matrix3f,
        x: Float,
        y: Float,
        z: Float,
        red: Int,
        green: Int,
        blue: Int,
        u: Float,
        v: Float
    ) {
        vertexConsumer.vertex(positionMatrix, x, y, z).color(red, green, blue, 255).texture(u, v)
            .overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
            .normal(normalMatrix, 0.0f, 1.0f, 0.0f).next()
    }
}
