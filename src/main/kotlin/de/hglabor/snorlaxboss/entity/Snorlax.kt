package de.hglabor.snorlaxboss.entity

import de.hglabor.snorlaxboss.entity.player.ModifiedPlayer
import de.hglabor.snorlaxboss.extension.*
import de.hglabor.snorlaxboss.network.NetworkManager
import de.hglabor.snorlaxboss.network.NetworkManager.BOOM_SHAKE_PACKET
import de.hglabor.snorlaxboss.particle.Attacks
import de.hglabor.snorlaxboss.particle.ParticleManager
import de.hglabor.snorlaxboss.render.camera.CameraShaker
import de.hglabor.snorlaxboss.utils.CustomHitBox
import de.hglabor.snorlaxboss.utils.UUIDWrapper
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import net.minecraft.command.argument.EntityAnchorArgumentType
import net.minecraft.entity.*
import net.minecraft.entity.ai.pathing.EntityNavigation
import net.minecraft.entity.attribute.DefaultAttributeContainer
import net.minecraft.entity.attribute.EntityAttribute
import net.minecraft.entity.attribute.EntityAttributeInstance
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.boss.BossBar
import net.minecraft.entity.boss.ServerBossBar
import net.minecraft.entity.data.DataTracker
import net.minecraft.entity.data.TrackedData
import net.minecraft.entity.mob.MobEntity
import net.minecraft.entity.mob.PathAwareEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.particle.ParticleTypes
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.util.math.Box
import net.minecraft.world.World
import net.silkmc.silk.core.entity.directionVector
import net.silkmc.silk.core.entity.modifyVelocity
import net.silkmc.silk.core.kotlin.ticks
import net.silkmc.silk.core.task.infiniteMcCoroutineTask
import net.silkmc.silk.core.task.mcCoroutineTask
import net.silkmc.silk.core.text.broadcastText
import net.silkmc.silk.core.text.literalText
import software.bernie.geckolib.animatable.GeoEntity
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.core.animation.AnimatableManager
import software.bernie.geckolib.core.animation.AnimationController
import software.bernie.geckolib.core.animation.RawAnimation
import software.bernie.geckolib.core.`object`.PlayState
import software.bernie.geckolib.util.GeckoLibUtil
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class Snorlax(entityType: EntityType<out PathAwareEntity>, world: World) : PathAwareEntity(entityType, world),
    GeoEntity {
    private val factory = GeckoLibUtil.createInstanceCache(this)
    private val bossBar = ServerBossBar(literalText("Snorlax"), BossBar.Color.BLUE, BossBar.Style.PROGRESS)

    enum class Attack(
        val animation: RawAnimation, val supplier: (Snorlax) -> Task
    ) {
        SHAKING("shaking".play(), Snorlax::ShakingTargetTask),
        IDLE("check-target".play(), Snorlax::IdleTargetTask),
        BEAM("beam".play(), Snorlax::BeamTask),
        CHECK_TARGET("check-target".loop(), Snorlax::CheckTargetTask),
        PUNCH("punch".play(), Snorlax::PunchTargetTask),
        RUN("run".loop(), Snorlax::RunToTargetTask),
        BELLY_FLOP("belly-flop".hold(), Snorlax::BellyFlopTask),
        SLEEP("sleep".once().loop("sleep-idle"), Snorlax::SleepingTask),
        JUMP("jump".hold(), Snorlax::JumpTask);
    }

    companion object {
        fun createAttributes(): DefaultAttributeContainer.Builder {
            return MobEntity.createLivingAttributes().add(EntityAttributes.GENERIC_FOLLOW_RANGE, 35.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.23000000417232513)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 3.0)
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 300.0)
                .add(EntityAttributes.GENERIC_ARMOR, 4.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, KNOCKBACK_RESISTANCE_BASE)
                .add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 1.5)
        }

        const val KNOCKBACK_RESISTANCE_BASE = 0.6000000238418579

        val STANDING_DIMENSIONS: EntityDimensions = EntityDimensions.changing(3.4f, 5.2f)
        val SLEEPING_DIMENSIONS: EntityDimensions = EntityDimensions.changing(5.2f, 1.5f)
        private val POSE_DIMENSIONS: Map<Attack, EntityDimensions> = mutableMapOf(
            Attack.SLEEP to SLEEPING_DIMENSIONS
        )

        private val ATTACK: TrackedData<Attack> = DataTracker.registerData(Snorlax::class.java, NetworkManager.ATTACK)
    }


    override fun tick() {
        super.tick()
        if (!isAiDisabled) {
            if (task?.isFinished == true) {
                attack = task!!.nextTask()
            }
        }
    }

    override fun mobTick() {
        super.mobTick()
        bossBar.percent = this.health / this.maxHealth
    }

    var customHitBox: EntityDimensions? = null
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

    override fun onTrackedDataSet(data: TrackedData<*>?) {
        super.onTrackedDataSet(data)
        if (data?.equals(ATTACK) == true) {
            //TODO ich weiß nicht ob das mal probleme macht
            //das ist dafür da dass auf dem client der task gesycned ist und die hitbox geupdatet wird nach x sekunden z.b. beim bellytask
            //attack = attack
            calculateDimensions()
        }
    }

    override fun getTarget(): LivingEntity? {
        return world.getClosestPlayer(this, 40.0)
    }

    override fun computeFallDamage(fallDistance: Float, damageMultiplier: Float): Int {
        return super.computeFallDamage(fallDistance, damageMultiplier) - 100
    }

    override fun onStartedTrackingBy(player: ServerPlayerEntity) {
        super.onStartedTrackingBy(player)
        bossBar.addPlayer(player)
    }

    override fun onStoppedTrackingBy(player: ServerPlayerEntity) {
        super.onStoppedTrackingBy(player)
        bossBar.removePlayer(player)
    }

    private val dimension: EntityDimensions
        //Parameter ist irrelevant weil ich eh was anderes use
        get() = getDimensions(EntityPose.STANDING)

    override fun createNavigation(world: World): EntityNavigation = SnorlaxNavigation(this, world)

    private val EntityAttribute.instance: EntityAttributeInstance?
        get() = attributes.getCustomInstance(this)

    sealed class Task(val name: String) {
        var jobs = mutableListOf<Job>()
        var isFinished = false
        abstract fun onEnable()
        open fun onDisable() = jobs.forEach(Job::cancel)
        open fun nextTask(): Attack = Attack.IDLE
    }

    override fun calculateBoundingBox(): Box {
        return if (attack == Attack.SLEEP) {
            dimension.getBoxAt(pos.subtract(directionVector.normalize().multiply(2.0)))
        } else if (attack == Attack.BELLY_FLOP) {
            if (customHitBox != null) {
                //TODO junge hier ist diese kack rote eyepos hitbox in der luft aber bei sleep nicht man zukunftsmax kümmer dich drum was soll die scheiße
                dimension.getBoxAt(pos.add(directionVector.normalize().multiply(2.0)))
            } else {
                dimension.getBoxAt(pos)
            }
        } else {
            dimension.getBoxAt(pos)
        }
    }

    inner class SleepingTask : Task("Sleep") {
        override fun onEnable() {
            EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE.instance?.baseValue = KNOCKBACK_RESISTANCE_BASE.times(2)
            val sleepSeconds = Random.nextLong(5, 10)
            //world.playSoundFromEntity(null,this,SLEEP)
            Attacks.sleeping(this@Snorlax, sleepSeconds)
            mcCoroutineTask(delay = sleepSeconds.seconds) { isFinished = true }
        }

        override fun onDisable() {
            super.onDisable()
            EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE.instance?.baseValue = KNOCKBACK_RESISTANCE_BASE
        }
    }

    inner class JumpTask : Task("Jump") {
        override fun onEnable() {
            //world.playSoundFromEntity(null,this,)
            modifyVelocity(0, Random.nextDouble(1.0, 2.0), 0)
            jobs += infiniteMcCoroutineTask(delay = 5.ticks) {
                val isGrounded = isOnGround
                if (isGrounded) {
                    mcCoroutineTask(delay = 2.seconds) { isFinished = isOnGround }
                    //world.playSoundFromEntity(null,this,Landing)
                    Attacks.radialWave(this@Snorlax, Random.nextInt(8, 30))
                    this.cancel()
                }
            }
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
        }
    }

    inner class CheckTargetTask : Task("CheckTarget") {
        override fun onEnable() {
            val nextInt = Random.nextInt(5, 10)

            //world.playSoundFromEntity(LEFT,RIGHT)

            mcCoroutineTask(delay = nextInt.seconds) {
                val pos = eyePos.add(0.0, 2.5, 0.0)
                (world as? ServerWorld?)?.spawnParticles(
                    ParticleManager.EXCLAMATION_MARK,
                    pos.x,
                    pos.y,
                    pos.z,
                    0,
                    0.0,
                    0.0,
                    0.0,
                    0.0
                )
                //world.playSoundFromEntity(null,this,blockPos,EXCLAMATIONMARK)
                isFinished = true
            }
        }
    }

    inner class IdleTargetTask : Task("Idle") {
        override fun onEnable() {
            lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, target!!.pos)
        }
    }

    inner class ShakingTargetTask : Task("Shaking") {
        private val shakeDuration = 2.seconds
        private var pausePlayer: IPauseEntityMovement? = null
        private var modifiedPlayer: ModifiedPlayer? = null
        override fun onEnable() {
            if (target != null) {
                pausePlayer = target as? IPauseEntityMovement?
                modifiedPlayer = target as? ModifiedPlayer?
                val player = target as? PlayerEntity?

                val direction = directionVector
                val eyePos = eyePos.add(direction.multiply(2.0))
                target?.teleport(eyePos.x, eyePos.y - 1.9, eyePos.z)
                pausePlayer?.pause()

                mcCoroutineTask(delay = 13.ticks) {
                    modifiedPlayer?.setShaky(true)
                    //world.playSound(null,target!!.blockPos,SHAKING)

                    jobs += infiniteMcCoroutineTask(period = 1.ticks) {
                        (player as? ServerPlayerEntity?)?.apply {
                            BOOM_SHAKE_PACKET.send(CameraShaker.BoomShake(.30, .0, .5), this)
                        }
                    }

                    jobs += infiniteMcCoroutineTask(period = 7.ticks) {
                        repeat(Random.nextInt(1, 3)) {
                            val (item, slot) = player?.randomMainInvItem ?: return@repeat
                            mcCoroutineTask(delay = it.ticks) {
                                world?.playSound(
                                    null,
                                    player.blockPos,
                                    SoundEvents.ITEM_BUNDLE_DROP_CONTENTS,
                                    SoundCategory.NEUTRAL,
                                    1f,
                                    1f
                                )
                                player.inventory.main[slot] = ItemStack.EMPTY
                                player.dropItem(item, true, false)
                            }
                        }
                    }
                }

                mcCoroutineTask(delay = shakeDuration) { isFinished = true }
            }
        }

        override fun onDisable() {
            super.onDisable()
            pausePlayer?.unpause()
            modifiedPlayer?.setShaky(false)
        }
    }

    inner class PunchTargetTask : Task("Punch") {
        override fun onEnable() {
            val radius = 15.0
            mcCoroutineTask(delay = 5.ticks) {
                world.getOtherEntities(this@Snorlax, Box.of(pos, radius, radius, radius)).filter(::canSee)
                    .filterIsInstance<LivingEntity>().forEach(::attack)
            }
            mcCoroutineTask(delay = 20.ticks) {
                isFinished = true
            }
        }

        private fun attack(entity: LivingEntity) {
            this@Snorlax.tryAttack(entity)
            val player = entity as? PlayerEntity ?: return
            val item = if (player.isUsingItem) player.activeItem else ItemStack.EMPTY
            if (item.isOf(Items.SHIELD)) {
                player.itemCooldownManager.set(Items.SHIELD, 100)
                world.sendEntityStatus(player, EntityStatuses.BREAK_SHIELD)
            }
        }
    }

    inner class BeamTask : Task("Beam") {
        private val prepareTime = 1.seconds.plus(36.milliseconds)
        private var isPreparing = true
        override fun onEnable() {
            lookAtEntity(target, 90f, 90f)

            //world.playSoundFromEntity(null,this)

            jobs += infiniteMcCoroutineTask {
                if (isPreparing) {
                    lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, target!!.pos)
                    val pos = eyePos.add(directionVector.normalize().multiply(3.0))

                    (world as? ServerWorld?)?.spawnParticles(
                        ParticleTypes.SONIC_BOOM,
                        pos.x,
                        pos.y,
                        pos.z,
                        1,
                        0.0,
                        0.0,
                        0.0,
                        0.0
                    )
                }
            }

            mcCoroutineTask(delay = prepareTime) {
                isPreparing = false
                lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, target!!.pos)
                Attacks.hyperBeam(this@Snorlax, Random.nextLong(50, 150))
            }

            mcCoroutineTask(delay = Random.nextInt(5, 10).seconds) {
                isFinished = true
            }
        }
    }

    inner class BellyFlopTask : Task("BellyFlop") {
        override fun onEnable() {
            val from = pos
            val to = target!!.pos //TODO nullcheck
            val direction = to.subtract(from)
            modifyVelocity(direction.normalize().multiply(1.2, 0.0, 1.2))
            modifyVelocity(0, Random.nextDouble(1.0, 1.5), 0)
            //world.playSoundFromEntity(null,this)

            mcCoroutineTask(delay = 560.milliseconds) {
                NetworkManager.SET_CUSTOM_HIT_BOX_PACKET.sendToAll(CustomHitBox(uuid, SLEEPING_DIMENSIONS))
            }

            jobs += infiniteMcCoroutineTask(delay = 5.ticks) {
                val isGrounded = isOnGround

                if (isGrounded) {
                    mcCoroutineTask(delay = 1.seconds) { isFinished = isOnGround }
                    world.getEntitiesByClass(PlayerEntity::class.java, Box.of(pos, 14.0, 5.0, 14.0)) { true }
                        .forEach { player ->
                            world.playSound(
                                null,
                                player.blockPos,
                                SoundEvents.ENTITY_PUFFER_FISH_BLOW_OUT,
                                SoundCategory.PLAYERS
                            )
                            (player as ModifiedPlayer).setFlat(true)
                            mcCoroutineTask(delay = Random.nextInt(5, 10).seconds) {
                                (player as ModifiedPlayer).setFlat(false)
                                world.playSound(
                                    null,
                                    player.blockPos,
                                    SoundEvents.ENTITY_PUFFER_FISH_BLOW_UP,
                                    SoundCategory.PLAYERS
                                )
                            }
                        }
                    this.cancel()
                }
            }
        }

        override fun onDisable() {
            super.onDisable()
            NetworkManager.REMOVE_CUSTOM_HIT_BOX_PACKET.sendToAll(UUIDWrapper(uuid))
        }
    }

    //TODO Sleeping hitbox
    override fun getDimensions(pose: EntityPose): EntityDimensions {
        return customHitBox ?: POSE_DIMENSIONS.getOrDefault(attack, STANDING_DIMENSIONS)
    }

    override fun registerControllers(controller: AnimatableManager.ControllerRegistrar) {
        controller.add(AnimationController(this, "controller", 0) {
            it.controller.setAnimation(attack.animation)
            return@AnimationController PlayState.CONTINUE
        })
    }

    override fun getAnimatableInstanceCache(): AnimatableInstanceCache = factory
}
