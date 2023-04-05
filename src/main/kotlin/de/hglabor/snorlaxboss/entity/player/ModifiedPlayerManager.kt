package de.hglabor.snorlaxboss.entity.player

import de.hglabor.snorlaxboss.entity.Snorlax
import de.hglabor.snorlaxboss.event.events.PlayerEvents
import net.minecraft.client.MinecraftClient
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Text
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

    fun PlayerEntity.handleSnorlaxDismount() {
        if (vehicle is Snorlax) {
            sendMessage(Text.of("Relaxo hält dich fest"), true)
        } else {
            stopRiding()
        }
    }

    fun PlayerEntity.tryWakeUp() {
        val dummy = this as ModifiedPlayer
        sleepTicks = maxSleepTicks.coerceAtMost(sleepTicks + 5)
    }

    fun handleNormalReachDistance(default: Float): Float {
        val player = MinecraftClient.getInstance().player as? ModifiedPlayer ?: return default
        return player.getNormalReach()
    }
}
