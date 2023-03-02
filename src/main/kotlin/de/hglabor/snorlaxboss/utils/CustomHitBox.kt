package de.hglabor.snorlaxboss.utils

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.minecraft.entity.EntityDimensions
import java.util.*

@Serializable
data class CustomHitBox(
    @Serializable(with = UUIDSerializer::class) val entityId: UUID,
    val width: Float,
    val height: Float,
    val fixed: Boolean,
) {
    constructor(entityId: UUID, dimensions: EntityDimensions) : this(
        entityId,
        dimensions.width,
        dimensions.height,
        dimensions.fixed
    )

    @Transient
    val dimension: EntityDimensions = EntityDimensions(width, height, fixed)
}
