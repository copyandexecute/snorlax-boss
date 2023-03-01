package de.hglabor.snorlaxboss.entity.player

import de.hglabor.snorlaxboss.entity.IPauseEntityMovement
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.MovementType
import net.minecraft.util.math.Vec3d
import net.silkmc.silk.commands.command
import net.silkmc.silk.core.entity.modifyVelocity
import net.silkmc.silk.core.kotlin.ticks
import net.silkmc.silk.core.task.mcCoroutineTask
import net.silkmc.silk.core.text.literal
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
        val pauseEntity = (livingEntity as IPauseEntityMovement)
        pauseEntity.pause()
        val shakyPlayer = livingEntity as? ModifiedPlayer?
        shakyPlayer?.setShaky(true)
        mcCoroutineTask(delay = 3.seconds) {
            pauseEntity.unpause()
            shakyPlayer?.setShaky(false)
        }
    }
}
