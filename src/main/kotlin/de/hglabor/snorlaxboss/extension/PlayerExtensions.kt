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

val PlayerEntity.allFoodItems: Map<Int, ItemStack>
    get() = buildMap {
        inventory.main.forEachIndexed { index, itemStack ->
            if (itemStack.isFood) {
                put(index, itemStack)
            }
        }
    }

fun PlayerEntity.dropRandomFood(singleDrop: Boolean = false, callBack: (() -> Unit)?) {
    val (index, food) = allFoodItems.random() ?: return
    dropItem(
        inventory.removeStack(index, if (singleDrop) 1 else 1.coerceAtLeast(Random.nextInt(food.count))),
        true,
        false
    )
    callBack?.invoke()
}

val PlayerEntity.randomInvFoodItem: Pair<ItemStack, Int>?
    get() {
        if (inventory.main.all { it == ItemStack.EMPTY }) return null
        val index = Random.nextInt(inventory.main.size)
        val itemStack = inventory.main[index]
        if (itemStack.isItemEqual(ItemStack.EMPTY) || itemStack.isOf(Items.AIR) || itemStack.isFood) return null
        return Pair(itemStack, index)
    }
