package de.hglabor.snorlaxboss.particles

import de.hglabor.snorlaxboss.extension.toId
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes
import net.minecraft.particle.DefaultParticleType
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry


object ParticleManager {
    val SLEEP: DefaultParticleType = FabricParticleTypes.simple()
    val SLEEP_MIDDLE: DefaultParticleType = FabricParticleTypes.simple()
    val SLEEP_BIG: DefaultParticleType = FabricParticleTypes.simple()
    fun init() {
        Registry.register(Registries.PARTICLE_TYPE, "sleep".toId(), SLEEP)
        Registry.register(Registries.PARTICLE_TYPE, "sleep_middle".toId(), SLEEP_MIDDLE)
        Registry.register(Registries.PARTICLE_TYPE, "sleep_big".toId(), SLEEP_BIG)

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
