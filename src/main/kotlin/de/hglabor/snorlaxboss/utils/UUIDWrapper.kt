package de.hglabor.snorlaxboss.utils

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class UUIDWrapper(@Serializable(with = UUIDSerializer::class) val uuid: UUID)
