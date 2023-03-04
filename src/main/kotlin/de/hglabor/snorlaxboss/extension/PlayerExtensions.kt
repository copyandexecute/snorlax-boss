package de.hglabor.snorlaxboss.extension

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import kotlin.random.Random

val PlayerEntity.randomMainInvItem: Pair<ItemStack, Int>?
    get() {
        if (inventory.main.all { it == ItemStack.EMPTY }) return null
        val index = Random.nextInt(inventory.main.size)
        val itemStack = inventory.main[index]
        if (itemStack.isItemEqual(ItemStack.EMPTY) || itemStack.isOf(Items.AIR)) return null
        return Pair(itemStack, index)
    }
