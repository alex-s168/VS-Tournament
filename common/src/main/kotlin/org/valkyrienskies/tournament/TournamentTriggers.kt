package org.valkyrienskies.tournament

import net.minecraft.advancements.CriteriaTriggers
import org.valkyrienskies.tournament.advancements.*
import net.minecraft.advancements.CriterionTrigger
import org.valkyrienskies.tournament.util.extension.once

object TournamentTriggers {

    private val all = ArrayList<CriterionTrigger<*>>()

    val SHIP_ASSEMBLY_TRIGGER = reg(ShipAssemblyTrigger())
    val BALLOON_SHOT_TRIGGER = reg(BalloonShotTrigger())

    private fun <T: CriterionTrigger<*>> reg (trigger: T): T {
        all.add(trigger)
        return trigger
    }

    val register by once {
        all.forEach {
            CriteriaTriggers.CRITERIA[it.id] = it
        }
    }

}