package de.hglabor.snorlaxboss.particles

import kotlinx.coroutines.cancel
import net.minecraft.particle.ParticleEffect
import net.minecraft.particle.ParticleTypes
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import net.silkmc.silk.core.task.infiniteMcCoroutineTask
import net.silkmc.silk.core.text.broadcastText
import java.lang.Math.cos
import java.lang.Math.sin

object RadialWave {
    fun radialWave(world: ServerWorld, pos: Vec3d) {
        var t = Math.PI / 4
        world.addParticle()
        world.addParticle(ParticleTypes.HEART,pos.x,pos.z+2,pos.y,0.0,0.0,0.0)
        infiniteMcCoroutineTask {
            t += 0.1 * Math.PI
            var theta = 0.0
            while (theta <= 2 * Math.PI) {
                var x: Double = t * cos(theta)
                var y: Double = 2 * Math.exp(-0.1 * t) * sin(t) + 1.5
                var z: Double = t * sin(theta)
                pos.add(x, y, z).apply {
                    world.addParticle(ParticleTypes.NOTE, x, y, z, 0.0, 0.0, 0.0)
                }
                theta += Math.PI / 64
                x = t * cos(theta)
                y = 2 * Math.exp(-0.1 * t) * sin(t) + 1.5
                z = t * sin(theta)
                pos.add(x, y, z).apply {
                    world.addParticle(ParticleTypes.NOTE, x, y, z, 0.0, 0.0, 0.0)
                }
                theta += Math.PI / 32
            }

            if (t > 20) {
                this.cancel()
            }
        }
    }
}