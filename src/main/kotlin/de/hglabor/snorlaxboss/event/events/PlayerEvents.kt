package de.hglabor.snorlaxboss.event.events

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.server.network.ServerPlayerEntity
import net.silkmc.silk.core.annotations.ExperimentalSilkApi
import net.silkmc.silk.core.event.Event

object PlayerEvents {
    open class PlayerEvent<T : PlayerEntity>(val player: T)

    @OptIn(ExperimentalSilkApi::class)
    val jump = Event.onlySyncImmutable<PlayerEvent<ServerPlayerEntity>>()
}
