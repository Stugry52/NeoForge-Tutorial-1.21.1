package net.stugry.tutorialmod.util;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.stugry.tutorialmod.TutorialMod;
import net.stugry.tutorialmod.component.ModDataComponents;
import net.stugry.tutorialmod.item.ModItems;

public class ModItemsProperties {
    public static void addCustomItemProperties(){
        ItemProperties.register(ModItems.CHISEL.get(), ResourceLocation.fromNamespaceAndPath(TutorialMod.MOD_ID, "used"),
                ((stack, level, entity, seed) -> stack.get(ModDataComponents.COORDINATES) != null ? 1f : 0f));
    }
}
