package de.hglabor.snorlaxboss.item

import de.hglabor.snorlaxboss.extension.toId
import de.hglabor.snorlaxboss.item.items.PokeBallItem
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents.ModifyEntries
import net.minecraft.item.ItemGroups
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry


object ItemManager {
    val POKEBALL = PokeBallItem(FabricItemSettings().maxCount(1))

    fun init() {
        Registry.register(Registries.ITEM, "pokeball".toId(), POKEBALL)

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.SPAWN_EGGS).register(ModifyEntries { content ->
            content.add(POKEBALL)
        })
    }
}