package de.hglabor.snorlaxboss.extension

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket
import net.minecraft.particle.ParticleEffect
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvent
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World

fun <T : ParticleEffect?> ServerWorld.spawnParticlesForcefully(
    particle: T,
    x: Double,
    y: Double,
    z: Double,
    count: Int,
    deltaX: Double,
    deltaY: Double,
    deltaZ: Double,
    speed: Double
): Int {
    val particleS2CPacket = ParticleS2CPacket(
        particle,
        false,
        x,
        y,
        z,
        deltaX.toFloat(),
        deltaY.toFloat(),
        deltaZ.toFloat(),
        speed.toFloat(),
        count
    )
    var i = 0
    for (j in this.players.indices) {
        val serverPlayerEntity = this.players[j] as ServerPlayerEntity
        if (this.sendToPlayerIfNearby(serverPlayerEntity, true, x, y, z, particleS2CPacket)) {
            ++i
        }
    }
    return i
}

fun ServerWorld.playRelativeSound(
    pos: Vec3d,
    sound: RegistryEntry<SoundEvent>,
    category: SoundCategory,
    volume: Float,
    pitch: Float,
    seed: Long
) {
    val soundDistance = (sound.value() as SoundEvent).getDistanceToTravel(volume).toDouble()
    this.getNearbyPlayers(null, pos.x, pos.y, pos.z, soundDistance, this.registryKey).forEach {
        it.networkHandler.sendPacket(
            PlaySoundS2CPacket(
                sound,
                category,
                pos.x,
                pos.y,
                pos.z,
                Math.max(
                    0f,
                    1f - (soundDistance.toFloat() / MathHelper.sqrt(
                        it.squaredDistanceTo(pos.x, pos.y, pos.z).toFloat()
                    ))
                ),
                pitch,
                seed
            )
        )
    }
}

fun ServerWorld.getNearbyPlayers(
    player: PlayerEntity?,
    x: Double,
    y: Double,
    z: Double,
    distance: Double,
    worldKey: RegistryKey<World?>
): MutableList<ServerPlayerEntity> {
    val players = mutableListOf<ServerPlayerEntity>()
    for (i in this.players.indices) {
        val serverPlayerEntity = this.players[i] as ServerPlayerEntity
        if (serverPlayerEntity !== player && serverPlayerEntity.world.registryKey === worldKey) {
            val d = x - serverPlayerEntity.x
            val e = y - serverPlayerEntity.y
            val f = z - serverPlayerEntity.z
            if (d * d + e * e + f * f < distance * distance) {
                players.add(serverPlayerEntity)
            }
        }
    }
    return players
}
