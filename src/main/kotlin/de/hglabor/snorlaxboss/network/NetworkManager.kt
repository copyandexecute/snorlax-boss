package de.hglabor.snorlaxboss.network

import de.hglabor.snorlaxboss.entity.Snorlax
import de.hglabor.snorlaxboss.extension.toId
import de.hglabor.snorlaxboss.render.camera.CameraShaker
import kotlinx.serialization.ExperimentalSerializationApi
import net.minecraft.entity.data.TrackedDataHandler
import net.minecraft.entity.data.TrackedDataHandlerRegistry
import net.silkmc.silk.network.packet.ServerToClientPacketDefinition
import net.silkmc.silk.network.packet.s2cPacket

@OptIn(ExperimentalSerializationApi::class)
object NetworkManager {
    val ATTACK: TrackedDataHandler<Snorlax.Attack> = TrackedDataHandler.ofEnum(Snorlax.Attack::class.java)
    val BOOM_SHAKE_PACKET: ServerToClientPacketDefinition<CameraShaker.BoomShake> = s2cPacket("boom_shake".toId())

    fun init() {
        TrackedDataHandlerRegistry.register(ATTACK)

        BOOM_SHAKE_PACKET.receiveOnClient { packet, _ ->
            CameraShaker.addEvent(packet)
        }
    }
}