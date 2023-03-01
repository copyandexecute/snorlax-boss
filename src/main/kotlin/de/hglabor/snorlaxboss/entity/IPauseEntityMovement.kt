package de.hglabor.snorlaxboss.entity

interface IPauseEntityMovement {
    fun pause()
    fun unpause()
    fun isPaused(): Boolean
}
