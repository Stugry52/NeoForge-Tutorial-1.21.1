package net.stugry.tutorialmod.event;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import net.stugry.tutorialmod.TutorialMod;

@EventBusSubscriber(modid = TutorialMod.MOD_ID)
public class EncoreEvents {

    // Убирает у библиотекарей возможность продавать зачарованные книги
    @SubscribeEvent
    public static void onVillagerTrades(VillagerTradesEvent event) {
        if (event.getType().equals(VillagerProfession.LIBRARIAN)) {
            event.getTrades().forEach((level, trades) -> {
                trades.removeIf(trade -> {
                    String className = trade.getClass().getSimpleName();

                    return className.contains("EnchantBookForEmerald");
                });
            });
        }
    }

    // Когда пытаеся сделать стол зачарования, убирает из готового крафта заменяет на пустую ячейку
    @SubscribeEvent
    public static void onContainerUpdate(PlayerTickEvent.Post event){
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

    // При загрузке мира убирает все столы зачарования
    @SubscribeEvent
    public static void onLevelLoad(BlockEvent.NeighborNotifyEvent event){
        if (event.getState().is(Blocks.ENCHANTING_TABLE)){
            event.getLevel().setBlock(event.getPos(), Blocks.AIR.defaultBlockState(), 3);
        }
    }
    // Если игрок ставит стол зачарования, он сразу пропадает
    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event){
        if (event.getPlacedBlock().is(Blocks.ENCHANTING_TABLE)){
            event.setCanceled(true);
        }
    }

    // Когда игрок в режим приключения или выживания поднимает предмет с него пропадают все чары
    @SubscribeEvent
    public static void onItemPickup(ItemEntityPickupEvent.Pre event){
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

    // Когда игрок открывает меню сундука или торговое меню предметы в нем теряю чары
    @SubscribeEvent
    public static void onPlayerContainerTick(PlayerTickEvent.Post event){
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

    // Когда игрок в режиме приключения или выживания выкидывает предмет он теряет чары
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event){
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
