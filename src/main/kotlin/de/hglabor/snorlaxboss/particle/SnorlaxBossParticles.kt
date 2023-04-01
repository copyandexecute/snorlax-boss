package de.hglabor.snorlaxboss.particle

import de.hglabor.snorlaxboss.extension.toId
import de.hglabor.snorlaxboss.particle.particles.ExclamationMarkParticle
import de.hglabor.snorlaxboss.particle.particles.SleepParticle
import de.hglabor.snorlaxboss.particle.particles.YawnParticle
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry.PendingParticleFactory
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes
import net.minecraft.particle.DefaultParticleType
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry


object SnorlaxBossParticles {
    private val FACTORIES = LinkedHashMap<DefaultParticleType, PendingParticleFactory<DefaultParticleType>>()

    val SLEEP = add("sleep", SleepParticle::FactorySmall)
    val SLEEP_MIDDLE = add("sleep_middle", SleepParticle::FactoryMedium)
    val SLEEP_BIG = add("sleep_big", SleepParticle::FactoryBig)
    val EXCLAMATION_MARK = add("exclamation_mark", ExclamationMarkParticle::Factory)
    val YAWN = add("yawn", YawnParticle::Factory)

    fun init() {
        val registry = ParticleFactoryRegistry.getInstance()
        FACTORIES.forEach(registry::register)
    }

    private fun add(name: String, constructor: PendingParticleFactory<DefaultParticleType>): DefaultParticleType {
        val particle = Registry.register(Registries.PARTICLE_TYPE, name.toId(), FabricParticleTypes.simple())
        FACTORIES[particle] = constructor
        return particle
    }
}
