package de.hglabor.snorlaxboss.entity.player

import net.silkmc.silk.commands.command
import net.silkmc.silk.core.text.literal

object ModifiedPlayerManager {
    var isActive = false

    fun init() {
        command("tickmovement") {
            runs {
                isActive = !isActive
                this.source.sendMessage(isActive.toString().literal)
            }
        }
    }
}