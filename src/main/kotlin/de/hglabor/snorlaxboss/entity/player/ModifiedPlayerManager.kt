package de.hglabor.snorlaxboss.entity.player

import de.hglabor.snorlaxboss.event.events.PlayerEvents
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
                it.player.world.playSound(
                    null,
                    it.player.blockPos,
                    SoundEvents.ENTITY_PUFFER_FISH_BLOW_UP,
                    SoundCategory.PLAYERS
                )
            }
        }
    }
}
