package net.stugry.tutorialmod.item;

import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.SimpleTier;
import net.stugry.tutorialmod.util.ModTags;

public class ModToolTiers {
    public static final Tier BISMUTH = new SimpleTier(ModTags.Blocks.INCORRECT_FOR_BISMUTH_TOOL,
            1400, 4f, 3f, 28, () -> Ingredient.of(ModItems.BISMUTH));
}
