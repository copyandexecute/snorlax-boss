package de.hglabor.snorlaxboss

import de.hglabor.snorlaxboss.command.CommandManager
import de.hglabor.snorlaxboss.entity.EntityManager
import de.hglabor.snorlaxboss.entity.player.ModifiedPlayerManager
import de.hglabor.snorlaxboss.item.ItemManager
import de.hglabor.snorlaxboss.network.NetworkManager
import de.hglabor.snorlaxboss.sound.SoundManager
import net.fabricmc.api.ModInitializer
import net.fabricmc.loader.api.FabricLoader

class SnorlaxBoss : ModInitializer {
    companion object {
        val IS_DEVELOPMENT = FabricLoader.getInstance().isDevelopmentEnvironment
    }

    override fun onInitialize() {
        NetworkManager.init()
        ModifiedPlayerManager.init()
        SoundManager.init()
        ItemManager.init()
        EntityManager.init()
        CommandManager.init()
    }
}

