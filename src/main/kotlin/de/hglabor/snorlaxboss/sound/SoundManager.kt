package de.hglabor.snorlaxboss.sound

import de.hglabor.snorlaxboss.extension.toId
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.sound.SoundEvent

object SoundManager {
    var JUMP = SoundEvent.of("jump".toId()).register()
    var SLEEPING = SoundEvent.of("sleeping".toId()).register()
    var HYPERBEAM = SoundEvent.of("hyperbeam".toId()).register()
    var LANDING = SoundEvent.of("landing".toId()).register()
    var SHAKING = SoundEvent.of("shaking".toId()).register()
    var FOOT_STEP = SoundEvent.of("foot_step".toId()).register()
    var EXCLAMATION_MARK = SoundEvent.of("exclamation_mark".toId()).register()
    var SEARCHING_LEFT = SoundEvent.of("searching_left".toId()).register()
    var INHALE = SoundEvent.of("inhale".toId()).register()

    fun init() {
    }

    private fun SoundEvent.register(): SoundEvent = Registry.register(Registries.SOUND_EVENT, this.id, this)
}
