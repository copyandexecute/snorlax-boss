package de.hglabor.snorlaxboss.network

import de.hglabor.snorlaxboss.entity.Snorlax
import de.hglabor.snorlaxboss.extension.toId
import de.hglabor.snorlaxboss.mixin.accessor.WorldAccessor
import de.hglabor.snorlaxboss.render.camera.CameraShaker
import de.hglabor.snorlaxboss.utils.CustomHitBox
import de.hglabor.snorlaxboss.utils.UUIDSerializer
import de.hglabor.snorlaxboss.utils.UUIDWrapper
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import net.minecraft.entity.data.TrackedDataHandler
import net.minecraft.entity.data.TrackedDataHandlerRegistry
import net.silkmc.silk.network.packet.ServerToClientPacketDefinition
import net.silkmc.silk.network.packet.s2cPacket
import java.util.UUID

@OptIn(ExperimentalSerializationApi::class)
object NetworkManager {
    val ATTACK: TrackedDataHandler<Snorlax.Attack> = TrackedDataHandler.ofEnum(Snorlax.Attack::class.java)
    val BOOM_SHAKE_PACKET: ServerToClientPacketDefinition<CameraShaker.BoomShake> = s2cPacket("boom_shake".toId())
    val SET_CUSTOM_HIT_BOX_PACKET = s2cPacket<CustomHitBox>("set_custom_hitbox".toId())
    val REMOVE_CUSTOM_HIT_BOX_PACKET = s2cPacket<UUIDWrapper>("remove_custom_hitbox".toId())

    fun init() {
        TrackedDataHandlerRegistry.register(ATTACK)

        BOOM_SHAKE_PACKET.receiveOnClient { packet, _ ->
            CameraShaker.addEvent(packet)
        }

        SET_CUSTOM_HIT_BOX_PACKET.receiveOnClient { packet, context ->
            val snorlax =
                (context.client.world as? WorldAccessor)?.invokeGetEntityLookup()?.get(packet.entityId) as? Snorlax?
                    ?: return@receiveOnClient

            snorlax.customHitBox = packet.dimension
        }

        REMOVE_CUSTOM_HIT_BOX_PACKET.receiveOnClient { packet, context ->
            val snorlax = (context.client.world as? WorldAccessor)?.invokeGetEntityLookup()?.get(packet.uuid) as? Snorlax?
                ?: return@receiveOnClient
            snorlax.customHitBox = null
        }
    }
}
