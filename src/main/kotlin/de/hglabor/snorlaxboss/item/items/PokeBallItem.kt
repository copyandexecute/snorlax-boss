package de.hglabor.snorlaxboss.item.items

import de.hglabor.snorlaxboss.entity.projectile.PokeBallEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.util.Hand
import net.minecraft.util.TypedActionResult
import net.minecraft.world.World

class PokeBallItem(settings: Settings) : Item(settings) {
    override fun use(world: World, user: PlayerEntity, hand: Hand?): TypedActionResult<ItemStack>? {
        val itemStack = user.getStackInHand(hand)
        world.playSound(
            null as PlayerEntity?,
            user.x,
            user.y,
            user.z,
            SoundEvents.ENTITY_EGG_THROW,
            SoundCategory.PLAYERS,
            0.5f,
            0.4f / (world.getRandom().nextFloat() * 0.4f + 0.8f)
        )
        if (!world.isClient) {
            val pokeballEntity = PokeBallEntity(world, user)
            pokeballEntity.setItem(itemStack)
            pokeballEntity.setVelocity(user, user.pitch, user.yaw, 0.0f, 1.5f, 1.0f)
            world.spawnEntity(pokeballEntity)
        }
        if (!user.abilities.creativeMode) {
            itemStack.decrement(1)
        }
        return TypedActionResult.success(itemStack, world.isClient())
    }
}