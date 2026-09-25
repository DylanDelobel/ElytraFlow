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

// copy of the fabric one, keep them in sync
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin{


    @Inject(
        method ="aiStep",
        at =@At(value= "INVOKE",
                 target ="Lnet/minecraft/client/player/LocalPlayer;tryToStartFallFlying()Z",
                 shift = At.Shift.AFTER))
    private void elytraflow$autoSwap(CallbackInfo ci) {
        if (!ElytraFlowState.enabled) return;
        LocalPlayer p= (LocalPlayer)(Object)this;
        if (p.onGround() || p.isFallFlying() || p.isInWater()
                || p.hasEffect(MobEffects.LEVITATION)) return; //no point swapping here
        if(p.getItemBySlot(EquipmentSlot.CHEST).has(DataComponents.GLIDER))return;


        SwapLogic.tryWearElytra(Minecraft.getInstance());
    }
}
