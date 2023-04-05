package de.hglabor.snorlaxboss.entity

import de.hglabor.snorlaxboss.entity.player.ModifiedPlayer
import de.hglabor.snorlaxboss.extension.*
import de.hglabor.snorlaxboss.mixin.accessor.MoveControllAccessor
import de.hglabor.snorlaxboss.network.NetworkManager
import de.hglabor.snorlaxboss.network.NetworkManager.BOOM_SHAKE_PACKET
import de.hglabor.snorlaxboss.particle.Attacks
import de.hglabor.snorlaxboss.particle.SnorlaxBossParticles
import de.hglabor.snorlaxboss.render.camera.CameraShaker
import de.hglabor.snorlaxboss.sound.SoundManager
import de.hglabor.snorlaxboss.utils.CustomHitBox
import de.hglabor.snorlaxboss.utils.UUIDWrapper
import de.hglabor.snorlaxboss.utils.weightedCollection
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import net.minecraft.block.BlockRenderType
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.command.argument.EntityAnchorArgumentType
import net.minecraft.entity.*
import net.minecraft.entity.ai.control.MoveControl
import net.minecraft.entity.ai.pathing.EntityNavigation
import net.minecraft.entity.attribute.DefaultAttributeContainer
import net.minecraft.entity.attribute.EntityAttribute
import net.minecraft.entity.attribute.EntityAttributeInstance
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.boss.BossBar
import net.minecraft.entity.boss.ServerBossBar
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.data.DataTracker
import net.minecraft.entity.data.TrackedData
import net.minecraft.entity.data.TrackedDataHandlerRegistry
import net.minecraft.entity.mob.MobEntity
import net.minecraft.entity.mob.PathAwareEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.projectile.ProjectileEntity
import net.minecraft.item.BlockItem
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.particle.BlockStateParticleEffect
import net.minecraft.particle.ItemStackParticleEffect
import net.minecraft.particle.ParticleTypes
import net.minecraft.registry.tag.BlockTags
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvent
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Text
import net.minecraft.util.math.*
import net.minecraft.world.World
import net.silkmc.silk.core.entity.directionVector
import net.silkmc.silk.core.entity.modifyVelocity
import net.silkmc.silk.core.kotlin.ticks
import net.silkmc.silk.core.math.geometry.filledSpherePositionSet
import net.silkmc.silk.core.task.infiniteMcCoroutineTask
import net.silkmc.silk.core.task.mcCoroutineTask
import net.silkmc.silk.core.text.literal
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import software.bernie.geckolib.animatable.GeoEntity
import software.bernie.geckolib.core.animatable.GeoAnimatable
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.core.animation.AnimatableManager
import software.bernie.geckolib.core.animation.AnimationController
import software.bernie.geckolib.core.animation.AnimationState
import software.bernie.geckolib.core.animation.RawAnimation
import software.bernie.geckolib.core.`object`.PlayState
import software.bernie.geckolib.util.GeckoLibUtil
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class Snorlax(entityType: EntityType<out PathAwareEntity>, world: World) : PathAwareEntity(entityType, world),
    GeoEntity {
    private val factory = GeckoLibUtil.createInstanceCache(this)
    private val bossBar =
        ServerBossBar(Text.translatable("entity.snorlaxboss.snorlax"), BossBar.Color.BLUE, BossBar.Style.PROGRESS)
    private val HIT_DISTANCE = 6.0f
    private val RUN_DISTANCE = 3.5f
    private var fixedJumpVelocity: Vec3d = Vec3d.ZERO

    var forceAnimationReset = false

    private var isDebug: Boolean
        get() = this.dataTracker.get(IS_DEBUG)
        set(value) = this.dataTracker.set(IS_DEBUG, value)

    var isSpinning: Boolean
        get() = this.dataTracker.get(SPINNING)
        set(value) = this.dataTracker.set(SPINNING, value)

    var isRolling: Boolean
        get() = this.dataTracker.get(ROLLING)
        set(value) = this.dataTracker.set(ROLLING, value)

    var isThrowingEntity: Boolean
        get() = this.dataTracker.get(THROWING_ENTITY)
        set(value) = this.dataTracker.set(THROWING_ENTITY, value)

    var isThrowingBlock: Boolean
        get() = this.dataTracker.get(THROWING_BLOCK)
        set(value) = this.dataTracker.set(THROWING_BLOCK, value)

    var isEating: Boolean
        get() = this.dataTracker.get(EATING)
        set(value) = this.dataTracker.set(EATING, value)

    var isPickingUpFood: Boolean
        get() = this.dataTracker.get(PICKUPFOOD)
        set(value) = this.dataTracker.set(PICKUPFOOD, value)

    var isBodyChecking: Boolean
        get() = this.dataTracker.get(BODYCHECK)
        set(value) = this.dataTracker.set(BODYCHECK, value)

    var isMoving: Boolean
        get() = this.dataTracker.get(MOVING)
        set(value) {
            if (value != isMoving) this.dataTracker.set(MOVING, value)
        }

    var istAmSpringen: Boolean
        get() = this.dataTracker.get(JUMPING)
        set(value) = this.dataTracker.set(JUMPING, value)

    private var task: Task? = null
    var customHitBox: EntityDimensions? = null
    var attack: Attack
        get() = this.dataTracker.get(ATTACK)
        set(value) {
            this.dataTracker.set(ATTACK, value)
            if (!this.world.isClient()) {
                task?.onDisable()
                task = value.supplier.invoke(this)
                task?.onEnable()/*world.server?.broadcastText {
                    text(task?.name!!) {
                        color = 0x5eff00
                    }
                }*/
            }
        }

    init {
        attack = attack
        this.moveControl = SnorlaxMoveControl()
        this.stepHeight = 3.5f
    }

    enum class Attack(
        val animation: RawAnimation, val supplier: (Snorlax) -> Task
    ) {
        SHAKING("shaking".play(), Snorlax::ShakingTask),
        IDLE("idle".play(), Snorlax::IdleTargetTask),
        BEAM("beam".play(), Snorlax::BeamTask),
        CHECK_TARGET("check-target".loop(), Snorlax::CheckTargetTask),
        PUNCH("punch".hold(), Snorlax::PunchTask),
        MULTIPLE_PUNCH("punch".hold(), Snorlax::MultiplePunchTask),
        RUN("idle".loop(), Snorlax::RunTask),
        ROLL("idle".loop(), Snorlax::RollTask),
        BELLY_FLOP("belly-flop".hold(), Snorlax::BellyFlopTask),
        PICKUP_AND_THROW_PLAYER("pickup".hold(), Snorlax::PickUpAndThrowPlayer),
        PICKUP_AND_THROW_BLOCK("pickup-block".hold(), Snorlax::PickUpAndThrowBlock),

        //At this point mach ich eh alles über idle und controller sau dumm nächstes mal animationen anders machen
        YAWN("idle".loop(), Snorlax::YawnTask),
        EAT("idle".loop(), Snorlax::EatTask),
        BODYCHECK("idle".play(), Snorlax::BodyCheckTask),

        //THROW_PLAYER("throw".hold(), Snorlax::ThrowPlayerTask),
        SLEEP("sleep".once().loop("sleep-idle"), Snorlax::SleepTask),
        JUMP("jump".hold(), Snorlax::JumpTask),
        INHALE("inhale".play(), Snorlax::InhaleTask);
    }

    companion object {
        fun createAttributes(): DefaultAttributeContainer.Builder {
            return MobEntity.createLivingAttributes().add(EntityAttributes.GENERIC_FOLLOW_RANGE, 35.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.23000000417232513)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 23.0) //Warden macht 30
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 500.0) //Wie Warden
                .add(EntityAttributes.GENERIC_ARMOR, 4.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, KNOCKBACK_RESISTANCE_BASE)
                .add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 1.5)
                .add(EntityAttributes.GENERIC_ARMOR, 8.0) //Doppel wither
        }

        const val KNOCKBACK_RESISTANCE_BASE = 0.6000000238418579

        val STANDING_DIMENSIONS: EntityDimensions = EntityDimensions.changing(3.4f, 5.2f)
        val SLEEPING_DIMENSIONS: EntityDimensions = EntityDimensions.changing(5.2f, 1.5f)
        private val POSE_DIMENSIONS: Map<Attack, EntityDimensions> = mutableMapOf(
            Attack.SLEEP to SLEEPING_DIMENSIONS
        )

        private val ATTACK = DataTracker.registerData(Snorlax::class.java, NetworkManager.ATTACK)
        private val IS_DEBUG = DataTracker.registerData(Snorlax::class.java, TrackedDataHandlerRegistry.BOOLEAN)
        private val SPINNING = DataTracker.registerData(Snorlax::class.java, TrackedDataHandlerRegistry.BOOLEAN)
        private val ROLLING = DataTracker.registerData(Snorlax::class.java, TrackedDataHandlerRegistry.BOOLEAN)
        private val MOVING = DataTracker.registerData(Snorlax::class.java, TrackedDataHandlerRegistry.BOOLEAN)
        private val JUMPING = DataTracker.registerData(Snorlax::class.java, TrackedDataHandlerRegistry.BOOLEAN)
        private val THROWING_ENTITY = DataTracker.registerData(Snorlax::class.java, TrackedDataHandlerRegistry.BOOLEAN)
        private val THROWING_BLOCK = DataTracker.registerData(Snorlax::class.java, TrackedDataHandlerRegistry.BOOLEAN)
        private val EATING = DataTracker.registerData(Snorlax::class.java, TrackedDataHandlerRegistry.BOOLEAN)
        private val PICKUPFOOD = DataTracker.registerData(Snorlax::class.java, TrackedDataHandlerRegistry.BOOLEAN)
        private val BODYCHECK = DataTracker.registerData(Snorlax::class.java, TrackedDataHandlerRegistry.BOOLEAN)
    }


    override fun tick() {
        super.tick()
        task?.tick()
        if (task?.isFinished == true) {
            if (isDebug) {
                task?.onDisable()
                task = null
            } else {
                attack = task!!.nextTask()
            }
        }


        if (!world.isClient) {
            if (istAmSpringen) {
                breakBlocksWhileJumping()
            }

            if (isRolling) {
                breakBlocksWhileRolling()
                squashEntitiesWhileRolling()
                addRollParticle()
            }

            if (isSpinning) {
                changeLookDirection(130.0, 0.0)
                //setHeadYaw(headYaw+3f)
            }
        }
    }

    override fun mobTick() {
        super.mobTick()
        bossBar.percent = this.health / this.maxHealth
    }

    override fun initDataTracker() {
        super.initDataTracker()
        dataTracker.startTracking(ATTACK, Attack.IDLE)
        dataTracker.startTracking(BODYCHECK, false)
        dataTracker.startTracking(IS_DEBUG, false)
        dataTracker.startTracking(SPINNING, false)
        dataTracker.startTracking(ROLLING, false)
        dataTracker.startTracking(MOVING, false)
        dataTracker.startTracking(JUMPING, false)
        dataTracker.startTracking(THROWING_ENTITY, false)
        dataTracker.startTracking(THROWING_BLOCK, false)
        dataTracker.startTracking(EATING, false)
        dataTracker.startTracking(PICKUPFOOD, false)
    }

    override fun readCustomDataFromNbt(nbt: NbtCompound) {
        super.readCustomDataFromNbt(nbt)
        this.isDebug = nbt.getBoolean("Debug")
    }

    override fun writeCustomDataToNbt(nbt: NbtCompound) {
        super.writeCustomDataToNbt(nbt)
        if (this.isDebug) {
            nbt.putBoolean("Debug", this.isDebug)
        }
    }

    fun moveToPosition(pos: Vec3d) {
        isMoving = true
        //targetPos = pos
    }

    private fun breakBlocksWhileJumping() {
        Vec3i(eyePos.x, eyePos.y, eyePos.z).filledSpherePositionSet(5).filter { it.y > y }.forEach {
            world.breakBlock(it, false, this@Snorlax)
        }
    }

    private fun breakBlocksWhileRolling() {
        Vec3i(x, y, z).filledSpherePositionSet(7).filter { it.y > y }.forEach {
            world.breakBlock(it, false, this@Snorlax)
        }
    }

    private fun squashEntitiesWhileRolling() {
        world.getOtherEntities(this, boundingBox.expand(2.0)).filterIsInstance<PlayerEntity>().forEach {
            tryFlatPlayer(it)
        }
    }

    override fun onTrackedDataSet(data: TrackedData<*>?) {
        super.onTrackedDataSet(data)
        if (data?.equals(ATTACK) == true) {
            //TODO ich weiß nicht ob das mal probleme macht
            //das ist dafür da dass auf dem client der task gesycned ist und die hitbox geupdatet wird nach x sekunden z.b. beim bellytask
            //attack = attack
            forceAnimationReset = true
            calculateDimensions()
        }
    }

    override fun getTarget(): LivingEntity? {
        return world.getClosestPlayer(this, 80.0)
    }

    override fun isFireImmune(): Boolean = true
    override fun doesRenderOnFire(): Boolean = false
    override fun handleFallDamage(fallDistance: Float, damageMultiplier: Float, damageSource: DamageSource?) = false
    override fun createNavigation(world: World): EntityNavigation = SnorlaxNavigation(this, world)
    private val dimension: EntityDimensions get() = getDimensions(EntityPose.STANDING)
    private val EntityAttribute.instance: EntityAttributeInstance? get() = attributes.getCustomInstance(this)

    override fun onStartedTrackingBy(player: ServerPlayerEntity) {
        super.onStartedTrackingBy(player)
        bossBar.addPlayer(player)
    }

    override fun onStoppedTrackingBy(player: ServerPlayerEntity) {
        super.onStoppedTrackingBy(player)
        bossBar.removePlayer(player)
    }

    private fun attack(entity: LivingEntity) {
        this.tryAttack(entity)
        val player = entity as? PlayerEntity ?: return
        when (weightedCollection {
            80.0 to "SHIELD"
            10.0 to "ICE"
            10.0 to "FIRE"
        }.next()) {
            "SHIELD" -> if (Random.nextBoolean()) shieldBreaker(player)
            "ICE" -> icePunch(player)
            "FIRE" -> firePunch(player)
        }
    }

    private fun firePunch(player: PlayerEntity) {
        if (player.isBlocking) return
        if (!player.isFrozen) {
            player.setOnFireFor(Random.nextInt(3, 7))
        }
    }

    private fun icePunch(player: PlayerEntity) {
        if (player.isBlocking) return
        if (!player.isOnFire) {
            player.frozenTicks = Random.nextInt(3, 7) * 20
        }
    }

    private fun shieldBreaker(player: PlayerEntity) {
        val item = if (player.isUsingItem) player.activeItem else ItemStack.EMPTY
        if (item.isOf(Items.SHIELD)) {
            player.itemCooldownManager.set(Items.SHIELD, 100)
            player.clearActiveItem()
            world.sendEntityStatus(player, EntityStatuses.BREAK_SHIELD)
        }
    }

    private fun tryAttackWithShieldBreak(entity: Entity) {
        tryAttack(entity)
        if (entity is PlayerEntity) {
            shieldBreaker(entity)
        }
    }

    override fun jump() {
        super.jump()
        istAmSpringen = true
    }

    override fun onLanding() {
        super.onLanding()
        istAmSpringen = false
    }

    private inner class SnorlaxMoveControl : MoveControl(this) {
        private var stuckTicks = 0
        private var lastJumpPos: BlockPos? = null
        private var lastPos: BlockPos? = null
        var jumpTry = 0

        override fun tick() {
            val n: Float
            if (state == State.STRAFE) {
                val f = entity.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED).toFloat()
                val g = this.speed.toFloat() * f
                var h = forwardMovement
                var i = sidewaysMovement
                var j = MathHelper.sqrt(h * h + i * i)
                if (j < 1.0f) {
                    j = 1.0f
                }
                j = g / j
                h *= j
                i *= j
                val k = MathHelper.sin(entity.yaw * 0.017453292f)
                val l = MathHelper.cos(entity.yaw * 0.017453292f)
                val m = h * l - i * k
                n = i * l + h * k
                if (!(this as MoveControllAccessor).invokeIsPosWalkable(m, n)) {
                    forwardMovement = 1.0f
                    sidewaysMovement = 0.0f
                }
                entity.movementSpeed = g
                entity.setForwardSpeed(forwardMovement)
                entity.setSidewaysSpeed(sidewaysMovement)
                state = State.WAIT
            } else if (state == State.MOVE_TO) {
                state = State.WAIT
                val d = targetX - entity.x
                val e = targetZ - entity.z
                val o = targetY - entity.y
                val p = d * d + o * o + e * e
                if (p < 2.500000277905201E-7) {
                    entity.setForwardSpeed(0.0f)
                    return
                }
                n = (MathHelper.atan2(e, d) * 57.2957763671875).toFloat() - 90.0f
                entity.yaw = wrapDegrees(entity.yaw, n, 90.0f)
                entity.movementSpeed =
                    (this.speed * entity.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED)).toFloat()
                val blockPos = entity.blockPos
                val blockState = entity.world.getBlockState(blockPos)
                val voxelShape = blockState.getCollisionShape(entity.world, blockPos)
                if (stuckTicks > 15 || (o > entity.stepHeight.toDouble() && d * d + e * e < 1.0f.coerceAtLeast(entity.width)
                        .toDouble() || !voxelShape.isEmpty && entity.y < voxelShape.getMax(
                        Direction.Axis.Y
                    ) + blockPos.y.toDouble() && !blockState.isIn(BlockTags.DOORS) && !blockState.isIn(BlockTags.FENCES))
                ) {
                    if (lastJumpPos?.isWithinDistance(Vec3i(blockPos.x, blockPos.y, blockPos.z), 5.0) == true) {
                        jumpTry++
                    } else {
                        jumpTry = 0
                    }

                    stuckTicks = 0
                    lastJumpPos = blockPos
                    entity.jumpControl.setActive()
                    state = State.JUMPING
                } else {
                    if (lastPos?.isWithinDistance(blockPos, 1.0) == true) {
                        stuckTicks++
                    } else {
                        stuckTicks = 0
                    }
                }
                lastPos = blockPos
            } else if (state == State.JUMPING) {
                entity.movementSpeed =
                    (this.speed * entity.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED)).toFloat()
                if (entity.isOnGround) {
                    state = State.WAIT
                }
            } else {
                entity.setForwardSpeed(0.0f)
            }
        }
    }

    sealed class Task {
        var jobs = mutableListOf<Job>()
        var isFinished = false
        abstract fun onEnable()
        open fun onDisable() = jobs.forEach(Job::cancel)
        open fun nextTask(): Attack = Attack.IDLE
        open fun tick() {}
    }

    override fun getJumpVelocity(): Float {
        val jumpControlModifier =
            (if ((moveControl as SnorlaxMoveControl).jumpTry > 0) (moveControl as SnorlaxMoveControl).jumpTry else 1)
        val rollingModifier = (if (isRolling) 1.5f else 1.0f)
        return 0.42f * this.jumpVelocityMultiplier * jumpControlModifier * rollingModifier
    }

    private fun throwRidingEntites(callBack: (() -> Unit)?) {
        isThrowingEntity = true
        val directionVector = directionVector.normalize()
        passengersDeep.forEach {
            it.stopRiding()
            it.modifyVelocity(Vec3d(directionVector.x, Random.nextDouble(2.0, 4.0), directionVector.z))
        }
        mcCoroutineTask(delay = 1.seconds) {
            isThrowingEntity = false
            callBack?.invoke()
        }
    }

    fun onProjectileCollision(
        projectileEntity: ProjectileEntity, ci: CallbackInfo
    ) {
        if (!world.isClient) {
            if (isRolling) {
                val reflect = projectileEntity.velocity.negate()

                val x = Random.nextDouble(-1.0, 1.0)
                val y = Random.nextDouble(-1.0, 1.0)
                val z = Random.nextDouble(-1.0, 1.0)

                projectileEntity.setVelocity(this, pitch, yaw, 0.0f, 1.5f, 1.0f)
                projectileEntity.velocity = reflect.multiply(x, y, z)

                ci.cancel()
            } else if (isMoving) {
                projectileEntity.velocity = Vec3d.ZERO
                ci.cancel()
            }
        }
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

    //lecker pickup
    inner class PickUpAndThrowBlock : Task() {
        override fun onEnable() {
            pickBlock()
            mcCoroutineTask(delay = 500.milliseconds) {
                isThrowingBlock = true

                mcCoroutineTask(delay = 250.milliseconds) {
                    throwBlock()
                    playSoundAtEyePos(SoundManager.THROW)
                }

                mcCoroutineTask(delay = 500.milliseconds) {
                    isThrowingBlock = false
                    isFinished = true
                }
            }
        }

        override fun tick() {
            super.tick()
            lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, target?.pos ?: eyePos)
        }

        private fun pickBlock() {
            val blockUnder = world.getBlockState(blockPos.down())
            if (blockUnder.isAir) {
                equipStack(EquipmentSlot.MAINHAND, Items.STONE.defaultStack)
            } else {
                equipStack(EquipmentSlot.MAINHAND, blockUnder.block.asItem().defaultStack)
            }

            val size = 2
            for (x in 0 until size) {
                for (y in 0 until size) {
                    for (z in 0 until size) {
                        val pos = blockPos.down().add(x, -y, z)
                        val blockState = world.getBlockState(pos)
                        world.setBlockState(pos, Blocks.AIR.defaultState)
                        (world as? ServerWorld?)?.spawnParticles(
                            BlockStateParticleEffect(ParticleTypes.BLOCK, blockState),
                            pos.x + 0.5,
                            pos.y + 0.5,
                            pos.z + 0.5,
                            1,
                            0.0,
                            0.0,
                            0.0,
                            0.0
                        )
                        world.playSound(null, pos, blockState.soundGroup.placeSound, SoundCategory.BLOCKS)
                    }
                }
            }
        }

        private fun throwBlock() {
            //TODO was danach als schaden mäßíg?
            //TODO hitbox schaden
            val currentPos = eyePos
            val targetPos = target?.pos
            val direction = targetPos?.subtract(currentPos)

            val fallingBlock = FallingBlockEntity.spawnFromBlock(
                world,
                BlockPos(x, eyePos.y + 5, z),
                (mainHandStack.item as? BlockItem?)?.block?.defaultState ?: Blocks.DIAMOND_BLOCK.defaultState
            ) ?: return
            (fallingBlock as BiggerFallingBlock).scaleSize = 15f
            (fallingBlock as BiggerFallingBlock).shooter = this@Snorlax
            fallingBlock.dropItem = false
            fallingBlock.modifyVelocity(
                direction?.normalize()?.multiply(3.0) ?: Vec3d(
                    directionVector.x,
                    Random.nextDouble(1.0, 2.0),
                    directionVector.z
                )
            )
            equipStack(EquipmentSlot.MAINHAND, Items.AIR.defaultStack)
        }

        override fun nextTask(): Attack {
            return weightedCollection {
                70.0 to Attack.PICKUP_AND_THROW_BLOCK
                10.0 to Attack.BODYCHECK
                10.0 to Attack.ROLL
                10.0 to Attack.RUN
            }.next()
        }
    }

    //lecker pickup
    inner class PickUpAndThrowPlayer : Task() {
        override fun onEnable() {
            target?.startRiding(this@Snorlax, true)
            passengersDeep.filterIsInstance<ServerPlayerEntity>()
                .forEach { it.sendMessage("Relaxo hält dich fest".literal, true) }
            mcCoroutineTask(delay = 140.milliseconds) {
                isSpinning = true
            }
            mcCoroutineTask(delay = Random.nextInt(2, 3).seconds) {
                isSpinning = false
                throwRidingEntites {
                    isFinished = true
                }
            }
        }

        override fun nextTask(): Attack {
            return weightedCollection {
                50.0 to Attack.JUMP
                20.0 to Attack.PICKUP_AND_THROW_BLOCK
                5.0 to Attack.SLEEP
                10.0 to Attack.CHECK_TARGET
            }.next()
        }
    }

    inner class SleepTask : Task() {
        override fun onEnable() {
            EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE.instance?.baseValue = KNOCKBACK_RESISTANCE_BASE.times(2)
            sound(SoundManager.SLEEPING, 0.14f, 1f)
            Attacks.sleeping(this@Snorlax, 7)
            mcCoroutineTask(delay = 7.seconds) { isFinished = true }
        }

        override fun onDisable() {
            super.onDisable()
            EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE.instance?.baseValue = KNOCKBACK_RESISTANCE_BASE
        }

        override fun nextTask(): Attack {
            return weightedCollection {
                60.0 to Attack.CHECK_TARGET
                20.0 to Attack.YAWN
                20.0 to Attack.RUN
            }.next()
        }
    }

    fun playSoundAtEyePos(soundEvent: SoundEvent) {
        world.playSound(null, eyePos.x, eyePos.y, eyePos.z, soundEvent, SoundCategory.HOSTILE, 1f, 1f)
    }

    fun yawn() {
        val pos = eyePos.add(directionVector.normalize().subtract(0.0, 0.3, 0.0).multiply(2.0))
        val serverWorld = world as? ServerWorld?
        playSoundAtEyePos(SoundManager.YAWN)
        repeat(20) {
            serverWorld?.spawnParticles(
                SnorlaxBossParticles.YAWN,
                pos.x,
                pos.y,
                pos.z,
                1,
                0.0,
                0.0,
                0.0,
                0.0,
            )
        }
    }

    inner class YawnTask : Task() {
        override fun onEnable() {
            makePlayersSleepy()
            yawn()
            mcCoroutineTask(delay = 2.seconds) {
                isFinished = true
            }
        }

        private fun makePlayersSleepy() {
            world.getOtherEntities(this@Snorlax, boundingBox.expand(5.0)).filterIsInstance<ServerPlayerEntity>()
                .forEach {
                    it.sleepAfterDuration(Random.nextInt(1, 5).seconds, Random.nextInt(80, 160))
                }
        }

        private fun ServerPlayerEntity.sleepAfterDuration(duration: Duration, maxSleepTicks: Int) {
            val player = this as ModifiedPlayer
            this.sendMessage(Text.translatable("snorlaxboss.player.tired"))
            mcCoroutineTask(delay = duration) {
                player.maxSleepTicks = maxSleepTicks
                player.isForceSleeping = true
            }
        }

        override fun nextTask(): Attack {
            return weightedCollection {
                50.0 to Attack.RUN
                30.0 to Attack.PICKUP_AND_THROW_BLOCK
                20.0 to Attack.CHECK_TARGET
            }.next()
        }
    }

    inner class JumpTask : Task() {
        private val radius = Random.nextInt(12, 50)

        override fun onEnable() {
            sound(SoundManager.JUMP, 0.9f, 0.9f)
            modifyVelocity(0, radius / 16.0, 0)
            jobs += infiniteMcCoroutineTask(delay = 5.ticks) {
                val isGrounded = isOnGround
                if (isGrounded) {
                    mcCoroutineTask(delay = 2.seconds) { isFinished = true }

                    sound(SoundManager.LANDING, 0.2f + radius / 30f, 1f)

                    world.getOtherEntities(this@Snorlax, Box.from(pos).expand(HIT_DISTANCE.toDouble() * 2))
                        .forEach(::tryAttackWithShieldBreak)

                    Attacks.radialWave(this@Snorlax, radius)
                    this.cancel()
                }
            }
        }

        override fun nextTask(): Attack {
            return weightedCollection {
                40.0 to Attack.RUN
                30.0 to Attack.ROLL
                20.0 to Attack.BODYCHECK
                20.0 to Attack.YAWN
            }.next()
        }
    }

    override fun playStepSound(pos: BlockPos, state: BlockState) {
        playSound(SoundManager.FOOT_STEP, 1f, 0.6f)
    }

    private fun addRollParticle() {
        val serverWorld = world as? ServerWorld ?: return
        val blockState = this.steppingBlockState
        if (blockState.renderType != BlockRenderType.INVISIBLE) {
            for (i in 0..29) {
                val x = this.x + Random.nextDouble(-dimension.width.toDouble(), dimension.width.toDouble())
                val y = this.y
                val z = this.z + Random.nextDouble(-dimension.width.toDouble(), dimension.width.toDouble())
                serverWorld.spawnParticles(
                    BlockStateParticleEffect(ParticleTypes.BLOCK, blockState), x, y, z, 1, 0.0, 0.0, 0.0, 0.0
                )
            }
        }
    }

    inner class RollTask : RunTask(0.0) {
        override fun onEnable() {
            super.onEnable()
            isRolling = true
        }

        override fun tick() {
            speed = 3.5.coerceAtMost(speed + 0.1)
            super.tick()
        }

        override fun onDisable() {
            super.onDisable()
            isRolling = false
        }

        override fun nextTask(): Attack {
            return weightedCollection {
                60.0 to Attack.JUMP
                20.0 to Attack.BELLY_FLOP
                10 to Attack.CHECK_TARGET
                10 to Attack.PICKUP_AND_THROW_PLAYER
            }.next()
        }
    }

    open inner class RunTask(var speed: Double = 2.0, val distanceToReach: Double = 9.0) : Task() {
        protected var runTarget: Entity? = null
        protected var shouldMove = true

        override fun onEnable() {
            isMoving = true
            runTarget = target
        }

        override fun tick() {
            if (shouldMove) {
                if (runTarget != null) {
                    if (squaredDistanceTo(runTarget) > distanceToReach) {
                        //TODO handle water
                        moveControl.moveTo(runTarget!!.x, runTarget!!.y, runTarget!!.z, speed)
                    } else {
                        reachedSpot()
                    }
                } else {
                    isFinished = true
                }
            }
        }

        open fun reachedSpot() {
            isFinished = true
        }

        override fun onDisable() {
            super.onDisable()
            isMoving = false
            runTarget = null
            (moveControl as SnorlaxMoveControl).jumpTry = 0
        }

        override fun nextTask(): Attack {
            return weightedCollection {
                if (target == null) {
                    100.0 to Attack.CHECK_TARGET
                } else {
                    40.0 to Attack.MULTIPLE_PUNCH
                    30.0 to Attack.PUNCH
                    12.0 to Attack.JUMP
                    12.0 to Attack.PICKUP_AND_THROW_PLAYER
                    12.0 to Attack.BELLY_FLOP
                    6.0 to Attack.YAWN
                    5.0 to Attack.SHAKING
                    1.0 to Attack.BEAM
                }
            }.next()
        }
    }

    inner class CheckTargetTask : Task() {
        private val checkingDuration = Random.nextInt(1, 5)

        override fun onEnable() {
            jobs += infiniteMcCoroutineTask(period = 20.ticks) {
                sound(SoundManager.SEARCHING_LEFT, 1f, 1f)
            }

            mcCoroutineTask(delay = checkingDuration.seconds) {
                if (target != null) {
                    lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, target!!.pos)
                    val pos = eyePos.add(0.0, 2.5, 0.0)
                    (world as? ServerWorld?)?.spawnParticlesForcefully(
                        SnorlaxBossParticles.EXCLAMATION_MARK, pos.x, pos.y, pos.z, 0, 0.0, 0.0, 0.0, 0.0
                    )
                    sound(SoundManager.EXCLAMATION_MARK, 1f, 1f)
                }
                isFinished = true
            }
        }

        override fun nextTask(): Attack {
            return weightedCollection {
                if (target == null) {
                    100.0 to Attack.IDLE
                } else {
                    70.0 to Attack.RUN
                    30.0 to Attack.ROLL
                    30.0 to Attack.BODYCHECK
                    if (health <= maxHealth / 2) {
                        15.0 to Attack.SLEEP
                    } else {
                        5.0 to Attack.SLEEP
                    }
                    20.0 to Attack.BEAM
                }
            }.next()
        }
    }

    inner class IdleTargetTask : Task() {
        override fun onEnable() {
            mcCoroutineTask(delay = Random.nextInt(5, 15).seconds) {
                isFinished = true
            }
            jobs += infiniteMcCoroutineTask {
                if (attacker != null) {
                    target = attacker
                    isFinished = true
                }
            }
        }

        override fun nextTask(): Attack {
            return weightedCollection {
                if (target != null) {
                    30.0 to Attack.RUN
                    30.0 to Attack.BODYCHECK
                    30.0 to Attack.ROLL
                    5.0 to Attack.PICKUP_AND_THROW_BLOCK
                } else {
                    80.0 to Attack.CHECK_TARGET
                    10.0 to Attack.SLEEP
                }
            }.next()
        }
    }

    inner class ShakingTask : Task() {
        private val shakeDuration = 2.seconds
        private var pausePlayer: ILivingEntity? = null
        private var modifiedPlayer: ModifiedPlayer? = null

        override fun onEnable() {
            if (target != null) {
                pausePlayer = target as? ILivingEntity?
                modifiedPlayer = target as? ModifiedPlayer?
                val player = target as? PlayerEntity?

                val direction = directionVector
                val eyePos = eyePos.add(direction.multiply(2.0))
                target?.teleport(eyePos.x, eyePos.y - 1.9, eyePos.z)
                pausePlayer?.pause()

                mcCoroutineTask(delay = 13.ticks) {
                    modifiedPlayer?.setShaky(true)
                    sound(SoundManager.SHAKING, 1f, 1f)

                    jobs += infiniteMcCoroutineTask(period = 1.ticks) {
                        (player as? ServerPlayerEntity?)?.apply {
                            BOOM_SHAKE_PACKET.send(CameraShaker.BoomShake(.30, .0, .5), this)
                        }
                    }

                    jobs += infiniteMcCoroutineTask(period = 4.ticks) {
                        tryAttack(target)
                        repeat(Random.nextInt(1, 3)) {
                            mcCoroutineTask(delay = it.ticks) {
                                player?.dropRandomFood(true) {
                                    player.playSound(SoundEvents.ITEM_BUNDLE_DROP_CONTENTS)
                                }
                            }
                        }
                    }
                }

                mcCoroutineTask(delay = shakeDuration) { isFinished = true }
            } else {
                isFinished = true
            }
        }

        override fun onDisable() {
            super.onDisable()
            pausePlayer?.unpause()
            modifiedPlayer?.setShaky(false)
        }

        override fun nextTask(): Attack {
            return Attack.EAT
        }
    }

    private fun PlayerEntity.playSound(soundEvent: SoundEvent) {
        world.playSound(null, blockPos, soundEvent, SoundCategory.PLAYERS, 1f, 1f)
    }

    inner class MultiplePunchTask : Task() {
        private val radius = 15.0

        override fun onEnable() {
            val job = mcCoroutineTask(howOften = Random.nextLong(2, 5), period = 25.ticks) {
                NetworkManager.FORCE_ANIMATION_RESET.sendToAll(UUIDWrapper(uuid))
                mcCoroutineTask(delay = 5.ticks) {
                    val hitRadius = Box.of(pos, radius, radius, radius)
                    if (target == null) {
                        isFinished = true
                    } else {
                        val toHit = world.getOtherEntities(this@Snorlax, hitRadius).filter(::canSee)
                            .filterIsInstance<LivingEntity>().toMutableList()
                        if (distanceTo(target) < HIT_DISTANCE) {
                            if (toHit.none { it.uuid.equals(target?.uuid) }) {
                                toHit.add(target!!)
                            }
                        }
                        toHit.forEach(::attack)
                    }
                }
            }
            jobs += job
            jobs += infiniteMcCoroutineTask {
                if (job.isCancelled || job.isCompleted) isFinished = true
            }
        }

        override fun nextTask(): Attack {
            return weightedCollection {
                20.0 to Attack.SHAKING
                30.0 to Attack.JUMP
                25.0 to Attack.BELLY_FLOP
                if (health <= maxHealth / 2) {
                    20.0 to Attack.SLEEP
                } else {
                    10.0 to Attack.SLEEP
                }
                7.0 to Attack.BEAM
            }.next()
        }
    }

    inner class PunchTask : Task() {
        private val radius = 15.0
        private var distanceFlag = false
        override fun onEnable() {
            NetworkManager.FORCE_ANIMATION_RESET.sendToAll(UUIDWrapper(uuid))

            mcCoroutineTask(delay = 5.ticks) {
                val hitRadius = Box.of(pos, radius, radius, radius)
                if (target == null) {
                    isFinished = true
                } else {
                    if (distanceTo(target) > HIT_DISTANCE) {
                        isFinished = true
                        distanceFlag = true
                    } else {
                        val toHit = world.getOtherEntities(this@Snorlax, hitRadius).filter(::canSee)
                            .filterIsInstance<LivingEntity>().toMutableList()
                        if (toHit.none { it.uuid.equals(target?.uuid) }) {
                            toHit.add(target!!)
                        }
                        toHit.forEach(::attack)
                        mcCoroutineTask(delay = 20.ticks) { isFinished = true }
                    }
                }
            }
        }

        override fun nextTask(): Attack {
            return weightedCollection {
                if (distanceFlag) {
                    90.0 to Attack.RUN
                    10.0 to Attack.BELLY_FLOP
                } else {
                    40.0 to Attack.PUNCH
                    20.0 to Attack.RUN
                    10.0 to Attack.BELLY_FLOP
                    10.0 to Attack.JUMP
                    5.0 to Attack.SLEEP
                    5.0 to Attack.BEAM
                }
            }.next()
        }
    }

    inner class BeamTask : Task() {
        private val prepareTime = 3.seconds
        private var isPreparing = true

        override fun onEnable() {
            lookAtEntity(target, 90f, 90f)

            sound(SoundManager.HYPERBEAM, 5f, 1f)

            jobs += infiniteMcCoroutineTask {
                if (isPreparing) {
                    lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, target!!.pos)
                    val pos = eyePos.add(directionVector.normalize().multiply(3.0))

                    (world as? ServerWorld?)?.spawnParticles(
                        ParticleTypes.SONIC_BOOM, pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0
                    )
                }
            }

            mcCoroutineTask(delay = prepareTime) {
                isPreparing = false
                lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, target!!.pos)
                Attacks.hyperBeam(this@Snorlax)
            }

            mcCoroutineTask(delay = Random.nextInt(5, 10).seconds) {
                isFinished = true
            }
        }

        override fun nextTask(): Attack {
            return weightedCollection {
                48.0 to Attack.CHECK_TARGET
                48.0 to Attack.RUN
                4.0 to Attack.SLEEP
            }.next()
        }
    }

    //TODO war eig gedacht dass wenn es zu lange dauert er einen aufsaugt jo
    inner class InhaleTask : Task() {
        private val prepareTime = 1.seconds
        private var isPreparing = true

        override fun onEnable() {
            lookAtEntity(target, 90f, 90f)

            sound(SoundManager.INHALE, 5f, 1f)

            jobs += infiniteMcCoroutineTask {
                if (isPreparing) {
                    lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, target!!.pos)
                    val pos = eyePos.add(directionVector.normalize().multiply(3.0))

                    (world as? ServerWorld?)?.spawnParticles(
                        ParticleTypes.CLOUD,
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
                Attacks.inhale(this@Snorlax)
            }

            mcCoroutineTask(delay = Random.nextInt(5, 10).seconds) {
                isFinished = true
            }
        }

        override fun nextTask(): Attack {
            // Er muss schlafen, weil seine ganze Luft raus ist und keine Energie mehr hat
            return Attack.SLEEP
        }
    }

    inner class BodyCheckTask : RunTask(speed = 2.5) {
        override fun reachedSpot() {
            isMoving = false
            shouldMove = false
            isBodyChecking = true
            //Animation Delay
            mcCoroutineTask(delay = 480.milliseconds) {
                val direction = directionVector.normalize().multiply(2.0)
                world.getOtherEntities(this@Snorlax, boundingBox.expand(4.5)).forEach {
                    tryAttackWithShieldBreak(it)
                    it.modifyVelocity(Vec3d(direction.x, Random.nextDouble(1.0, 2.0), direction.z))
                }
            }
            mcCoroutineTask(delay = 1200.milliseconds) {
                isBodyChecking = false
                isFinished = true
            }
        }

        override fun nextTask(): Attack {
            return weightedCollection {
                50.0 to Attack.PICKUP_AND_THROW_BLOCK
                20.0 to Attack.INHALE
                20.0 to Attack.BODYCHECK
                10.0 to Attack.BEAM
            }.next()
        }
    }

    inner class EatTask : RunTask(distanceToReach = 6.0) {
        private var itemEntity: ItemEntity? = null
        override fun onEnable() {
            //TODO food ist in erde und er springt -> boden attacke
            //TODO generell wenns zu lange dauert und ers nid findet
            val items =
                world.getOtherEntities(this@Snorlax, boundingBox.expand(15.0)) { it is ItemEntity && it.stack.isFood }
            isMoving = true
            itemEntity = items.randomOrNull() as? ItemEntity?
            runTarget = itemEntity
        }

        override fun reachedSpot() {
            isMoving = false
            shouldMove = false
            isPickingUpFood = true
            playSound(SoundEvents.ENTITY_ITEM_PICKUP, 1f, 1f)
            val itemStack = itemEntity?.stack
            equipStack(EquipmentSlot.MAINHAND, itemStack)
            itemEntity?.discard()
            mcCoroutineTask(delay = 360.milliseconds) {
                isPickingUpFood = false
                isEating = true

                jobs += infiniteMcCoroutineTask(period = 5.ticks) {
                    playSound(
                        itemStack?.eatSound,
                        0.5f + 0.5f * random.nextInt(2).toFloat(),
                        (random.nextFloat() - random.nextFloat()) * 0.2f + 1.0f
                    )
                    spawnItemParticles(itemStack, 16)
                }

                mcCoroutineTask(delay = 1.seconds) {
                    val hunger = itemStack?.item?.foodComponent?.hunger ?: 1
                    heal(hunger * 1f)
                    equipStack(EquipmentSlot.MAINHAND, Items.AIR.defaultStack)
                    isEating = false
                    isFinished = true
                }
            }
        }

        override fun nextTask(): Attack {
            return weightedCollection {
                if (health < maxHealth) {
                    50.0 to Attack.EAT
                    50.0 to Attack.CHECK_TARGET
                } else {
                    100.0 to Attack.SLEEP
                }
            }.next()
        }
    }

    private fun spawnItemParticles(stack: ItemStack?, count: Int) {
        for (i in 0 until count) {
            val offSet = 0.3
            val pos = eyePos.add(directionVector.normalize().subtract(0.0, 0.3, 0.0).multiply(2.0))
            (world as? ServerWorld?)?.spawnParticles(
                ItemStackParticleEffect(ParticleTypes.ITEM, stack),
                pos.x + Random.nextDouble(-offSet, offSet),
                pos.y + Random.nextDouble(-offSet, offSet),
                pos.z + Random.nextDouble(-offSet, offSet),
                0,
                0.0,
                0.0,
                0.0,
                0.0
            )
        }
    }

    inner class BellyFlopTask : Task() {
        override fun onEnable() {
            if (target == null) {
                isFinished = true
            } else {
                val from = pos
                val to = target!!.pos //TODO nullcheck
                val direction = to.subtract(from)
                modifyVelocity(direction.normalize().multiply(1.2, 0.0, 1.2))
                modifyVelocity(0, Random.nextDouble(1.0, 1.5), 0)
                sound(SoundManager.JUMP, 0.4f, 1f)

                mcCoroutineTask(delay = 560.milliseconds) {
                    NetworkManager.SET_CUSTOM_HIT_BOX_PACKET.sendToAll(CustomHitBox(uuid, SLEEPING_DIMENSIONS))
                }

                jobs += infiniteMcCoroutineTask(delay = 5.ticks) {
                    val isGrounded = isOnGround

                    if (isGrounded) {
                        mcCoroutineTask(delay = 1.seconds) { isFinished = true }
                        world.getEntitiesByClass(PlayerEntity::class.java, Box.of(pos, 14.0, 5.0, 14.0)) { true }
                            .forEach { player ->
                                tryFlatPlayer(player)
                                tryAttackWithShieldBreak(player)
                            }
                        this.cancel()
                    }
                }
            }
        }

        override fun onDisable() {
            super.onDisable()
            NetworkManager.REMOVE_CUSTOM_HIT_BOX_PACKET.sendToAll(UUIDWrapper(uuid))
        }

        override fun nextTask(): Attack {
            return weightedCollection {
                60.0 to Attack.RUN
                18.0 to Attack.CHECK_TARGET
                2.0 to Attack.SLEEP
            }.next()
        }
    }

    override fun isPushable(): Boolean = false

    private fun tryFlatPlayer(player: PlayerEntity) {
        val modifiedPlayer = player as ModifiedPlayer
        if (!player.isFlat()) {
            world.playSound(null, player.blockPos, SoundEvents.ENTITY_PUFFER_FISH_BLOW_OUT, SoundCategory.PLAYERS)
            player.setFlat(true)
            player.setFlatJumps(0)
            player.setNormalReach(3.0f)
            mcCoroutineTask(delay = Random.nextInt(10, 15).seconds) {
                if (player.isFlat()) {
                    player.setFlat(false)
                    player.setNormalReach(4.5f)
                    world.playSound(
                        null, player.blockPos, SoundEvents.ENTITY_PUFFER_FISH_BLOW_UP, SoundCategory.PLAYERS
                    )
                }
            }
        }
    }

    override fun onDeath(damageSource: DamageSource?) {
        super.onDeath(damageSource)
        task?.onDisable()
    }

    private fun sound(soundEvent: SoundEvent, volume: Float, pitch: Float) {
        world.playSoundFromEntity(null, this, soundEvent, SoundCategory.HOSTILE, volume, pitch)
    }

    //TODO Sleeping hitbox
    override fun getDimensions(pose: EntityPose): EntityDimensions {
        return customHitBox ?: POSE_DIMENSIONS.getOrDefault(attack, STANDING_DIMENSIONS)
    }

    override fun registerControllers(controller: AnimatableManager.ControllerRegistrar) {
        controller.add(AnimationController(this, "controller", 0) {

            if (forceAnimationReset) {
                it.controller.forceAnimationReset()
                forceAnimationReset = false
            }

            it.controller.setAnimation(attack.animation)
            return@AnimationController PlayState.CONTINUE
        }.setParticleKeyframeHandler { }).add(AnimationController(this, "spinning", 0, this::spinningController))
            .add(AnimationController(this, "rolling", 0, this::rollingController))
            .add(AnimationController(this, "walking", 0, this::walkingController))
            .add(AnimationController(this, "throwing-entity", 0, this::throwingEntityController))
            .add(AnimationController(this, "throwing-block", 0, this::throwingBlockController))
            .add(AnimationController(this, "eating", 0, this::eatingController))
            .add(AnimationController(this, "dieing", 0, this::dieingController))
            .add(AnimationController(this, "bodycheck", 0, this::bodycheckController))
    }

    private fun <T : GeoAnimatable> bodycheckController(state: AnimationState<T>): PlayState {
        if (isBodyChecking) {
            return state.setAndContinue("bodycheck".play())
        } else {
            state.controller.forceAnimationReset()
        }
        return PlayState.STOP
    }

    private fun <T : GeoAnimatable> dieingController(state: AnimationState<T>): PlayState {
        if (isDead) {
            return state.setAndContinue("sleep".once().loop("sleep-idle"))
        } else {
            state.controller.forceAnimationReset()
        }
        return PlayState.STOP
    }

    private fun <T : GeoAnimatable> spinningController(state: AnimationState<T>): PlayState {
        if (isSpinning) {
            return state.setAndContinue("spin".loop())
        } else {
            state.controller.forceAnimationReset()
        }
        return PlayState.STOP
    }

    private fun <T : GeoAnimatable> rollingController(state: AnimationState<T>): PlayState {
        if (isRolling) {
            return state.setAndContinue("rolling".loop())
        } else {
            state.controller.forceAnimationReset()
        }
        return PlayState.STOP
    }

    private fun <T : GeoAnimatable> throwingEntityController(state: AnimationState<T>): PlayState {
        if (isThrowingEntity) {
            return state.setAndContinue("throw".hold())
        } else {
            state.controller.forceAnimationReset()
        }
        return PlayState.STOP
    }

    private fun <T : GeoAnimatable> throwingBlockController(state: AnimationState<T>): PlayState {
        if (isThrowingBlock) {
            return state.setAndContinue("throw-block".hold())
        } else {
            state.controller.forceAnimationReset()
        }
        return PlayState.STOP
    }

    private fun <T : GeoAnimatable> eatingController(state: AnimationState<T>): PlayState {
        if (isPickingUpFood) {
            return state.setAndContinue("pickup-food".hold())
        } else {
            if (isEating) {
                return state.setAndContinue("eating".play())
            } else {
                state.controller.forceAnimationReset()
            }
        }
        return PlayState.STOP
    }

    private fun <T : GeoAnimatable> walkingController(state: AnimationState<T>): PlayState {
        if (istAmSpringen) {
            state.controller.setAnimation("jump".hold())
            return PlayState.CONTINUE
        }

        if (isMoving) {
            state.controller.setAnimation("run".loop())
            return PlayState.CONTINUE
        }
        return PlayState.STOP
    }

    override fun getAnimatableInstanceCache(): AnimatableInstanceCache = factory

    override fun updatePassengerPosition(passenger: Entity) {
        if (this.hasPassenger(passenger)) {
            val d = this.y + this.mountedHeightOffset + passenger.heightOffset
            val direction = directionVector.normalize().multiply(2.0)
            passenger.setPosition(this.x + direction.x, d - 1.5, this.z + direction.z)
        }
    }
}
