package de.hglabor.snorlaxboss.extension

import software.bernie.geckolib.core.animation.Animation
import software.bernie.geckolib.core.animation.RawAnimation

fun String.toAnimation(): RawAnimation = RawAnimation.begin()
fun String.loop(): RawAnimation = this.toAnimation().thenLoop("animation.hglabor.${this}")
fun String.play(): RawAnimation = this.toAnimation().thenPlay("animation.hglabor.${this}")
fun String.hold(): RawAnimation = this.toAnimation().thenPlayAndHold("animation.hglabor.${this}")
fun String.once(): RawAnimation = this.toAnimation().then("animation.hglabor.${this}", Animation.LoopType.PLAY_ONCE)
fun RawAnimation.loop(name: String): RawAnimation = this.thenLoop("animation.hglabor.${name}")