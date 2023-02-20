package de.hglabor.snorlaxboss.entity

import net.minecraft.entity.EntityType
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.ai.pathing.EntityNavigation
import net.minecraft.entity.attribute.DefaultAttributeContainer
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.data.DataTracker
import net.minecraft.entity.data.TrackedData
import net.minecraft.entity.data.TrackedDataHandlerRegistry
import net.minecraft.entity.mob.MobEntity
import net.minecraft.entity.mob.PathAwareEntity
import net.minecraft.world.World
import net.silkmc.silk.core.entity.modifyVelocity
import software.bernie.geckolib.animatable.GeoEntity
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.core.animation.AnimatableManager
import software.bernie.geckolib.core.animation.AnimationController
import software.bernie.geckolib.core.animation.RawAnimation
import software.bernie.geckolib.core.`object`.PlayState
import software.bernie.geckolib.util.GeckoLibUtil

class Snorlax(entityType: EntityType<out PathAwareEntity>, world: World) : PathAwareEntity(entityType, world),
    GeoEntity {
    private val factory = GeckoLibUtil.createInstanceCache(this)
    var currentTask: Task? = RunToTargetTask()
        set(value) {
            this.dataTracker.set(ANIMATION_STATE, value?.getName() ?: "Idle")
            field = value
        }

    companion object {
        fun createAttributes(): DefaultAttributeContainer.Builder {
            return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 35.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.23000000417232513)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 3.0).add(EntityAttributes.GENERIC_ARMOR, 2.0)
        }

        private val RUN = RawAnimation.begin().thenLoop("animation.hglabor.run")
        private val JUMP = RawAnimation.begin().thenPlayAndHold("animation.hglabor.jump")

        private val ANIMATION_STATE: TrackedData<String> = DataTracker.registerData(Snorlax::class.java, TrackedDataHandlerRegistry.STRING)
    }

    override fun tick() {
        super.tick()
        currentTask?.tick()
    }

    override fun initDataTracker() {
        super.initDataTracker()
        dataTracker.startTracking(ANIMATION_STATE, "Idle")
    }

    fun getAnimationState(): String {
        return this.dataTracker.get(ANIMATION_STATE)
    }

    override fun getTarget(): LivingEntity? {
        return world.getClosestPlayer(this, 40.0)
    }

    override fun computeFallDamage(fallDistance: Float, damageMultiplier: Float): Int {
        return super.computeFallDamage(fallDistance, damageMultiplier) - 100
    }

    override fun createNavigation(world: World): EntityNavigation = SnorlaxNavigation(this, world)

    interface Task {
        fun tick()
        fun getAnimation(): RawAnimation
        fun getName(): String
    }

    inner class JumpToPositionTask : Task {
        init {
            modifyVelocity(0, 2.5, 0)

        }

        override fun tick() {
        }

        override fun getAnimation(): RawAnimation = JUMP
        override fun getName(): String = "Jump"
    }

    inner class RunToTargetTask : Task {
        override fun tick() {
            if (target == null) {
                handleUnknownTarget()
            } else {
                if (pos.distanceTo(target!!.pos) >= 8f) {
                    if (navigation.isIdle) {
                        navigation.startMovingTo(target, 2.0)
                    }
                }
            }
        }

        override fun getAnimation(): RawAnimation = RUN
        override fun getName(): String = "Run"

        private fun handleUnknownTarget() {

        }
    }

    private fun yo(): RawAnimation {
        println(this.getAnimationState())
        return when(this.getAnimationState().lowercase()) {
            "jump" -> JUMP
            "run" -> RUN
            else -> RUN
        }
    }

    override fun registerControllers(controller: AnimatableManager.ControllerRegistrar) {
        controller.add(
            AnimationController(this, "controller", 0) {
                it.controller.setAnimation(yo())
                return@AnimationController PlayState.CONTINUE
            }
        )
    }

    override fun getAnimatableInstanceCache(): AnimatableInstanceCache = factory
}
