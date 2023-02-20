package de.hglabor.snorlaxboss.entity

import kotlinx.coroutines.Job
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
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket
import net.minecraft.text.Text
import net.minecraft.world.World
import net.silkmc.silk.core.entity.modifyVelocity
import net.silkmc.silk.core.kotlin.ticks
import net.silkmc.silk.core.task.infiniteMcCoroutineTask
import net.silkmc.silk.core.task.mcCoroutineTask
import net.silkmc.silk.core.text.broadcastText
import software.bernie.geckolib.animatable.GeoEntity
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.core.animation.AnimatableManager
import software.bernie.geckolib.core.animation.Animation
import software.bernie.geckolib.core.animation.AnimationController
import software.bernie.geckolib.core.animation.RawAnimation
import software.bernie.geckolib.core.`object`.PlayState
import software.bernie.geckolib.util.GeckoLibUtil
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

class Snorlax(entityType: EntityType<out PathAwareEntity>, world: World) : PathAwareEntity(entityType, world),
    GeoEntity {
    private val factory = GeckoLibUtil.createInstanceCache(this)
    var currentTask: Task? = RunToTargetTask()
        set(value) {
            this.dataTracker.set(ANIMATION_STATE, value?.name ?: "Idle")
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
        private val CHECK_TARGET = RawAnimation.begin().thenLoop("animation.hglabor.check-target")
        private val SHAKING = RawAnimation.begin().thenPlay("animation.hglabor.shaking")
        private val PUNCH = RawAnimation.begin().thenPlay("animation.hglabor.punch")
        private val BELLY_FLOP = RawAnimation.begin().thenPlayAndHold("animation.hglabor.belly-flop")
        private val BEAM = RawAnimation.begin().thenPlay("animation.hglabor.beam")
        private val SLEEP = RawAnimation.begin()
            .then("animation.hglabor.sleep", Animation.LoopType.PLAY_ONCE)
            .thenLoop("animation.hglabor.sleep-idle")

        private val ANIMATION_STATE: TrackedData<String> =
            DataTracker.registerData(Snorlax::class.java, TrackedDataHandlerRegistry.STRING)
    }

    init {
        currentTask?.onEnable()
    }

    override fun tick() {
        super.tick()
        if (currentTask?.isFinished == true) {
            currentTask?.onDisable()
            currentTask = currentTask?.nextTask()
            currentTask?.onEnable()
        }
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

    sealed class Task(val name: String) {
        var jobs = mutableListOf<Job>()
        var isFinished = false
        abstract fun onEnable()
        open fun onDisable() {
            jobs.forEach(Job::cancel)
        }

        abstract fun nextTask(): Task
    }

    inner class SleepingTask : Task("Sleep") {
        override fun onEnable() {
            val nextInt = Random.nextInt(5, 10)
            server?.broadcastText("Sleeping for ${nextInt} seconds")
            mcCoroutineTask(delay = nextInt.seconds) {
                isFinished = true
            }
        }

        override fun onDisable() {
            super.onDisable()
            server?.broadcastText("Stopped Sleeping")
        }

        override fun nextTask(): Task {
            return RunToTargetTask()
        }
    }
    inner class JumpToPositionTask : Task("Jump") {
        override fun onEnable() {
            modifyVelocity(0, Random.nextDouble(1.0, 2.0), 0)
            jobs += infiniteMcCoroutineTask(delay = 5.ticks) {
                isFinished = isOnGround
            }
        }

        override fun onDisable() {
            super.onDisable()
            server?.broadcastText(Text.of("Stopping Jump Task"))
        }

        override fun nextTask(): Task {
            return if (Random.nextBoolean()) RunToTargetTask() else SleepingTask()
        }
    }
    inner class RunToTargetTask : Task("Run") {
        override fun onEnable() {
            jobs += infiniteMcCoroutineTask {
                if (target == null) {
                } else {
                    if (pos.distanceTo(target!!.pos) >= 8f) {
                        if (navigation.isIdle) {
                            navigation.startMovingTo(target, 2.0)
                        }
                    } else {
                        isFinished = true
                    }
                }
            }
        }

        override fun onDisable() {
            super.onDisable()
            navigation.stop()
            server?.broadcastText(Text.of("Stopping Run Task"))
        }

        override fun nextTask(): Task {
            return listOf(ShakingTargetTask(),CheckTargetTask(),PunchTargetTask(),BeamTask(),BellyFlopTask(),JumpToPositionTask()).random()
        }
    }
    inner class CheckTargetTask : Task("CheckTarget") {
        override fun onEnable() {
            val nextInt = Random.nextInt(5, 10)
            server?.broadcastText("Checking Target for ${nextInt} seconds")
            mcCoroutineTask(delay = nextInt.seconds) {
                isFinished = true
            }
        }

        override fun nextTask(): Task {
            return RunToTargetTask()
        }
    }
    inner class ShakingTargetTask : Task("Shaking") {
        override fun onEnable() {
            val nextInt = Random.nextInt(5, 10)
            server?.broadcastText("Shaking Target for ${nextInt} seconds")
            mcCoroutineTask(delay = nextInt.seconds) {
                isFinished = true
            }
        }

        override fun nextTask(): Task {
            return RunToTargetTask()
        }
    }
    inner class PunchTargetTask : Task("Punch") {
        override fun onEnable() {
            val nextInt = Random.nextInt(5, 10)
            server?.broadcastText("Punch Target for ${nextInt} seconds")
            mcCoroutineTask(delay = nextInt.seconds) {
                isFinished = true
            }
        }

        override fun nextTask(): Task {
            return RunToTargetTask()
        }
    }
    inner class BeamTask : Task("Beam") {
        override fun onEnable() {
            val nextInt = Random.nextInt(5, 10)
            server?.broadcastText("Beam for ${nextInt} seconds")
            mcCoroutineTask(delay = nextInt.seconds) {
                isFinished = true
            }
        }

        override fun nextTask(): Task {
            return RunToTargetTask()
        }
    }
    inner class BellyFlopTask : Task("BellyFlop") {
        override fun onEnable() {
            modifyVelocity(0, Random.nextDouble(0.5, 1.5), 0)
            jobs += infiniteMcCoroutineTask(delay = 5.ticks) {
                isFinished = isOnGround
            }
        }

        override fun nextTask(): Task {
            return if (Random.nextBoolean()) RunToTargetTask() else SleepingTask()
        }
    }

    private fun yo(): RawAnimation {
        return when (this.getAnimationState().lowercase()) {
            "jump" -> JUMP
            "run" -> RUN
            "sleep" -> SLEEP
            "checktarget" -> CHECK_TARGET
            "bellyflop" -> BELLY_FLOP
            "beam" -> BEAM
            "shaking" -> SHAKING
            "punch" -> PUNCH
            else -> CHECK_TARGET
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
