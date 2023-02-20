package de.hglabor.snorlaxboss.extension

import net.minecraft.util.Identifier

fun String.toId(): Identifier = Identifier("snorlaxboss", this)
