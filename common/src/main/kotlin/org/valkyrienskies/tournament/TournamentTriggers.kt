package org.valkyrienskies.tournament

import org.valkyrienskies.tournament.mixin.advancements.MixinCriteriaTriggers
import org.valkyrienskies.tournament.advancements.*
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.resources.ResourceLocation
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
            (MixinCriteriaTriggers.getCriteria() as HashMap<ResourceLocation, CriterionTrigger<*>>)[it.id] = it
        }
    }

}