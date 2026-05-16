package net.stugry.tutorialmod;

import com.google.common.collect.ImmutableListMultimap;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import net.neoforged.neoforge.registries.*;
import net.stugry.tutorialmod.block.ModBlocks;
import net.stugry.tutorialmod.item.ModCreativeModeTabs;
import net.stugry.tutorialmod.item.ModItems;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.event.village.VillagerTradesEvent.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(TutorialMod.MOD_ID)
public class TutorialMod {
    public static final String MOD_ID = "tutorialmod";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIER_SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, TutorialMod.MOD_ID);
    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<RemoveItemModifier>> REMOVE_ITEM =
            LOOT_MODIFIER_SERIALIZERS.register("remove_item", () -> RemoveItemModifier.CODEC);

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public TutorialMod(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (ExampleMod) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

        ModCreativeModeTabs.register(modEventBus);

        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);

        LOOT_MODIFIER_SERIALIZERS.register(modEventBus);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {

    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ModItems.BISMUTH);
            event.accept(ModItems.RAW_BISMUTH);
        }
        if (event.getTabKey().equals(CreativeModeTabs.BUILDING_BLOCKS)) {
            event.accept(ModBlocks.BISMUTH_BLOCK);
            event.accept(ModBlocks.BISMUTH_ORE);
        }
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {

    }

    @SubscribeEvent
    public void onVillagerTrades(VillagerTradesEvent event) {
        if (event.getType().equals(VillagerProfession.LIBRARIAN)) {
            event.getTrades().forEach((level, trades) -> {
                trades.removeIf(trade -> {
                    String className = trade.getClass().getSimpleName();

                    return className.contains("EnchantBookForEmerald");
                });
            });
        }
    }

    @SubscribeEvent
    public void onContainerUpdate(PlayerTickEvent.Post event){
        var player = event.getEntity();

        if (player != null && !player.level().isClientSide()){
            var menu = player.containerMenu;
            if (menu != null){
                try {
                    var outputSlot = menu.getSlot(0);
                    if (outputSlot != null && outputSlot.hasItem()){
                        var craftedItem = outputSlot.getItem();

                        if (craftedItem.is(Items.ENCHANTING_TABLE)){
                            outputSlot.set(ItemStack.EMPTY);

                            menu.broadcastChanges();
                        }
                    }
                }catch (Exception e){

                }
            }
        }
    }

    @SubscribeEvent
    public void onLevelLoad(BlockEvent.NeighborNotifyEvent event){
        if (event.getState().is(Blocks.ENCHANTING_TABLE)){
            event.getLevel().setBlock(event.getPos(), Blocks.AIR.defaultBlockState(), 3);
        }
    }
    @SubscribeEvent
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event){
        if (event.getPlacedBlock().is(Blocks.ENCHANTING_TABLE)){
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onItemPickup(ItemEntityPickupEvent.Pre event){
        var player = event.getPlayer();
        if (player != null && !player.level().isClientSide() && !player.isCreative() && !player.isSpectator()){
            var itemEntity = event.getItemEntity();
            if (itemEntity != null) {

                var stack = itemEntity.getItem();

                if (!stack.isEmpty()) {
                    stack.remove(DataComponents.ENCHANTMENTS);
                    stack.remove(DataComponents.STORED_ENCHANTMENTS);
                }
            }
        }
    }

    @SubscribeEvent
    public void onPlayerContainerTick(PlayerTickEvent.Post event){
        var player = event.getEntity();

        if (player != null && !player.level().isClientSide() && !player.isCreative() && !player.isSpectator()){
            var menu = player.containerMenu;

            if (menu != null){
                if (menu instanceof ChestMenu chestMenu){
                    var container = chestMenu.getContainer();
                    for (int i = 0; i < container.getContainerSize(); i++){
                        var stack = container.getItem(i);
                        if (!stack.isEmpty()){
                            stack.remove(DataComponents.ENCHANTMENTS);
                            stack.remove(DataComponents.STORED_ENCHANTMENTS);
                        }
                    }
                }
                if (menu instanceof MerchantMenu merchantMenu){
                    for (MerchantOffer offer : merchantMenu.getOffers()){
                        var result = offer.getResult();
                        if (!result.isEmpty()){
                            result.remove(DataComponents.ENCHANTMENTS);
                            result.remove(DataComponents.STORED_ENCHANTMENTS);
                        }
                    }
                }
            }

        }
    }

    @SubscribeEvent
    public void onEntityJoinLevel(EntityJoinLevelEvent event){

        if (!event.getLevel().isClientSide() && event.getEntity() instanceof ItemEntity itemEntity){
            var owner = itemEntity.getOwner();

            if (owner instanceof Player player && (player.isCreative() || player.isSpectator())){
                return;
            }
            var stack = itemEntity.getItem();
            if (!stack.isEmpty()){
                stack.remove(DataComponents.ENCHANTMENTS);
                stack.remove(DataComponents.STORED_ENCHANTMENTS);
            }
        }
    }

}
