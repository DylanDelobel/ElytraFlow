package com.elytraflow.fabric;

import com.elytraflow.ElytraFlowState;
import com.elytraflow.SwapLogic;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.LocalPlayer;


import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;


public class ElytraFlowFabric implements ClientModInitializer{

    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("elytraflow", "elytraflow"));

    public static KeyMapping toggleKey;
    private static boolean wasOnGround = true;

    @Override
    public void onInitializeClient() {


        // unbound by default, too many mods fighting over keys already
        toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.elytraflow.toggle",
                InputConstants.Type.KEYBOARD,
                InputConstants.UNKNOWN.getValue(),
                CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            LocalPlayer player= client.player;
            if (player == null) return;

            while (toggleKey.consumeClick()) {
                ElytraFlowState.enabled= !ElytraFlowState.enabled;


                player.sendOverlayMessage(
                        Component.literal("ElytraFlow: " + (ElytraFlowState.enabled ? "Enabled" : "Disabled"))
                );
            }

            if (!ElytraFlowState.enabled){
                wasOnGround = true; //so re-enabling mid air doesnt instantly swap
                return;
            }

            boolean onGround=player.onGround();
            //only on the landing tick, not every tick standing around
            if (onGround && !player.isInWater()&& !wasOnGround
                    && player.getItemBySlot(EquipmentSlot.CHEST).has(DataComponents.GLIDER)) {

                SwapLogic.tryWearChestplate(client);
            }

            wasOnGround = onGround;
        });
    }
}
