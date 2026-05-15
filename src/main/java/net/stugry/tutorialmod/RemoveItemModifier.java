package net.stugry.tutorialmod;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

public class RemoveItemModifier extends LootModifier {

    public static final MapCodec<RemoveItemModifier> CODEC = RecordCodecBuilder.mapCodec(inst ->
            LootModifier.codecStart(inst).and(
                    BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(m -> m.itemToRemove)
            ).apply(inst, RemoveItemModifier::new));

    private final Item itemToRemove;

    public RemoveItemModifier(LootItemCondition[] conditionsIn, Item item){
        super(conditionsIn);
        this.itemToRemove = item;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        int removeCount = generatedLoot.size();
        generatedLoot.removeIf(stack -> stack.is(this.itemToRemove));


        if (removeCount > generatedLoot.size()){
            System.out.println("DEBUG: Book was remove");
        }
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
