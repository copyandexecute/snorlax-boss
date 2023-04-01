package de.hglabor.snorlaxboss.particle.particles

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.particle.*
import net.minecraft.client.world.ClientWorld
import net.minecraft.particle.DefaultParticleType

class ExclamationMarkParticle(
    clientWorld: ClientWorld,
    d: Double,
    e: Double,
    f: Double,
    g: Double,
    h: Double,
    i: Double
) : SpriteBillboardParticle(clientWorld, d, e, f, g, h, i) {

    init {
        this.maxAge = 60
        this.scale = 5.0f
    }

    override fun move(speed: Float): Particle = this
    override fun move(dx: Double, dy: Double, dz: Double) = Unit
    override fun getType(): ParticleTextureSheet = ParticleTextureSheet.PARTICLE_SHEET_OPAQUE

    @Environment(EnvType.CLIENT)
    class Factory(private val spriteProvider: SpriteProvider) :
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
            val exclamationMarkParticle = ExclamationMarkParticle(clientWorld, d, e, f, g, h, i)
            exclamationMarkParticle.setSprite(spriteProvider)
            return exclamationMarkParticle
        }
    }
}
