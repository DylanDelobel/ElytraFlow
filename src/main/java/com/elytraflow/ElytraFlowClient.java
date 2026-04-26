package com.elytraflow;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;

public class ElytraFlowClient implements ClientModInitializer {

    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("elytraflow", "elytraflow"));

    public static KeyMapping toggleKey;
    public static boolean enabled = true;
    private static boolean previousOnGround = true;

    @Override
    public void onInitializeClient() {
        toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.elytraflow.toggle",
                InputConstants.Type.KEYSYM,
                InputConstants.UNKNOWN.getValue(),
                CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            LocalPlayer player = client.player;
            if (player == null) return;

            while (toggleKey.consumeClick()) {
                enabled = !enabled;
                player.sendOverlayMessage(
                        Component.literal("ElytraFlow: " + (enabled ? "Enabled" : "Disabled"))
                );
            }

            if (!enabled) {
                previousOnGround = true;
                return;
            }

            boolean onGround = player.onGround();
            boolean inWater = player.isInWater();
            var chestStack = player.getItemBySlot(EquipmentSlot.CHEST);

            if (onGround && !inWater && !previousOnGround && chestStack.has(DataComponents.GLIDER)) {
                SwapLogic.tryWearChestplate(client);
            }

            previousOnGround = onGround;
        });
    }
}
