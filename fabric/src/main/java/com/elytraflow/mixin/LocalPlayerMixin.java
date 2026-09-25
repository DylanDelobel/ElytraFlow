package com.elytraflow.mixin;

import com.elytraflow.ElytraFlowState;
import com.elytraflow.SwapLogic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import org.spongepowered.asm.mixin.Mixin;


import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {


    // hook right after vanilla checks the jump key mid air, thats the "wants to glide" moment
    @Inject(
        method= "aiStep",
        at=@At(value = "INVOKE",
                 target = "Lnet/minecraft/client/player/LocalPlayer;tryToStartFallFlying()Z",
                 shift= At.Shift.AFTER))
    private void elytraflow$autoSwap(CallbackInfo ci) {
        if (!ElytraFlowState.enabled)return;
        LocalPlayer p = (LocalPlayer)(Object)this;
        //levitation was triggering swaps while floating up, skip it
        if (p.onGround() || p.isFallFlying() || p.isInWater()
                || p.hasEffect(MobEffects.LEVITATION))return;
        if (p.getItemBySlot(EquipmentSlot.CHEST).has(DataComponents.GLIDER)) return; //already wearing one


        SwapLogic.tryWearElytra(Minecraft.getInstance());
    }
}
