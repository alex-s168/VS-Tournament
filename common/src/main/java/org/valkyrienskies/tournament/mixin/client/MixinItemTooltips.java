package org.valkyrienskies.tournament.mixin.client;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.tournament.TournamentEvents;

import java.util.List;

@Mixin(Item.class)
public abstract class MixinItemTooltips {
    @Inject(method = "appendHoverText", at = @At("HEAD"))
    private void ValkyrienTournament$addMassToTooltip(
            final ItemStack itemStack,
            final Level level,
            final List<Component> list,
            final TooltipFlag tooltipFlag,
            final CallbackInfo ci
    ) {
        TournamentEvents.itemHoverText.emit(new TournamentEvents.ItemHoverText(
            itemStack,
            level,
            list,
            tooltipFlag
        ));
    }
}
