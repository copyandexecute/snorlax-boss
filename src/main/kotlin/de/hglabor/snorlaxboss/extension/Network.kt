package de.hglabor.snorlaxboss.extension

import de.hglabor.snorlaxboss.entity.Snorlax
import net.minecraft.entity.data.TrackedDataHandler
import net.minecraft.entity.data.TrackedDataHandlerRegistry

object Network {
    val ATTACK = TrackedDataHandler.ofEnum(Snorlax.Attack::class.java)

    fun init() {
        TrackedDataHandlerRegistry.register(ATTACK)
    }
}