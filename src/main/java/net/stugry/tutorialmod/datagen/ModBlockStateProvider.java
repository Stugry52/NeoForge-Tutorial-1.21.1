package net.stugry.tutorialmod.datagen;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.stugry.tutorialmod.TutorialMod;
import net.stugry.tutorialmod.block.ModBlocks;

public class ModBlockStateProvider extends BlockStateProvider {
    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, TutorialMod.MOD_ID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        blockWithItem(ModBlocks.BISMUTH_BLOCK);

        blockWithItem(ModBlocks.BISMUTH_ORE);
        blockWithItem(ModBlocks.BISMUTH_DEEPSLATE_ORE);

        blockWithItem(ModBlocks.MAGIC_BLOCK);
    }

    private void blockWithItem(DeferredBlock<?> deferredBlock){
        simpleBlockWithItem(deferredBlock.get(), cubeAll(deferredBlock.get()));
    }
}
