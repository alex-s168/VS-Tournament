package org.valkyrienskies.tournament.blockentity

import blitz.caching
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.world.WorldlyContainer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.tournament.TournamentConfig
import org.valkyrienskies.tournament.ship.TournamentShips
import org.valkyrienskies.tournament.tournamentFuel
import org.valkyrienskies.tournament.util.extension.void
import java.util.BitSet
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

class FuelTankBlockEntity(
    pos: BlockPos,
    state: BlockState,
    var capf: Float,
    src: () -> BlockEntityType<FuelTankBlockEntity>
): BlockEntity(
    src(),
    pos,
    state
) {

    @Volatile
    var wholeShipFillLevelSynced = 0.0f
        private set

    override fun getUpdateTag(): CompoundTag =
        CompoundTag().also {
            saveAdditional(it)
        }

    override fun getUpdatePacket(): ClientboundBlockEntityDataPacket? =
        ClientboundBlockEntityDataPacket.create(this)

    override fun saveAdditional(tag: CompoundTag) {
        tag.putFloat("ship_fill_synced", wholeShipFillLevelSynced)
        tag.putByteArray("neighbors", neighborsTransparent.toByteArray())
        super.saveAdditional(tag)
    }

    override fun load(tag: CompoundTag) {
        wholeShipFillLevelSynced = tag.getFloat("ship_fill_synced")

        neighborsTransparent = if (tag.contains("neighbors"))
            BitSet.valueOf(tag.getByteArray("neighbors"))
        else BitSet(6)

        super.load(tag)
    }

    fun update() {
        level?.sendBlockUpdated(blockPos, blockState, blockState, Block.UPDATE_ALL_IMMEDIATE)
    }

    fun tick() {
        ship {
            wholeShipFillLevelSynced = it.fuelCount / it.fuelCap
            update()
        }
    }

    val cap by caching(::capf) {
        ceil(TournamentConfig.SERVER.fuelContainerCap * capf).toInt()
    }

    @OptIn(ExperimentalContracts::class)
    fun <R> ship(fn: (TournamentShips) -> R): R? {
        contract {
            callsInPlace(fn, InvocationKind.AT_MOST_ONCE)
        }
        return TournamentShips.get(level!!, blockPos)?.let(fn)
    }

    fun onAdded() {
        ship {
            it.fuelCap += cap
        }
    }

    fun onRemoved() {
        ship {
            it.fuelCap -= cap
        }
    }

    fun getContainer(): CustomContainer {
        return CustomContainer(this)
    }

    fun canStoreCount(stack: ItemStack): Int =
        ship {
            val fuel = stack.tournamentFuel()?.fuel
            if (fuel != null && it.fuelType?.let { it == fuel } != false)
                min(stack.count, max(it.fuelCap - it.fuelCount, 0.0f).toInt())
            else 0
        } ?: 0

    fun forceStore(stack: ItemStack, count: Int) =
        ship {
            it.fuelType = stack.tournamentFuel()
            it.fuelCount += count
        }.void()

    var neighborsTransparent = BitSet(6)

    fun isNeighborTransparent(direction: Direction) =
        neighborsTransparent[direction.ordinal]

    class CustomContainer(
        val be: FuelTankBlockEntity
    ): WorldlyContainer {
        override fun clearContent() {
            be.onRemoved()
        }

        override fun getContainerSize(): Int =
            be.ship {
                it.fuelCap.toInt()
            } ?: 0

        override fun isEmpty(): Boolean =
            containerSize == 0

        override fun getItem(slot: Int) =
            ItemStack(Items.AIR)

        override fun removeItem(slot: Int, amount: Int) =
            error("can't remove item from fuel tank")

        override fun removeItemNoUpdate(slot: Int) =
            error("can't remove item from fuel tank")

        override fun setItem(slot: Int, stack: ItemStack) {
            require(canPlaceItemThroughFace(slot, stack, null))
            be.forceStore(stack, stack.count)
        }

        override fun setChanged() {}

        override fun stillValid(player: Player): Boolean =
            true

        override fun getSlotsForFace(side: Direction): IntArray =
            intArrayOf(0)

        override fun canPlaceItemThroughFace(index: Int, stack: ItemStack, direction: Direction?) =
            index == 0 && be.canStoreCount(stack) == stack.count

        override fun canTakeItemThroughFace(index: Int, stack: ItemStack, direction: Direction): Boolean =
            false

    }

    companion object {
        val ticker = BlockEntityTicker<FuelTankBlockEntity> { level, _, _, be ->
            be.tick()
        }
    }
}