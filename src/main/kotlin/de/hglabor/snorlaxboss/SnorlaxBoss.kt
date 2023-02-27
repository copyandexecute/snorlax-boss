package de.hglabor.snorlaxboss

import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.context.CommandContext
import de.hglabor.snorlaxboss.entity.ModifiedPlayer
import de.hglabor.snorlaxboss.entity.Snorlax
import de.hglabor.snorlaxboss.extension.Network
import de.hglabor.snorlaxboss.extension.toId
import de.hglabor.snorlaxboss.particles.ParticleManager
import de.hglabor.snorlaxboss.particles.Attacks
import de.hglabor.snorlaxboss.render.SnorlaxRenderer
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry
import net.fabricmc.fabric.api.`object`.builder.v1.entity.FabricDefaultAttributeRegistry
import net.fabricmc.fabric.api.`object`.builder.v1.entity.FabricEntityTypeBuilder
import net.minecraft.entity.EntityDimensions
import net.minecraft.entity.SpawnGroup
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import net.silkmc.silk.commands.command


class SnorlaxBoss : ModInitializer {
    companion object {
        val SNORLAX = Registry.register(
            Registries.ENTITY_TYPE,
            "snorlax".toId(),
            FabricEntityTypeBuilder
                .create(SpawnGroup.CREATURE) { type, world -> Snorlax(type, world) }
                .dimensions(EntityDimensions.changing(2f, 2f)).build()
        )
    }

    override fun onInitialize() {
        FabricDefaultAttributeRegistry.register(SNORLAX, Snorlax.createAttributes())
        EntityRendererRegistry.register(SNORLAX) { SnorlaxRenderer(it) }

        ParticleManager.init()
        Network.init()

        fun CommandContext<ServerCommandSource>.getAllSnorlax(): MutableList<out Snorlax> {
            return this.source.world.getEntitiesByType(SNORLAX) { true }
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
                this.source.sendMessage(Text.of("${player.isFlat()}"))
            }
        }

        command("beam2") {
            runs {
                Attacks.hyperBeam(this.source.playerOrThrow)
            }
        }
    }
}

