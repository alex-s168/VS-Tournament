package org.valkyrienskies.tournament.blockentity

import blitz.Provider
import blitz.toBool
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NumericTag
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.mod.common.util.toJOMLD
import org.valkyrienskies.tournament.TournamentBlockEntities
import org.valkyrienskies.tournament.TournamentProperties
import org.valkyrienskies.tournament.ship.TournamentShips
import org.valkyrienskies.tournament.util.extension.toBlock
import org.valkyrienskies.tournament.util.helper.convertShipToWorldSpace

class ThrusterBlockEntity(
    pos: BlockPos,
    state: BlockState,
    private val multiplier: Provider<Float> = {0f},
): BlockEntity(TournamentBlockEntities.THRUSTER.get(), pos, state)
{
    @JvmField var throttle = 0f
    @JvmField var throttleForced = false
    @JvmField var actualThrottle = 0f
    @JvmField var fuelTypeCompactId: Int? = null

    @JvmField var lastActualThrottle = 0f

    private fun tick(level: ServerLevel) {
        val controller = TournamentShips.getOrCreate(level, worldPosition) ?: return
        val thruster = controller.thrusterV2(worldPosition)!!
        val state = level.getBlockState(worldPosition)!!
        val signal = level.getBestNeighborSignal(worldPosition)

        if (!throttleForced) {
            throttle = state.getValue(TournamentProperties.TIER).toFloat() *
                    (signal.toFloat() / 15f) *
                    multiplier()
        }

        val water = level.isWaterAt(
            level.convertShipToWorldSpace(worldPosition.toJOMLD()).toBlock())

        if (water) {
            thruster.throttle = 0f
        } else {
            thruster.throttle = throttle
        }

        thruster.throttle *= controller.fuelType?.fuel?.getBurnRate(throttle)?.let(controller::useFuel) ?: 0f

        actualThrottle = thruster.throttle
        if (actualThrottle != lastActualThrottle || fuelTypeCompactId != controller.fuelType?.compactId) {
            lastActualThrottle = actualThrottle
            fuelTypeCompactId = controller.fuelType?.compactId
            level.sendBlockUpdated(blockPos, blockState, blockState, Block.UPDATE_CLIENTS)
        }
    }

    override fun saveAdditional(tag: CompoundTag) {
        tag.putFloat("throttle", throttle)
        tag.putBoolean("throttle_forced", throttleForced)
        tag.putFloat("actual_throttle", actualThrottle)
        fuelTypeCompactId?.let {
            tag.putInt("fuel_type", it)
        }

        super.saveAdditional(tag)
    }

    override fun load(tag: CompoundTag) {
        throttle = tag.get("throttle")?.let { (it as NumericTag).asFloat } ?: 0f
        throttleForced = tag.get("throttle_forced")?.let { (it as NumericTag).asByte.toBool() } ?: false
        actualThrottle = tag.get("actual_throttle")?.let { (it as NumericTag).asFloat } ?: 0f
        fuelTypeCompactId = tag.get("fuel_type")?.let { (it as NumericTag).asInt } ?: 0

        super.load(tag)
    }

    override fun getUpdateTag(): CompoundTag =
        CompoundTag().also {
            saveAdditional(it)
        }

    override fun getUpdatePacket(): ClientboundBlockEntityDataPacket? =
        ClientboundBlockEntityDataPacket.create(this)

    companion object {
        val ticker = BlockEntityTicker<ThrusterBlockEntity> { level, _, _, be ->
            if (level !is ServerLevel)
                return@BlockEntityTicker
            be.tick(level)
        }
    }
}
