package de.hglabor.snorlaxboss.command

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.context.CommandContext
import de.hglabor.snorlaxboss.SnorlaxBoss.Companion.IS_DEVELOPMENT
import de.hglabor.snorlaxboss.entity.EntityManager
import de.hglabor.snorlaxboss.entity.Snorlax
import de.hglabor.snorlaxboss.entity.player.ModifiedPlayer
import de.hglabor.snorlaxboss.extension.randomMainInvItem
import de.hglabor.snorlaxboss.particle.Attacks
import net.minecraft.particle.ParticleEffect
import net.minecraft.registry.Registries
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.util.Identifier
import net.silkmc.silk.commands.command

object CommandManager {
    fun init() {
        if (IS_DEVELOPMENT) {
            fun CommandContext<ServerCommandSource>.getAllSnorlax(): MutableList<out Snorlax> {
                return this.source.world.getEntitiesByType(EntityManager.SNORLAX) { true }
            }

            command("wave") {
                argument<Int>("radius", IntegerArgumentType.integer(1)) { radius ->
                    runs {
                        Attacks.radialWave(this.source.playerOrThrow, radius())
                    }
                }
            }

            command("attack") {
                argument("state") { state ->
                    suggestList { Snorlax.Attack.values().map { it.name } }
                    runs {
                        this.getAllSnorlax().forEach {
                            it.attack = Snorlax.Attack.valueOf(state())
                        }
                    }
                }
            }

            command("randomitem") {
                runs {
                    val player = this.source.playerOrThrow
                    val (item, slot) = player.randomMainInvItem ?: Pair(null, null)
                }
            }

            command("flat") {
                runs {
                    val player = this.source.playerOrThrow as ModifiedPlayer
                    player.setFlat(!player.isFlat())
                }
            }

            command("beam") {
                argument<String>("particle") { particle ->
                    suggestList { Registries.PARTICLE_TYPE.ids.map { it.path } }
                    runs {
                        val identifier = Identifier("minecraft", particle())
                        Attacks.hyperBeam(
                            this.source.playerOrThrow,
                            Registries.PARTICLE_TYPE.get(identifier) as ParticleEffect
                        )
                    }
                }
                runs {
                    Attacks.hyperBeam(this.source.playerOrThrow)
                }
            }
        }
    }
}
