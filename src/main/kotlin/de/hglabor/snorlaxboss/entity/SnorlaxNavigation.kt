package de.hglabor.snorlaxboss.entity

import net.minecraft.entity.ai.pathing.MobNavigation
import net.minecraft.entity.mob.MobEntity
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World

class SnorlaxNavigation(entity: MobEntity, world: World) : MobNavigation(entity, world) {
    override fun canPathDirectlyThrough(origin: Vec3d?, target: Vec3d?): Boolean {
        return true
    }
}
