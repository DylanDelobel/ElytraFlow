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

@Mod(value = "elytraflow", dist = Dist.CLIENT)
public class ElytraFlowNeoForge {

    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("elytraflow", "elytraflow"));

    private static KeyMapping toggleKey;
    private static boolean previousOnGround = true;

    public ElytraFlowNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::onRegisterKeyMappings);
        NeoForge.EVENT_BUS.register(this);
    }

    private void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        toggleKey = new KeyMapping(
                "key.elytraflow.toggle",
                InputConstants.Type.KEYSYM,
                InputConstants.UNKNOWN.getValue(),
                CATEGORY
        );
        event.register(toggleKey);
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Post event) {
        var client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null) return;

        while (toggleKey != null && toggleKey.consumeClick()) {
            ElytraFlowState.enabled = !ElytraFlowState.enabled;
            player.sendOverlayMessage(
                    Component.literal("ElytraFlow: " + (ElytraFlowState.enabled ? "Enabled" : "Disabled"))
            );
        }

        if (!ElytraFlowState.enabled) {
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
    }
}
