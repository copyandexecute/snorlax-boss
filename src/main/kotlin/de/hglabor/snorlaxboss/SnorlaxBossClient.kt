package de.hglabor.snorlaxboss

import de.hglabor.snorlaxboss.particle.SnorlaxBossParticles
import net.fabricmc.api.ClientModInitializer
import net.silkmc.silk.core.logging.logger

class SnorlaxBossClient : ClientModInitializer {
    override fun onInitializeClient() {
        logger().info("Intializing Snorlax Boss Client...")
        SnorlaxBossParticles.init()
    }
}
