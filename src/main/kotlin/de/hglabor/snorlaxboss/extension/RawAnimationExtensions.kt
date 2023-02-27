package de.hglabor.snorlaxboss.extension

import software.bernie.geckolib.core.animation.RawAnimation

fun String.toAnimation(): RawAnimation = RawAnimation.begin()
fun String.loop(): RawAnimation = this.toAnimation().thenLoop("animation.hglabor.${this}")
fun String.play(): RawAnimation = this.toAnimation().thenPlay("animation.hglabor.${this}")
fun String.hold(): RawAnimation = this.toAnimation().thenPlayAndHold("animation.hglabor.${this}")