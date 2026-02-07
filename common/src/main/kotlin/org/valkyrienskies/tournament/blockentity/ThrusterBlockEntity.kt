package org.valkyrienskies.tournament.blockentity

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.IntTag
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.tournament.TournamentBlockEntities

// TODO

class ThrusterBlockEntity(pos: BlockPos, state: BlockState):
    BlockEntity(TournamentBlockEntities.THRUSTER.get(), pos, state)
{
    var fuelId: Int? = null
    var throttle = 0f

    override fun saveAdditional(tag: CompoundTag) {
        fuelId?.let { tag.putInt("fuel", it) }
        tag.putFloat("throttle", throttle)

        super.saveAdditional(tag)
    }

    override fun load(tag: CompoundTag) {
        fuelId = tag.get("throttle")?.let { (it as IntTag).asInt }
        throttle = tag.getFloat("throttle")

        super.load(tag)
    }

    override fun getUpdateTag(): CompoundTag =
        CompoundTag().also {
            saveAdditional(it)
        }

    override fun getUpdatePacket(): ClientboundBlockEntityDataPacket? =
        ClientboundBlockEntityDataPacket.create(this)
}
