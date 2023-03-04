package de.hglabor.snorlaxboss.command

import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.context.CommandContext
import de.hglabor.snorlaxboss.SnorlaxBoss.Companion.IS_DEVELOPMENT
import de.hglabor.snorlaxboss.entity.EntityManager
import de.hglabor.snorlaxboss.entity.Snorlax
import de.hglabor.snorlaxboss.entity.player.ModifiedPlayer
import de.hglabor.snorlaxboss.extension.randomMainInvItem
import de.hglabor.snorlaxboss.particle.Attacks
import net.minecraft.server.command.ServerCommandSource
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

            command("beam") {
                argument<Double>("radius", DoubleArgumentType.doubleArg(0.0)) { radius ->
                    argument<Int>("length", IntegerArgumentType.integer(1)) { length ->
                        runs {
                            Attacks.beam(this.source.playerOrThrow, radius(), length())
                        }
                    }
                }
            }

            command("flat") {
                runs {
                    val player = this.source.playerOrThrow as ModifiedPlayer
                    player.setFlat(!player.isFlat())
                }
            }

            command("beam2") {
                runs {
                    Attacks.hyperBeam(this.source.playerOrThrow)
                }
            }
        }
    }
}
