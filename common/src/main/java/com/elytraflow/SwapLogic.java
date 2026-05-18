package com.elytraflow;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

public class SwapLogic {

    public static void tryWearElytra(Minecraft client) {
        Player player = client.player;
        if (player == null) return;

        int slot = findBestElytraSlot(player);
        if (slot == -1) return;

        performSwap(client, slot);

        client.player.startFallFlying();
        client.getConnection().send(new ServerboundPlayerCommandPacket(
                player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING
        ));
    }

    public static void tryWearChestplate(Minecraft client) {
        Player player = client.player;
        if (player == null) return;

        int slot = findBestChestplateSlot(player);
        if (slot == -1) return;

        performSwap(client, slot);
    }

    private static int findBestElytraSlot(Player player) {
        int bestSlot = -1;
        double bestScore = -1;

        for (int i : inventorySlots()) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            if (!stack.has(DataComponents.GLIDER)) continue;
            if (hasCurseOfBinding(player, stack)) continue;

            double score = scoreElytra(player, stack);
            if (score > bestScore) {
                bestScore = score;
                bestSlot = i;
            }
        }
        return bestSlot;
    }

    private static int findBestChestplateSlot(Player player) {
        int bestSlot = -1;
        double bestScore = -1;

        for (int i : inventorySlots()) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            if (!isChestEquippable(stack)) continue;
            if (hasCurseOfBinding(player, stack)) continue;

            double score = scoreChestplate(player, stack);
            if (score > bestScore) {
                bestScore = score;
                bestSlot = i;
            }
        }
        return bestSlot;
    }

    private static boolean isChestEquippable(ItemStack stack) {
        var equippable = stack.get(DataComponents.EQUIPPABLE);
        return equippable != null && equippable.slot() == EquipmentSlot.CHEST;
    }

    private static double scoreChestplate(Player player, ItemStack stack) {
        double armor = 0, toughness = 0;
        var modifiers = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (modifiers != null) {
            for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
                if (entry.slot().test(EquipmentSlot.CHEST)) {
                    var modifier = entry.modifier();
                    if (entry.attribute().equals(Attributes.ARMOR)) {
                        armor += modifier.amount();
                    } else if (entry.attribute().equals(Attributes.ARMOR_TOUGHNESS)) {
                        toughness += modifier.amount();
                    }
                }
            }
        }

        double protection = getEnchantLevel(player, stack, Enchantments.PROTECTION);
        double mending = getEnchantLevel(player, stack, Enchantments.MENDING);
        double unbreaking = getEnchantLevel(player, stack, Enchantments.UNBREAKING);
        boolean named = stack.getCustomName() != null;

        return armor + toughness + 2 * protection + 0.5 * mending + 0.08 * unbreaking + (named ? 0.25 : 0);
    }

    private static double scoreElytra(Player player, ItemStack stack) {
        double mending = getEnchantLevel(player, stack, Enchantments.MENDING);
        double unbreaking = getEnchantLevel(player, stack, Enchantments.UNBREAKING);
        return (mending * 3 + 1) + unbreaking;
    }

    private static boolean hasCurseOfBinding(Player player, ItemStack stack) {
        return getEnchantLevel(player, stack, Enchantments.BINDING_CURSE) > 0;
    }

    private static int getEnchantLevel(Player player, ItemStack stack, ResourceKey<Enchantment> key) {
        try {
            Holder<Enchantment> holder = player.level()
                    .registryAccess()
                    .lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(key);
            return EnchantmentHelper.getItemEnchantmentLevel(holder, stack);
        } catch (Exception e) {
            return 0;
        }
    }

    private static void performSwap(Minecraft client, int inventorySlot) {
        Player player = client.player;
        if (player == null) return;
        MultiPlayerGameMode gameMode = client.gameMode;
        if (gameMode == null) return;

        int containerId = player.inventoryMenu.containerId;
        int screenSlot = inventoryToScreenSlot(inventorySlot);
        if (screenSlot == -1) return;

        int chestScreenSlot = 6;

        gameMode.handleContainerInput(containerId, screenSlot, 0, ContainerInput.PICKUP, player);
        gameMode.handleContainerInput(containerId, chestScreenSlot, 0, ContainerInput.PICKUP, player);
        gameMode.handleContainerInput(containerId, screenSlot, 0, ContainerInput.PICKUP, player);
    }

    private static int inventoryToScreenSlot(int inventorySlot) {
        if (inventorySlot >= 0 && inventorySlot <= 8) {
            return 36 + inventorySlot;
        } else if (inventorySlot >= 9 && inventorySlot <= 35) {
            return inventorySlot;
        } else if (inventorySlot == 40) {
            return 45;
        }
        return -1;
    }

    private static int[] inventorySlots() {
        int[] slots = new int[37];
        for (int i = 0; i <= 35; i++) slots[i] = i;
        slots[36] = 40;
        return slots;
    }
}
