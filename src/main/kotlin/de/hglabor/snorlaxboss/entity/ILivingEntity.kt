package de.hglabor.snorlaxboss.entity

interface ILivingEntity {
    fun pause()
    fun unpause()
    fun isPaused(): Boolean

    fun getMaximumNoDamageTicks(): Int
    fun setMaximumNoDamageTicks(ticks: Int)
}
