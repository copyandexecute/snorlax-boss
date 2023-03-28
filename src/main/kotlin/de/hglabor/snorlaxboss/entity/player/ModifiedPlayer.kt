package de.hglabor.snorlaxboss.entity.player

interface ModifiedPlayer {
    fun isFlat(): Boolean
    fun setFlat(flag: Boolean)
    fun setShaky(flag: Boolean)
    fun isShaky(): Boolean
    fun setFlatJumps(amount: Int)
    fun getFlatJumps(): Int
    fun setNormalReach(value: Float)
    fun getNormalReach(): Float
}
