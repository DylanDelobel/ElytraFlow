package com.elytraflow.neoforge;

import com.elytraflow.ElytraFlowState;
import com.elytraflow.SwapLogic;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;


import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;


import net.neoforged.neoforge.common.NeoForge;

@Mod(value= "elytraflow", dist =Dist.CLIENT)
public class ElytraFlowNeoForge{

    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("elytraflow","elytraflow"));

    private static KeyMapping toggleKey;
    private static boolean wasOnGround =true;

    public ElytraFlowNeoForge(IEventBus modBus, ModContainer container) {


        modBus.addListener(this::registerKeys);
        NeoForge.EVENT_BUS.register(this); // for the tick handler below
    }

    private void registerKeys(RegisterKeyMappingsEvent event){
        toggleKey =new KeyMapping(
                "key.elytraflow.toggle",
                InputConstants.Type.KEYBOARD,
                InputConstants.UNKNOWN.getValue(),
                CATEGORY
        );
        event.register(toggleKey);
    }

    //same logic as the fabric tick callback
    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Post event) {

        var client=Minecraft.getInstance();
        LocalPlayer player=client.player;
        if (player == null) return;

        //null check bc ticks can fire before key registration on neo
        while (toggleKey != null && toggleKey.consumeClick()) {
            ElytraFlowState.enabled = !ElytraFlowState.enabled;
            player.sendOverlayMessage(
                    Component.literal("ElytraFlow: " + (ElytraFlowState.enabled ? "Enabled" : "Disabled"))
            );
        }

        if (!ElytraFlowState.enabled){
            wasOnGround = true;
            return;
        }

        boolean onGround = player.onGround();
        if(onGround && !player.isInWater() && !wasOnGround
                && player.getItemBySlot(EquipmentSlot.CHEST).has(DataComponents.GLIDER)) {
            SwapLogic.tryWearChestplate(client);// landed, put the chestplate back on
        }

        wasOnGround = onGround;
    }
}
