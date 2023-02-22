package de.hglabor.snorlaxboss

import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.context.CommandContext
import de.hglabor.snorlaxboss.entity.ModifiedPlayer
import de.hglabor.snorlaxboss.entity.Snorlax
import de.hglabor.snorlaxboss.extension.toId
import de.hglabor.snorlaxboss.particles.ParticleManager
import de.hglabor.snorlaxboss.particles.RadialWave
import de.hglabor.snorlaxboss.render.SnorlaxRenderer
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry
import net.fabricmc.fabric.api.`object`.builder.v1.entity.FabricDefaultAttributeRegistry
import net.fabricmc.fabric.api.`object`.builder.v1.entity.FabricEntityTypeBuilder
import net.minecraft.client.render.entity.EntityRendererFactory
import net.minecraft.entity.EntityDimensions
import net.minecraft.entity.SpawnGroup
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import net.silkmc.silk.commands.command
import net.silkmc.silk.core.task.infiniteMcCoroutineTask


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

        fun CommandContext<ServerCommandSource>.getAllSnorlax(): MutableList<out Snorlax> {
            return this.source.world.getEntitiesByType(SNORLAX) { true }
        }

        command("wave") {
            argument<Int>("radius", IntegerArgumentType.integer(1)) { radius ->
                runs {
                    RadialWave.radialWave(this.source.playerOrThrow, radius())
                }
            }
        }

        command("beam") {
            argument<Double>("radius", DoubleArgumentType.doubleArg(0.0)) { radius ->
                argument<Int>("length", IntegerArgumentType.integer(1)) { length ->
                    runs {
                        RadialWave.beam(this.source.playerOrThrow, radius(), length())
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

        command("sleeping") {
            runs {
                RadialWave.spawnSleepingParticles(this.source.playerOrThrow)
            }
        }

        command("beam2") {
            runs {
                RadialWave.beam2(this.source.playerOrThrow)
            }
        }

        command("snorlax") {
            literal("run") {
                runs {
                    this.getAllSnorlax().forEach {
                        it.currentTask = it.RunToTargetTask()
                    }
                }
            }
            literal("jump") {
                runs {
                    this.getAllSnorlax().forEach {
                        it.currentTask = it.JumpToPositionTask()
                    }
                }
            }
            literal("sleep") {
                runs {
                    this.getAllSnorlax().forEach {
                        it.currentTask = it.SleepingTask()
                    }
                }
            }
        }
    }
}

