package de.hglabor.snorlaxboss.command

import com.mojang.brigadier.arguments.FloatArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.context.CommandContext
import de.hglabor.snorlaxboss.SnorlaxBoss.Companion.IS_DEVELOPMENT
import de.hglabor.snorlaxboss.entity.EntityManager
import de.hglabor.snorlaxboss.entity.Snorlax
import de.hglabor.snorlaxboss.entity.player.ModifiedPlayer
import de.hglabor.snorlaxboss.extension.randomMainInvItem
import de.hglabor.snorlaxboss.particle.Attacks
import kotlinx.coroutines.cancel
import net.minecraft.enchantment.Enchantments
import net.minecraft.entity.EntityPose
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Items
import net.minecraft.particle.ParticleEffect
import net.minecraft.potion.Potions
import net.minecraft.registry.Registries
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.util.Identifier
import net.minecraft.util.math.MathHelper
import net.silkmc.silk.commands.command
import net.silkmc.silk.core.item.itemStack
import net.silkmc.silk.core.item.setPotion
import net.silkmc.silk.core.task.infiniteMcCoroutineTask
import net.silkmc.silk.core.task.mcCoroutineTask
import net.silkmc.silk.core.text.broadcastText

object CommandManager {
    fun init() {
        if (IS_DEVELOPMENT) {
            fun CommandContext<ServerCommandSource>.getAllSnorlax(): MutableList<out Snorlax> {
                return this.source.world.getEntitiesByType(EntityManager.SNORLAX) { true }
            }

            command("wave") {
                argument<Int>("radius", IntegerArgumentType.integer(1)) { radius ->
                    runs {
                        Attacks.radialWave(this.source.playerOrThrow, radius())
                    }
                }
            }

            command("sleep") {
                runs {
                    val modifiedPlayer = this.source.playerOrThrow as ModifiedPlayer
                    modifiedPlayer.isForceSleeping = !modifiedPlayer.isForceSleeping
                }
            }

            command("snorlax") {
                literal("spinning") {
                    runs {
                        this.getAllSnorlax().forEach {
                            it.isSpinning = !it.isSpinning
                        }
                    }
                }
                literal("rolling") {
                    runs {
                        this.getAllSnorlax().forEach {
                            it.isRolling = !it.isRolling
                        }
                    }
                }
                literal("throw") {
                    runs {
                        this.getAllSnorlax().forEach {
                            it.isThrowing = !it.isThrowing
                        }
                    }
                }
                literal("come") {
                    runs {
                        val pos = this.source.playerOrThrow.pos
                        getAllSnorlax().forEach {
                            it.moveToPosition(pos)
                        }
                    }
                }
            }

            command("attack") {
                argument("state") { state ->
                    suggestList { Snorlax.Attack.values().map { it.name } }
                    runs {
                        this.getAllSnorlax().forEach {
                            it.attack = Snorlax.Attack.valueOf(state())
                        }
                    }
                }
            }

            command("randomitem") {
                runs {
                    val player = this.source.playerOrThrow
                    val (item, slot) = player.randomMainInvItem ?: Pair(null, null)
                }
            }

            command("reach") {
                argument<Float>("value", FloatArgumentType.floatArg(0.0f, 4.5f)) { value ->
                    runs {
                        (this.source.playerOrThrow as? ModifiedPlayer?)?.apply {
                            setNormalReach(value())
                        }
                    }
                }
            }

            command("flat") {
                runs {
                    val player = this.source.playerOrThrow as ModifiedPlayer
                    player.setFlat(!player.isFlat())
                    if (player.isFlat()) {
                        player.setFlatJumps(0)
                    }
                }
            }

            command("equip") {
                runs {
                    this.source.playerOrThrow.bossEquip()
                }
            }

            command("beam") {
                argument<String>("particle") { particle ->
                    suggestList { Registries.PARTICLE_TYPE.ids.map { it.path } }
                    runs {
                        val identifier = Identifier("minecraft", particle())
                        Attacks.hyperBeam(
                            this.source.playerOrThrow,
                            Registries.PARTICLE_TYPE.get(identifier) as ParticleEffect
                        )
                    }
                }
                runs {
                    Attacks.hyperBeam(this.source.playerOrThrow)
                }
            }
            command("inhale") {
                argument<String>("particle") { particle ->
                    suggestList { Registries.PARTICLE_TYPE.ids.map { it.path } }
                    runs {
                        val identifier = Identifier("minecraft", particle())
                        Attacks.inhale(
                            this.source.playerOrThrow,
                            Registries.PARTICLE_TYPE.get(identifier) as ParticleEffect,
                            target = source.playerOrThrow
                        )
                    }
                }
                runs {
                    Attacks.inhale(this.source.playerOrThrow, target = source.playerOrThrow)
                }
            }
        }
    }

    private fun PlayerEntity.bossEquip() {
        inventory.clear()
        equipStack(EquipmentSlot.HEAD, itemStack(Items.NETHERITE_HELMET) {
            addEnchantment(Enchantments.PROTECTION, 4)
            addEnchantment(Enchantments.UNBREAKING, 3)
        })
        equipStack(EquipmentSlot.CHEST, itemStack(Items.NETHERITE_CHESTPLATE) {
            addEnchantment(Enchantments.PROTECTION, 4)
            addEnchantment(Enchantments.UNBREAKING, 3)
        })
        equipStack(EquipmentSlot.LEGS, itemStack(Items.NETHERITE_LEGGINGS) {
            addEnchantment(Enchantments.PROTECTION, 4)
            addEnchantment(Enchantments.UNBREAKING, 3)
        })
        equipStack(EquipmentSlot.FEET, itemStack(Items.NETHERITE_BOOTS) {
            addEnchantment(Enchantments.PROTECTION, 4)
            addEnchantment(Enchantments.UNBREAKING, 3)
            addEnchantment(Enchantments.FEATHER_FALLING, 4)
        })
        equipStack(EquipmentSlot.MAINHAND, itemStack(Items.NETHERITE_SWORD) {
            addEnchantment(Enchantments.SHARPNESS, 5)
            addEnchantment(Enchantments.UNBREAKING, 3)
        })
        equipStack(EquipmentSlot.OFFHAND, itemStack(Items.SHIELD) {
            addEnchantment(Enchantments.UNBREAKING, 3)
        })
        giveItemStack(itemStack(Items.BOW) {
            addEnchantment(Enchantments.POWER, 5)
            addEnchantment(Enchantments.UNBREAKING, 3)
            addEnchantment(Enchantments.PUNCH, 2)
            addEnchantment(Enchantments.INFINITY, 1)
        })
        giveItemStack(itemStack(Items.COOKED_BEEF) {
            count = 64
        })
        inventory.setStack(8, itemStack(Items.WATER_BUCKET) {})
        inventory.setStack(7, itemStack(Items.ENDER_PEARL) {
            count = 16
        })
        giveItemStack(itemStack(Items.OAK_PLANKS) {
            count = 64
        })
        giveItemStack(itemStack(Items.GOLDEN_APPLE) {
            count = 47
        })
        giveItemStack(itemStack(Items.ENCHANTED_GOLDEN_APPLE) {
            count = 2
        })
        giveItemStack(itemStack(Items.SHIELD) {
            addEnchantment(Enchantments.UNBREAKING, 3)
        })
        giveItemStack(itemStack(Items.ARROW) {})
        giveItemStack(itemStack(Items.WATER_BUCKET) {})

        giveItemStack(itemStack(Items.NETHERITE_HELMET) {
            addEnchantment(Enchantments.PROTECTION, 4)
            addEnchantment(Enchantments.UNBREAKING, 3)
        })
        giveItemStack(itemStack(Items.NETHERITE_CHESTPLATE) {
            addEnchantment(Enchantments.PROTECTION, 4)
            addEnchantment(Enchantments.UNBREAKING, 3)
        })
        giveItemStack(itemStack(Items.NETHERITE_LEGGINGS) {
            addEnchantment(Enchantments.PROTECTION, 4)
            addEnchantment(Enchantments.UNBREAKING, 3)
        })
        giveItemStack(itemStack(Items.NETHERITE_BOOTS) {
            addEnchantment(Enchantments.PROTECTION, 4)
            addEnchantment(Enchantments.UNBREAKING, 3)
            addEnchantment(Enchantments.FEATHER_FALLING, 4)
        })
        giveItemStack(itemStack(Items.NETHERITE_SWORD) {
            addEnchantment(Enchantments.SHARPNESS, 5)
            addEnchantment(Enchantments.UNBREAKING, 3)
        })

        repeat(10) {
            giveItemStack(itemStack(Items.SPLASH_POTION) {
                setPotion(Potions.STRONG_HEALING)
            })
        }
        repeat(2) {
            giveItemStack(itemStack(Items.POTION) {
                setPotion(Potions.STRONG_REGENERATION)
            })
        }
        repeat(3) {
            giveItemStack(itemStack(Items.POTION) {
                setPotion(Potions.STRONG_SWIFTNESS)
            })
        }
        repeat(3) {
            giveItemStack(itemStack(Items.TOTEM_OF_UNDYING) {})
        }
    }
}
