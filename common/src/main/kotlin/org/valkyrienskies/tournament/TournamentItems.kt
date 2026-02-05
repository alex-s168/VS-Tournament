package org.valkyrienskies.tournament

import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.Item
import net.minecraft.core.Registry
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.*
import net.minecraft.world.item.Item.Properties
import net.minecraft.world.item.crafting.Ingredient
import org.valkyrienskies.tournament.items.GiftBagItem
import org.valkyrienskies.tournament.items.PulseGunItem
import org.valkyrienskies.tournament.items.RopeItem
import org.valkyrienskies.tournament.items.ShipDeleteWandItem
import org.valkyrienskies.tournament.items.old.OldItem
import org.valkyrienskies.tournament.items.old.UpdateItem
import org.valkyrienskies.tournament.registry.DeferredRegister
import org.valkyrienskies.tournament.registry.RegistrySupplier
import org.valkyrienskies.tournament.util.extension.once

@Suppress("unused")
object TournamentItems {
    val ITEMS = DeferredRegister.create(TournamentMod.MOD_ID, Registries.ITEM)
    val fuelItems = mutableListOf<Pair<String, FuelType>>()

    lateinit var ROPE              :  RegistrySupplier<RopeItem>
    lateinit var TOOL_PULSEGUN     :  RegistrySupplier<PulseGunItem>
    lateinit var TOOL_DELETEWAND   :  RegistrySupplier<ShipDeleteWandItem>
    lateinit var UPGRADE_THRUSTER  :  RegistrySupplier<Item>
    lateinit var GIFT_BAG          :  RegistrySupplier<GiftBagItem>
    lateinit var INGOT_PHYNITE     :  RegistrySupplier<Item>
    lateinit var WOOL_SHEET        :  RegistrySupplier<Item>
    lateinit var UNSTABLE_LAPIS    :  RegistrySupplier<Item>

    val TAB: ResourceKey<CreativeModeTab> =
        ResourceKey.create(Registries.CREATIVE_MODE_TAB, ResourceLocation(TournamentMod.MOD_ID, "main_tab"))

    val register by once {
        ROPE                    = ITEMS.register("rope", ::RopeItem)
        TOOL_PULSEGUN           = ITEMS.register("pulse_gun", ::PulseGunItem)
        TOOL_DELETEWAND         = ITEMS.register("delete_wand", ::ShipDeleteWandItem)
        UPGRADE_THRUSTER        = ITEMS.register("upgrade_thruster") {
            Item(Properties().stacksTo(16))
        }
        GIFT_BAG                = ITEMS.register("gift_bag", ::GiftBagItem)
        ITEMS.register("iron_cube") {
            Item(Properties().stacksTo(64))
        }
        ITEMS.register("coal_dust") {
            Item(Properties().stacksTo(64))
        }
        ITEMS.register("hammer") {
            Item(Properties().stacksTo(8))
        }

        ITEMS.register("raw_phynite") {
            Item(Properties().stacksTo(64))
        }

        INGOT_PHYNITE = ITEMS.register("ingot_phynite") {
            Item(Properties().stacksTo(64))
        }

        ITEMS.register("physics_shard") {
            Item(Properties().stacksTo(64))
        }

        WOOL_SHEET = ITEMS.register("wool_sheet") {
            Item(Properties().stacksTo(64))
        }

        ITEMS.register("wool_wing") {
            Item(Properties().stacksTo(64))
        }

        ITEMS.register("iron_tip") {
            Item(Properties().stacksTo(64))
        }

        UNSTABLE_LAPIS = ITEMS.register("unstable_lapis") {
            Item(Properties().stacksTo(64))
        }

        ITEMS.register("unstable_powder") {
            Item(Properties().stacksTo(64))
        }

        fuelItems.forEach { ITEMS.register(it.first) { Item(Properties().stacksTo(64)) } }

        // old:
        ITEMS.register("grab_gun") { OldItem("grab_gun") }
        ITEMS.register("delete_gun") { UpdateItem(TOOL_DELETEWAND.get()) }


        TournamentBlocks.registerItems(ITEMS)
        ITEMS.applyAll()
    }

}
