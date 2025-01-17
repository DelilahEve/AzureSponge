package xyz.azuremoon.listeners

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.data.Waterlogged
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.block.SpongeAbsorbEvent

import xyz.azuremoon.util.ConfigController
import xyz.azuremoon.listeners.AdjListener.Companion.spongeRadiusAdj
import xyz.azuremoon.listeners.AdjListener.Companion.spongeShapeAdj

import kotlin.math.sqrt

class ASEventListener : Listener {

    @EventHandler
    fun onSpongePlace(e: BlockPlaceEvent) {

        val allowedReplaceBlocks = listOf(Material.LAVA, Material.WATER)

        when {
            e.blockPlaced.type != Material.SPONGE -> return
            e.blockReplacedState.type !in allowedReplaceBlocks -> return
        }

        val drainArea = if (e.player.hasPermission("sponge.adj")) {
            areaAround(e.block.location,
                spongeRadiusAdj[e.player.uniqueId]?.first ?: ConfigController.spongeRadius,
                spongeShapeAdj[e.player.uniqueId]?.second ?: ConfigController.clearShape)
        } else if (e.player.hasPermission("sponge.use")) {
            areaAround(e.block.location, ConfigController.spongeRadius)
        } else {
            areaAround(e.block.location, 5, "sphere")
        }

        drainArea.forEach { void ->
            when (void.type) {
                Material.KELP_PLANT -> {
                    void.breakNaturally(); void.type = Material.AIR
                }

                Material.KELP -> {
                    void.breakNaturally(); void.type = Material.AIR
                }

                Material.SEAGRASS -> void.type = Material.AIR
                Material.TALL_SEAGRASS -> void.type = Material.AIR
                Material.WATER -> void.type = Material.AIR
                Material.BUBBLE_COLUMN -> void.type = Material.AIR
                Material.LAVA -> if (e.player.hasPermission("sponge.lava")) {
                    void.type = Material.AIR
                }
                else -> {}
            }
            if (void.blockData is Waterlogged && ConfigController.clearWaterlogged) {
                val wl: Waterlogged = void.blockData as Waterlogged
                if (wl.isWaterlogged) {
                    wl.isWaterlogged = false
                    void.blockData = wl
                    void.state.update()
                }
            }
        }

        if (e.player.hasPermission("sponge.shield") && e.player.isSneaking) {

            val shieldArea = if (e.player.hasPermission("sponge.adj")) {
                areaAround(e.block.location,
                    spongeRadiusAdj[e.player.uniqueId]?.second ?: ConfigController.shieldRadius,
                    spongeShapeAdj[e.player.uniqueId]?.second ?: ConfigController.clearShape,
                    true)
            }
            else { areaAround(e.blockPlaced.location, (ConfigController.shieldRadius), hollow = true) }

            shieldArea.forEach {
                when (it.type) {
                    Material.AIR -> it.type = Material.STRUCTURE_VOID
                    else -> {}
                }
            }
        }
        if (e.player.hasPermission("sponge.dry")) {
            e.blockPlaced.type = Material.SPONGE
        } else {
            e.blockPlaced.type = Material.WET_SPONGE
        }
    }

    @EventHandler
    fun onSpongeRemove(e: BlockBreakEvent) {
        if (e.block.type == Material.SPONGE || e.block.type == Material.WET_SPONGE) {
            areaAround(e.block.location, ConfigController.maxAdjRadius, "cube").forEach {
                when (it.type) {
                    Material.STRUCTURE_VOID -> it.type = Material.AIR
                    else -> {}
                }
            }
        }
    }

    @EventHandler
    fun spongeOverride(e: SpongeAbsorbEvent){
        e.isCancelled = true
    }

    private fun areaAround(
        location: Location,
        radius: Int,
        shape: String = ConfigController.clearShape,
        hollow: Boolean = false
        ): List<Block> {
        val area = mutableListOf<Block>()
        val range = -radius..radius
        range.forEach { x ->
            range.forEach { y ->
                range.forEach { z ->
                    when (shape) {
                        "cube" ->
                            if (!hollow || ((x == -radius || x == radius) || (y == -radius || y == radius) || (z == -radius || z == radius))) {
                                area.add(location.block.getRelative(x, y, z))
                            }

                        "sphere" -> {
                            val distance = sqrt((x * x + y * y + z * z).toDouble())
                            if (distance <= radius && !(hollow && distance <= (radius - 1))) {
                                area.add(location.block.getRelative(x, y, z))
                            }
                        }

                        "cylinder" -> {
                            val distance = sqrt((x * x + z * z).toDouble())
                            if (distance <= radius && !(hollow && distance <= (radius - 1))) {
                                area.add(location.block.getRelative(x, y, z))
                            }
                        }
                    }
                }
            }
        }
        return area
    }
}


