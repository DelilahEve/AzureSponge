package xyz.azuremoon.listeners

import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import xyz.azuremoon.commands.CommandController.Commands.Companion.shapeList
import xyz.azuremoon.util.ConfigController
import java.util.*


class AdjListener : Listener {

    companion object {
        val spongeRadiusAdj = mutableMapOf<UUID, Pair<Int,Int>>()
        val spongeShapeAdj = mutableMapOf<UUID, Pair<Material,String>>()
    }

    @EventHandler
    fun spongeSizeAdj(e: InventoryClickEvent){
        if (e.view.title != "Sponge ConfigGui" || e.currentItem == null) return

        e.isCancelled = true

        var slot = e.rawSlot

        val name = e.currentItem!!.itemMeta!!.displayName

        when (name) {
            "-1" -> slot -= 9
            "+1" -> slot += 9
            "<-" -> slot -= 9
            "->" -> slot += 9
        }
        updateConfigItem(slot, e.inventory, name)
    }

    @EventHandler
    fun spongeSizeAdjClose(e: InventoryCloseEvent){
        if (e.view.title != "Sponge ConfigGui") return

        val range = 0..26
        var ssRange = 0
        var saRange = 0
        var sShape = "cube"
        var sBlock = Material.AIR

        range.forEach{slot ->
            when (e.inventory.getItem(slot)?.type){
                Material.SPONGE -> {ssRange = e.inventory.getItem(slot)?.amount ?: 0}
                Material.WET_SPONGE -> {saRange = e.inventory.getItem(slot)?.amount ?: 0 }
                Material.SLIME_BLOCK -> {sShape = "cube"; sBlock = Material.SLIME_BLOCK}
                Material.SLIME_BALL -> {sShape = "sphere"; sBlock = Material.SLIME_BALL}
                Material.SUNFLOWER -> {sShape = "cylinder"; sBlock = Material.SUNFLOWER}
                else -> {}
            }
        }
        spongeRadiusAdj[e.player.uniqueId] = Pair(saRange, ssRange)
        spongeShapeAdj[e.player.uniqueId] = Pair(sBlock , sShape)
        }

    private fun updateConfigItem(slot: Int, inventory: Inventory, name: String) {

        var amount = inventory.getItem(slot)!!.amount
        val itemName = inventory.getItem(slot)!!.itemMeta!!.displayName
        val itemMaterial = inventory.getItem(slot)!!.type
        var index = 0
        when (name) {
            "-1" -> if (amount >= 2) {
                amount -= 1
            }

            "+1" -> if (amount < ConfigController.maxAdjRadius) amount += 1
            "->" -> {
                index = updateShapeItem(+1, Pair(itemMaterial, itemName))
            }

            "<-" -> {
                index = updateShapeItem(-1, Pair(itemMaterial, itemName))
            }
        }

        when (itemName) {
            "Shield Radius" -> {
                val shield = ItemStack(Material.SPONGE, amount)
                val ssMeta = shield.itemMeta
                ssMeta?.setDisplayName("Shield Radius")
                shield.itemMeta = ssMeta
                inventory.setItem(slot, shield)
            }

            "Absorption Radius" -> {
                val sponge = ItemStack(Material.WET_SPONGE, amount)
                val srMeta = sponge.itemMeta
                srMeta?.setDisplayName("Absorption Radius")
                sponge.itemMeta = srMeta
                inventory.setItem(slot, sponge)
            }

            else -> {
                val shape = ItemStack(shapeList[index].first)
                val sMeta = shape.itemMeta
                sMeta?.setDisplayName(shapeList[index].second)
                shape.itemMeta = sMeta
                inventory.setItem(slot, shape)
            }
        }
    }

    private fun updateShapeItem(direction: Int, item: Pair<Material, String>): Int {

        var index = shapeList.indexOf(item)

        index += direction

        if (index > (shapeList.count() -1)) {index = 0}
        else if (index < 0) {index = (shapeList.count() -1)}

        return index
    }
}