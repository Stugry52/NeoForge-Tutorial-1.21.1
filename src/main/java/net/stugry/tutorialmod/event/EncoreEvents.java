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
import net.minecraft.world.InteractionResult;
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
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
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



    // Полу патерн визера для проверки
    public static BlockPattern WITHER_PATTERN = BlockPatternBuilder.start()
            .aisle("###", "~#~")
            .where('#', BlockInWorld.hasState(s -> s.is(BlockTags.WITHER_SUMMON_BASE_BLOCKS)))
            .where('~', BlockInWorld.hasState(s -> s.is(Blocks.AIR)))
            .build();

    // Время последней смерти визера
    private static long lastWitherDeathTime = -1800 * 20;

    // Перехват события появления визера в мире
    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.RightClickBlock event){
        Level level = event.getLevel();
        if(level.isClientSide() || !event.getItemStack().is(Items.WITHER_SKELETON_SKULL)) return;

        // Проверяем в каком мире находимся в момент попытки призыва

        BlockPos pos = event.getPos();
        BlockState posState = event.getLevel().getBlockState(pos);

        BlockPos anchorPos = pos;

        if (posState.is(BlockTags.WITHER_SUMMON_BASE_BLOCKS)){
            anchorPos = pos;
        } else {
            BlockPos below = pos.below();
            if (level.getBlockState(below).is(BlockTags.WITHER_SUMMON_BASE_BLOCKS)){
                anchorPos = below;
            }
        }


        // Записывыем позицию по заготовленному полу-патерну визера
        BlockPattern.BlockPatternMatch match = WITHER_PATTERN.find(level, anchorPos.offset(0, 0, 0));
        //List<BlockPos> structureBlocks = getWitherStructureBlocks(level, pos);

        // Проверяем на совпадения по патерну, если match что-то нашел
        if (match == null) return;

        if (level.dimension().equals(Level.NETHER)) {

            long currentTime = level.getGameTime();
            long cooldownTicks = 1800 * 20;

            if (lastWitherDeathTime != -1 && (currentTime - lastWitherDeathTime) < cooldownTicks) {
                long remainingSeconds = (cooldownTicks - (currentTime - lastWitherDeathTime)) / 20;
                event.setCanceled(true);
                if (event.getEntity() instanceof ServerPlayer sp) {
                    sp.displayClientMessage(Component.literal("§cРитуал на перезарядке: " + remainingSeconds + " секунд"), true);
                }
                return;
            }
            boolean witherExists = level.getEntitiesOfClass(WitherBoss.class, level.players().get(0).getBoundingBox().inflate(1000)).size() > 0;

            if (witherExists) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.FAIL);

                if (event.getEntity() instanceof ServerPlayer sp) {
                    sp.displayClientMessage(Component.literal("§cВ Аду уже есть активный Иссушитель!"), true);
                }
                return;
            }
        } else {
            // Отменяем призыв визера
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);

            // Используем метод с записаными блоками
            destroyAndDrop(level, match);

            // Выводим текст игрокам вблизи призыва Визера
            if (event.getEntity() instanceof ServerPlayer sp) {
                sp.displayClientMessage(Component.literal("§cРитуал не возможен вне Ада!"), true);
            }

            // Звук не удачи, для красоты
            level.playSound(null, pos, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 0.5f, 0.5f);
        }

    }

    // Перехват для обновления таймера с последней смерти визера
    @SubscribeEvent
    public static void onWitherDeath(LivingDeathEvent event){
        if (event.getEntity() instanceof WitherBoss && event.getEntity().level().dimension().equals(Level.NETHER)){
            lastWitherDeathTime = event.getEntity().level().getGameTime();
        }
    }

    // Полный метод для уничтожения и выброса блоков
    private static void destroyAndDrop(Level level, BlockPattern.BlockPatternMatch match){
        List<ItemStack> drops = new ArrayList<>();
        drops.add(new ItemStack(Items.WITHER_SKELETON_SKULL, 1));

        // Перебераем пространство вокруг патерна и записываем в список наши блоки
        // вопрос за 4х4х4 поиск
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                for (int z = 0; z < 3; z++) {
                    BlockInWorld block = match.getBlock(x, y, z);
                    BlockState state = block.getState();
                    if (state.is(BlockTags.WITHER_SUMMON_BASE_BLOCKS)) {
                        // Добавляем блоки в список
                        drops.add(new ItemStack(state.getBlock()));
                        // Уничтожаем записаные блоки
                        level.destroyBlock(block.getPos(), false);
                    }
                }
            }
        }

        // Выдаем, ввиде дропа, использованые предметы
        BlockPos center = match.getBlock(1, 1, 0).getPos();
        for (ItemStack stack : drops) {
            level.addFreshEntity(new ItemEntity(level, center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5, stack));

        }
    }
}
