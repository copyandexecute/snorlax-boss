package de.hglabor.snorlaxboss.particle.particles

import net.minecraft.client.particle.*
import net.minecraft.client.world.ClientWorld
import net.minecraft.particle.DefaultParticleType
import kotlin.random.Random

class YawnParticle(
    world: ClientWorld,
    x: Double,
    y: Double,
    z: Double,
    velX: Double,
    velY: Double,
    velZ: Double,
    sprites: SpriteProvider
) :
    SpriteBillboardParticle(world, x, y, z, velX, velY, velZ) {
    private val sprites: SpriteProvider

    init {
        this.velocityX = (random.nextDouble() * 2 - 1) / 30
        this.velocityY = 0.1 + random.nextDouble() / Random.nextInt(5,15)
        this.velocityZ = (random.nextDouble() * 2 - 1) / 30
        this.maxAge = 16 + random.nextInt(15)
        this.sprites = sprites
        this.scale(1f + Random.nextDouble(1.0, 4.0).toFloat())
        this.setSpriteForAge(sprites)
    }

    override fun tick() {
        super.tick()
        this.setSpriteForAge(sprites)
    }

    override fun getType(): ParticleTextureSheet = ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT

    data class Factory(val sprites: SpriteProvider) : ParticleFactory<DefaultParticleType> {
        override fun createParticle(
            type: DefaultParticleType,
            world: ClientWorld,
            x: Double,
            y: Double,
            z: Double,
            xSpeed: Double,
            ySpeed: Double,
            zSpeed: Double
        ): Particle {
            return YawnParticle(world, x, y, z, xSpeed, ySpeed, zSpeed, sprites)
        }
    }
}
