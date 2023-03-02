package de.hglabor.snorlaxboss.particle

import de.hglabor.snorlaxboss.extension.toId
import de.hglabor.snorlaxboss.particle.particles.ExclamationMarkParticle
import de.hglabor.snorlaxboss.particle.particles.SleepParticle
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes
import net.minecraft.particle.DefaultParticleType
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry


object ParticleManager {
    val SLEEP: DefaultParticleType = FabricParticleTypes.simple()
    val SLEEP_MIDDLE: DefaultParticleType = FabricParticleTypes.simple()
    val SLEEP_BIG: DefaultParticleType = FabricParticleTypes.simple()
    val EXCLAMATION_MARK: DefaultParticleType = FabricParticleTypes.simple()

    fun init() {
        Registry.register(Registries.PARTICLE_TYPE, "sleep".toId(), SLEEP)
        Registry.register(Registries.PARTICLE_TYPE, "sleep_middle".toId(), SLEEP_MIDDLE)
        Registry.register(Registries.PARTICLE_TYPE, "sleep_big".toId(), SLEEP_BIG)
        Registry.register(Registries.PARTICLE_TYPE, "exclamation_mark".toId(), EXCLAMATION_MARK)

        ParticleFactoryRegistry.getInstance()
            .register(EXCLAMATION_MARK, ParticleFactoryRegistry.PendingParticleFactory {
                ExclamationMarkParticle.Factory(it, 5f)
            })

        ParticleFactoryRegistry.getInstance().register(SLEEP, ParticleFactoryRegistry.PendingParticleFactory {
            SleepParticle.Factory(it, 1f, 1 * 11)
        })

        ParticleFactoryRegistry.getInstance().register(SLEEP_MIDDLE, ParticleFactoryRegistry.PendingParticleFactory {
            SleepParticle.Factory(it, 2.5f, 1 * 12)
        })

        ParticleFactoryRegistry.getInstance().register(SLEEP_BIG, ParticleFactoryRegistry.PendingParticleFactory {
            SleepParticle.Factory(it, 3.5f, 1 * 13)
        })
    }
}
