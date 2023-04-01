package de.hglabor.snorlaxboss.particle.particles

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.particle.*
import net.minecraft.client.world.ClientWorld
import net.minecraft.particle.DefaultParticleType
import net.minecraft.util.math.MathHelper

class SleepParticle(
    clientWorld: ClientWorld,
    d: Double,
    e: Double,
    f: Double,
    g: Double,
    h: Double,
    i: Double
) :
    AbstractSlowingParticle(clientWorld, d, e, f, g, h, i) {

    init {
        this.maxAge = 20
    }

    override fun getType(): ParticleTextureSheet {
        return ParticleTextureSheet.PARTICLE_SHEET_OPAQUE
    }

    override fun move(dx: Double, dy: Double, dz: Double) {
        boundingBox = boundingBox.offset(dx, dy, dz)
        repositionFromBoundingBox()
    }

    override fun getSize(tickDelta: Float): Float {
        val f = ((age.toFloat() + tickDelta) / maxAge.toFloat()) * scale
        return scale * (1.0f - f * f * 0.5f)
    }

    public override fun getBrightness(tint: Float): Int {
        var f = (age.toFloat() + tint) / maxAge.toFloat()
        f = MathHelper.clamp(f, 0.0f, 1.0f)
        val i = super.getBrightness(tint)
        var j = i and 255
        val k = i shr 16 and 255
        j += (f * 15.0f * 16.0f).toInt()
        if (j > 240) {
            j = 240
        }
        return j or (k shl 16)
    }

    @Environment(EnvType.CLIENT)
    open class Factory(private val spriteProvider: SpriteProvider, private val scale: Float, private val maxAge: Int) :
        ParticleFactory<DefaultParticleType> {
        override fun createParticle(
            defaultParticleType: DefaultParticleType,
            clientWorld: ClientWorld,
            d: Double,
            e: Double,
            f: Double,
            g: Double,
            h: Double,
            i: Double
        ): Particle {
            val flameParticle = SleepParticle(clientWorld, d, e, f, g, h, i)
            flameParticle.setSprite(spriteProvider)
            flameParticle.scale(scale)
            flameParticle.maxAge = maxAge
            return flameParticle
        }
    }

    class FactorySmall(spriteProvider: SpriteProvider) : Factory(spriteProvider, 1f, 1 * 11)
    class FactoryMedium(spriteProvider: SpriteProvider) : Factory(spriteProvider, 2.5f, 1 * 12)
    class FactoryBig(spriteProvider: SpriteProvider) : Factory(spriteProvider,  3.5f, 1 * 13)
}
