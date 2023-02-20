package de.hglabor.snorlaxboss

import com.mojang.brigadier.context.CommandContext
import de.hglabor.snorlaxboss.entity.Snorlax
import de.hglabor.snorlaxboss.extension.toId
import de.hglabor.snorlaxboss.render.SnorlaxRenderer
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

        fun CommandContext<ServerCommandSource>.getAllSnorlax(): MutableList<out Snorlax> {
            return this.source.world.getEntitiesByType(SNORLAX) { true }
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
        }
    }
}

