package de.hglabor.snorlaxboss.entity.player

import de.hglabor.snorlaxboss.entity.ILivingEntity
import net.minecraft.entity.LivingEntity
import net.silkmc.silk.commands.command
import net.silkmc.silk.core.task.mcCoroutineTask
import kotlin.time.Duration.Companion.seconds

object ModifiedPlayerManager {
    fun init() {
        command("tickmovement") {
            runs {
                handlePause(this.source.playerOrThrow)
            }
        }
    }

    fun handlePause(livingEntity: LivingEntity) {
        val pauseEntity = (livingEntity as ILivingEntity)
        pauseEntity.pause()
        val shakyPlayer = livingEntity as? ModifiedPlayer?
        shakyPlayer?.setShaky(true)
        mcCoroutineTask(delay = 3.seconds) {
            pauseEntity.unpause()
            shakyPlayer?.setShaky(false)
        }
    }
}
