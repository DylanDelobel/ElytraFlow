package com.elytraflow.mixin;

import com.elytraflow.ElytraFlowClient;
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

    @Inject(
        method = "aiStep",
        at = @At(value = "INVOKE",
                 target = "Lnet/minecraft/client/player/LocalPlayer;tryToStartFallFlying()Z",
                 shift = At.Shift.AFTER))
    private void elytraflow$autoSwap(CallbackInfo ci) {
        if (!ElytraFlowClient.enabled) return;
        LocalPlayer self = (LocalPlayer)(Object)this;
        if (self.onGround() || self.isFallFlying() || self.isInWater()
                || self.hasEffect(MobEffects.LEVITATION)) return;
        if (self.getItemBySlot(EquipmentSlot.CHEST).has(DataComponents.GLIDER)) return;
        SwapLogic.tryWearElytra(Minecraft.getInstance());
    }
}
