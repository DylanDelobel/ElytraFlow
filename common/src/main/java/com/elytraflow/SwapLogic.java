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



    // chest slot in the player inventory screen (0 = crafting out, 1-4 grid, 5 helmet, 6 chest)
    private static final int CHEST_SLOT = 6;

    //hotbar + main inv, then 40 = offhand. armor slots skipped on purpose
    private static final int[]SLOTS =new int[37];
    static {
        for (int i = 0; i <= 35; i++) SLOTS[i] = i;
        SLOTS[36] = 40;
    }



    public static void tryWearElytra(Minecraft mc) {
        Player player = mc.player;
        if (player == null)return;

        int slot = bestElytraSlot(player);
        if (slot == -1) return;

        swapIntoChest(mc,slot);

        //server wont start the glide on its own after the swap, have to tell it
        mc.player.startFallFlying();
        mc.getConnection().send(new ServerboundPlayerCommandPacket(
                player,ServerboundPlayerCommandPacket.Action.START_FALL_FLYING
        ));

    }

    public static void tryWearChestplate(Minecraft mc) {
        Player player=mc.player;
        if (player == null)return;

        int slot = bestChestplateSlot(player);
        if (slot == -1) return;
        swapIntoChest(mc, slot);
    }

    private static int bestElytraSlot(Player player) {
        int best =-1;
        double bestScore = -1;

        for(int i : SLOTS){
            ItemStack stack=player.getInventory().getItem(i);
            if (stack.isEmpty() || !stack.has(DataComponents.GLIDER)) continue;
            if(isBound(player,stack))continue;// cant take it off again, skip

            double score = elytraScore(player, stack);
            if(score > bestScore) {
                bestScore= score;
                best=i;
            }


        }
        return best;
    }

    // same deal as above, kept seperate since the filters drift apart every update
    private static int bestChestplateSlot(Player player) {
        int best=-1;
        double bestScore=-1;

        for (int i : SLOTS) {
            ItemStack stack =player.getInventory().getItem(i);
            if(stack.isEmpty()|| !goesInChest(stack)) continue;
            if(isBound(player, stack))continue;

            double score = chestplateScore(player, stack);
            if (score > bestScore) {
                bestScore=score;
                best = i;


            }
        }
        return best;
    }

    private static boolean goesInChest(ItemStack stack) {
        var eq=stack.get(DataComponents.EQUIPPABLE);
        return eq != null && eq.slot() == EquipmentSlot.CHEST;
    }

    private static double chestplateScore(Player player, ItemStack stack){
        double armor= 0,toughness = 0;
        var mods = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (mods != null){
            for (ItemAttributeModifiers.Entry e : mods.modifiers()) {
                if(!e.slot().test(EquipmentSlot.CHEST))continue;
                if (e.attribute().equals(Attributes.ARMOR)) {
                    armor += e.modifier().amount();
                }else if(e.attribute().equals(Attributes.ARMOR_TOUGHNESS)) {
                    toughness += e.modifier().amount();
                }

            }
        }

        double prot =enchLevel(player, stack,Enchantments.PROTECTION);
        double mending= enchLevel(player, stack, Enchantments.MENDING);
        double unbreaking = enchLevel(player, stack,Enchantments.UNBREAKING);
        // named = probably the players "good" one, small tiebreak
        boolean named= stack.getCustomName()!= null;

        // weights picked by feel, prot matters most. tried 1.0 for mending, too strong
        return armor + toughness + 2 * prot + 0.5 * mending + 0.08 * unbreaking + (named ? 0.25 : 0);
    }


    private static double elytraScore(Player player,ItemStack stack) {
        double mending=enchLevel(player, stack, Enchantments.MENDING);
        double unbreaking = enchLevel(player,stack, Enchantments.UNBREAKING);
        return 1 + mending * 3 + unbreaking; //mending is basically mandatory on elytra
    }

    private static boolean isBound(Player player, ItemStack stack){
        return enchLevel(player,stack,Enchantments.BINDING_CURSE) > 0;
    }

    private static int enchLevel(Player player,ItemStack stack,ResourceKey<Enchantment> key){
        try {
            Holder<Enchantment> ench = player.level()
                    .registryAccess()
                    .lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(key);


            return EnchantmentHelper.getItemEnchantmentLevel(ench, stack);
        } catch (Exception e) {
            // datapacks can yank vanilla enchants out of the registry , just treat as 0
            return 0;
        }
    }

    // pick up item, click chest slot (swaps), put old chest item back where it came from
    private static void swapIntoChest(Minecraft mc, int invSlot) {
        Player player = mc.player;
        if (player == null) return;
        MultiPlayerGameMode gm =mc.gameMode;
        if (gm == null) return;



        int id = player.inventoryMenu.containerId;

        int from = toScreenSlot(invSlot);
        if (from == -1) return;

        gm.handleContainerInput(id, from, 0, ContainerInput.PICKUP, player);
        gm.handleContainerInput(id,CHEST_SLOT,0, ContainerInput.PICKUP,player);
        gm.handleContainerInput(id, from, 0, ContainerInput.PICKUP, player);


        // System.out.println("swapped " + invSlot + " -> " + from);
    }

    // inventory index -> InventoryMenu slot. hotbar lives at the bottom (36-44), offhand is 45
    private static int toScreenSlot(int invSlot) {
        if(invSlot >= 0 && invSlot <= 8) return 36 + invSlot;
        if (invSlot >= 9 && invSlot <= 35)return invSlot;
        if (invSlot == 40) return 45;
        return -1;
    }
}

