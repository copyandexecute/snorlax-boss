package de.hglabor.snorlaxboss.entity.projectile

import de.hglabor.snorlaxboss.entity.EntityManager
import de.hglabor.snorlaxboss.item.ItemManager
import net.minecraft.entity.EntityStatuses
import net.minecraft.entity.EntityType
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.projectile.thrown.ThrownItemEntity
import net.minecraft.item.Item
import net.minecraft.particle.ItemStackParticleEffect
import net.minecraft.particle.ParticleTypes
import net.minecraft.util.hit.EntityHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.world.World

class PokeBallEntity : ThrownItemEntity {
    constructor(entityType: EntityType<out PokeBallEntity?>?, world: World?) : super(entityType, world)
    constructor(world: World?, owner: LivingEntity?) : super(EntityManager.POKEBALL, owner, world)
    constructor(world: World?, x: Double, y: Double, z: Double) : super(EntityManager.POKEBALL, x, y, z, world)

    override fun handleStatus(status: Byte) {
        if (status == EntityStatuses.PLAY_DEATH_SOUND_OR_ADD_PROJECTILE_HIT_PARTICLES) {
            val d = 0.08
            for (i in 0..7) {
                world.addParticle(
                    ItemStackParticleEffect(ParticleTypes.ITEM, this.stack),
                    this.x,
                    this.y,
                    this.z,
                    (random.nextFloat().toDouble() - 0.5) * 0.08,
                    (random.nextFloat().toDouble() - 0.5) * 0.08,
                    (random.nextFloat().toDouble() - 0.5) * 0.08
                )
            }
        }
    }

    override fun onEntityHit(entityHitResult: EntityHitResult) {
        super.onEntityHit(entityHitResult)
        entityHitResult.entity.damage(DamageSource.thrownProjectile(this, owner), 0.0f)
    }

    override fun onCollision(hitResult: HitResult) {
        super.onCollision(hitResult)
        if (!world.isClient) {

            val snorlax = EntityManager.SNORLAX.create(world)
            if (snorlax != null) {
                snorlax.refreshPositionAndAngles(this.x, this.y, this.z, yaw, 0.0f)
                world.spawnEntity(snorlax)
            }

            world.sendEntityStatus(this, EntityStatuses.PLAY_DEATH_SOUND_OR_ADD_PROJECTILE_HIT_PARTICLES)
            discard()
        }
    }

    override fun getDefaultItem(): Item {
        return ItemManager.POKEBALL
    }
}
