package de.hglabor.snorlaxboss.extension

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import kotlin.random.Random

val PlayerEntity.randomMainInvItem: Pair<ItemStack, Int>?
    get() {
        if (inventory.main.all { it == ItemStack.EMPTY }) return null
        var itemStack: ItemStack? = null
        var index = 0
        while (itemStack == null) {
            index = Random.nextInt(inventory.main.size)
            itemStack = inventory.main[index]
            if (itemStack.isItemEqual(ItemStack.EMPTY) || itemStack.isOf(Items.AIR)) {
                itemStack = null
            }
        }
        return Pair(itemStack, index)
    }
