package net.stugry.tutorialmod.event;

import net.minecraft.client.model.WitherBossModel;
import net.minecraft.client.renderer.entity.WitherBossRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import net.minecraft.world.level.block.state.pattern.BlockPatternBuilder;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import net.stugry.tutorialmod.TutorialMod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import static net.minecraft.world.Containers.dropItemStack;

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

    // Перехват события появления визера в мире
//    @SubscribeEvent
//    public static void onWitherEntityJoin(EntityJoinLevelEvent event){
//        if (event.getEntity() instanceof WitherBoss witherBoss && !event.getLevel().isClientSide){
//            Level level = event.getLevel();
//
//            // Отменяем призыв визера
//            if (!level.dimension().equals(Level.NETHER)) {
//
//                BlockPos pos = witherBoss.blockPosition();
//
//                event.setCanceled(true);
//
//                // Выдаем, ввиде дропа, использованые предметы
//                dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(Items.WITHER_SKELETON_SKULL, 3));
//
//               BlockPos centerPos = pos.below();
//               BlockState centerState = level.getBlockState(centerPos);
//               ItemStack material = new ItemStack(Blocks.SOUL_SAND);
//               if (centerState.is(Blocks.SOUL_SOIL)){
//                   material = new ItemStack(Blocks.SOUL_SOIL);
//               }
//
//               material.setCount(4);
//                dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), material);
//
//                // Выводим текст игрокам вблизи призыва Визера
//                level.players().stream()
//                        .filter(p -> p.distanceToSqr(witherBoss.position()) < 25)
//                        .forEach(p -> {
//                            if (p instanceof ServerPlayer serverPlayer) {
//                                serverPlayer.displayClientMessage(
//                                        Component.literal("Ритуал не возможен вне Ада!"), true
//                                );
//                            }
//                        });
//
//                level.playSound(null, pos, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 0.5f, 0.5f);
//            }
//        }
//    }

    private static final Map<BlockPos, List<BlockState>> STRUCTURE_CACHE = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.RightClickBlock event){
        Level level = event.getLevel();
        if (level.isClientSide() || !event.getItemStack().is(Items.WITHER_SKELETON_SKULL)) return;

        BlockPos pos = event.getPos();
        BlockPattern pattern = WitherStructure.getOrCreateWitherBase();
        BlockPattern.BlockPatternMatch match = pattern.find(level, pos);

        if (match != null){
            List<BlockState> captured = new ArrayList<>();

            for (int i = 0; i < pattern.getWidth(); i++){
                for (int j = 0; j < pattern.getHeight(); j++){
                    BlockState state = match.getBlock(i, j, 0).getState();
                    if (state.is(BlockTags.WITHER_SUMMON_BASE_BLOCKS)){
                        captured.add(state);
                    }
                }
            }
            STRUCTURE_CACHE.put(pos, captured);
        }
    }

    @SubscribeEvent
    public static void onWitherSpawn(EntityJoinLevelEvent event){
        if (event.getEntity() instanceof WitherBoss witherBoss && !event.getLevel().isClientSide()){
            Level level = event.getLevel();
            if (level.dimension().equals(Level.NETHER)) return;

            BlockPos spawnPos = witherBoss.blockPosition();

            BlockPos key = STRUCTURE_CACHE.keySet().stream()
                    .filter(pos -> pos.distSqr(spawnPos) < 9)
                    .findFirst().orElse(null);

            if (key != null){
                event.setCanceled(true);
                List<BlockState> materials = STRUCTURE_CACHE.remove(key);

                for (BlockState state : materials){
                    level.addFreshEntity(new ItemEntity(level, spawnPos.getX() + 0.5, spawnPos.getY() + 0.5, spawnPos.getZ() + 0.5, new ItemStack(state.getBlock())));
                }

                level.addFreshEntity(new ItemEntity(level, spawnPos.getX() + 0.5, spawnPos.getY() + 1.0, spawnPos.getZ() + 0.5, new ItemStack(Items.WITHER_SKELETON_SKULL)));

                level.players().stream()
                        .filter(p -> p.distanceToSqr(witherBoss.position()) < 25)
                        .forEach(p -> {
                            if (p instanceof ServerPlayer serverPlayer) {
                                serverPlayer.displayClientMessage(
                                        Component.literal("Ритуал не возможен вне Ада!"), true
                                );
                            }
                        });

                level.playSound(null, spawnPos, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 0.5f, 0.5f);
            }
        }
    }

    private static class WitherStructure{
        private static BlockPattern witherBasePattern;

        private static final Predicate<BlockInWorld> IS_SOUL_BLOCK = blockInWorld -> {
            BlockState state = blockInWorld.getState();
            return state.is(Blocks.SOUL_SAND) || state.is(Blocks.SOUL_SOIL);
        };

        public static BlockPattern getOrCreateWitherBase(){
            if (witherBasePattern == null){
                witherBasePattern = BlockPatternBuilder.start()
                        .aisle("^^^", "###", "~#~")
                        .where('#', IS_SOUL_BLOCK)
                        .where('^', BlockInWorld.hasState(s -> s.is(Blocks.WITHER_SKELETON_SKULL) || s.is(Blocks.WITHER_SKELETON_WALL_SKULL)))
                        .where('~', BlockInWorld.hasState(s -> s.is(Blocks.AIR)))
                        .build();
            }
            return witherBasePattern;
        }
    }


}
