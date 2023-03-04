package de.hglabor.snorlaxboss.entity.damage

import net.minecraft.entity.damage.DamageSource

object DamageManager {
    val HYPERBEAM: DamageSource = DamageSource("hyperbeam").setScaledWithDifficulty()

    fun init() {

    }
}
