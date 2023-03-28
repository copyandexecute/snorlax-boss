package de.hglabor.snorlaxboss.entity.player

import de.hglabor.snorlaxboss.event.events.PlayerEvents
import net.minecraft.client.MinecraftClient
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.silkmc.silk.core.annotations.ExperimentalSilkApi

object ModifiedPlayerManager {
    @OptIn(ExperimentalSilkApi::class)
    fun init() {
        PlayerEvents.jump.listen {
            val modifiedPlayer = it.player as? ModifiedPlayer? ?: return@listen
            modifiedPlayer.setFlatJumps(modifiedPlayer.getFlatJumps() + 1)
            if (modifiedPlayer.getFlatJumps() >= 5 && modifiedPlayer.isFlat()) {
                modifiedPlayer.setFlat(false)
                modifiedPlayer.setNormalReach(4.5f)
                it.player.world.playSound(
                    null,
                    it.player.blockPos,
                    SoundEvents.ENTITY_PUFFER_FISH_BLOW_UP,
                    SoundCategory.PLAYERS
                )
            }
        }
    }

    fun handleNormalReachDistance(default: Float): Float {
        val player = MinecraftClient.getInstance().player as? ModifiedPlayer ?: return default
        return player.getNormalReach()
    }
}
