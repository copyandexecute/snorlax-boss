package de.hglabor.snorlaxboss.entity

import de.hglabor.snorlaxboss.entity.projectile.PokeBallEntity
import de.hglabor.snorlaxboss.extension.toId
import de.hglabor.snorlaxboss.render.SnorlaxRenderer
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry
import net.fabricmc.fabric.api.`object`.builder.v1.entity.FabricDefaultAttributeRegistry
import net.fabricmc.fabric.api.`object`.builder.v1.entity.FabricEntityTypeBuilder
import net.minecraft.client.render.entity.EntityRendererFactory
import net.minecraft.client.render.entity.FlyingItemEntityRenderer
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityDimensions
import net.minecraft.entity.EntityType
import net.minecraft.entity.SpawnGroup
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry


object EntityManager {
    val SNORLAX = "snorlax".register<Snorlax>(
        FabricEntityTypeBuilder
            .create(SpawnGroup.CREATURE, ::Snorlax)
            .dimensions(Snorlax.STANDING_DIMENSIONS)
            .build()
    )

    val POKEBALL = "pokeball".register<PokeBallEntity>(
        FabricEntityTypeBuilder
            .create(SpawnGroup.MISC, ::PokeBallEntity)
            .dimensions(EntityDimensions.fixed(0.25F, 0.25F))
            .trackRangeBlocks(4)
            .trackedUpdateRate(10)
            .build()
    )

    fun init() {
        FabricDefaultAttributeRegistry.register(SNORLAX, Snorlax.createAttributes())
        //Renderer
        EntityRendererRegistry.register(SNORLAX, ::SnorlaxRenderer)
        EntityRendererRegistry.register(POKEBALL, ::FlyingItemEntityRenderer)
    }

    private fun <T : Entity> String.register(entityType: EntityType<T>): EntityType<T> {
        return Registry.register(Registries.ENTITY_TYPE, this.toId(), entityType)
    }
}