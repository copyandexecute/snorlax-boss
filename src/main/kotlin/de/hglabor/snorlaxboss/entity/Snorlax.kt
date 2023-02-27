package de.hglabor.snorlaxboss.entity

import de.hglabor.snorlaxboss.extension.Network
import de.hglabor.snorlaxboss.extension.hold
import de.hglabor.snorlaxboss.extension.loop
import de.hglabor.snorlaxboss.extension.play
import de.hglabor.snorlaxboss.particles.Attacks
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import net.minecraft.entity.EntityDimensions
import net.minecraft.entity.EntityPose
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
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.math.Box
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
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Supplier
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

class Snorlax(entityType: EntityType<out PathAwareEntity>, world: World) : PathAwareEntity(entityType, world),
    GeoEntity {
    private val factory = GeckoLibUtil.createInstanceCache(this)

    enum class Attack(
        val animation: RawAnimation,
        val supplier: (Snorlax) -> Task
    ) {
        SHAKING("shaking".play(), Snorlax::ShakingTargetTask),
        IDLE("idle".play(), Snorlax::IdleTargetTask),
        BEAM("beam".play(), Snorlax::BeamTask),
        CHECK_TARGET("check-target".loop(), Snorlax::CheckTargetTask),
        PUNCH("punch".play(), Snorlax::PunchTargetTask),
        RUN("run".loop(), Snorlax::RunToTargetTask),
        BELLY_FLOP("belly-flop".hold(), Snorlax::BellyFlopTask),
        SLEEP(
            RawAnimation.begin()
                .then("animation.hglabor.sleep", Animation.LoopType.PLAY_ONCE)
                .thenLoop("animation.hglabor.sleep-idle"), Snorlax::SleepingTask
        ),
        JUMP("jump".hold(), Snorlax::JumpToPositionTask);
    }

    companion object {
        fun createAttributes(): DefaultAttributeContainer.Builder {
            return MobEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 35.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.23000000417232513)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 3.0).add(EntityAttributes.GENERIC_ARMOR, 2.0)
        }

        val STANDING_DIMENSIONS: EntityDimensions = EntityDimensions.changing(2f, 2f)
        private val POSE_DIMENSIONS: Map<EntityPose, EntityDimensions> = mutableMapOf(
            EntityPose.SLEEPING to EntityDimensions.changing(3f, 1f)
        )

        private val ATTACK: TrackedData<Attack> = DataTracker.registerData(Snorlax::class.java, Network.ATTACK)
    }

    override fun tick() {
        super.tick()
        /*if (task?.isFinished == true) {
            task?.onDisable()
            //currentTask = currentTask?.nextTask()
            //currentTask?.onEnable()
        }*/
    }

    var task: Task? = null
    var attack: Attack
        get() = this.dataTracker.get(ATTACK)
        set(value) {
            this.dataTracker.set(ATTACK, value)
            task?.onDisable()
            world.server?.broadcastText("Disabling ${task?.name}")
            task = value.supplier.invoke(this)
            task?.onEnable()
            world.server?.broadcastText("Enabling ${task?.name}")
        }

    override fun initDataTracker() {
        super.initDataTracker()
        dataTracker.startTracking(ATTACK, Attack.IDLE)
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
            val sleepSeconds = Random.nextLong(5, 10)
            Attacks.sleeping(this@Snorlax, sleepSeconds)
            mcCoroutineTask(delay = sleepSeconds.seconds) {
                isFinished = true
            }
        }

        override fun nextTask(): Task {
            return RunToTargetTask()
        }
    }

    inner class JumpToPositionTask : Task("Jump") {
        override fun onEnable() {
            modifyVelocity(0, Random.nextDouble(1.0, 2.0), 0)
            jobs += infiniteMcCoroutineTask(delay = 5.ticks) {
                val isGrounded = isOnGround
                if (isGrounded) {
                    mcCoroutineTask(delay = 2.seconds) { isFinished = isOnGround }
                    Attacks.radialWave(this@Snorlax, Random.nextInt(8, 30))
                    this.cancel()
                }
            }
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
            return BellyFlopTask()
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

    inner class IdleTargetTask : Task("Idle") {
        override fun onEnable() {
        }

        override fun nextTask(): Task {
            return IdleTargetTask()
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
            lookAtEntity(target, 90f, 90f)
            Attacks.hyperBeam(this@Snorlax, Random.nextLong(50, 150))
            mcCoroutineTask(delay = Random.nextInt(5, 10).seconds) {
                isFinished = true
            }
        }

        override fun nextTask(): Task {
            return RunToTargetTask()
        }
    }

    inner class BellyFlopTask : Task("BellyFlop") {
        override fun onEnable() {
            val from = pos
            val to = target!!.pos //TODO nullcheck
            val direction = to.subtract(from)
            modifyVelocity(direction.normalize().multiply(1.2, 0.0, 1.2))
            modifyVelocity(0, Random.nextDouble(1.0, 1.5), 0)
            jobs += infiniteMcCoroutineTask(delay = 5.ticks) {
                val isGrounded = isOnGround

                if (isGrounded) {
                    mcCoroutineTask(delay = 1.seconds) { isFinished = isOnGround }
                    world.getEntitiesByClass(PlayerEntity::class.java, Box.of(pos, 14.0, 5.0, 14.0)) { true }
                        .forEach { player ->
                            (player as ModifiedPlayer).setFlat(true)
                            mcCoroutineTask(delay = Random.nextInt(5, 10).seconds) {
                                (player as ModifiedPlayer).setFlat(false)
                            }
                        }
                    this.cancel()
                }
            }
        }

        override fun nextTask(): Task {
            return RunToTargetTask()
        }
    }

    //TODO Sleeping hitbox
    override fun getDimensions(pose: EntityPose): EntityDimensions {
        return POSE_DIMENSIONS.getOrDefault(pose, STANDING_DIMENSIONS)
    }

    override fun registerControllers(controller: AnimatableManager.ControllerRegistrar) {
        controller.add(
            AnimationController(this, "controller", 0) {
                it.controller.setAnimation(attack.animation)
                return@AnimationController PlayState.CONTINUE
            }
        )
    }

    override fun getAnimatableInstanceCache(): AnimatableInstanceCache = factory
}
