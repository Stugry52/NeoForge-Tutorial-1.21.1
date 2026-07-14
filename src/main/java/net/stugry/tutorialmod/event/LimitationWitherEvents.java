package net.stugry.tutorialmod.event;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import net.minecraft.world.level.block.state.pattern.BlockPatternBuilder;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.stugry.tutorialmod.TutorialMod;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = TutorialMod.MOD_ID)
public class LimitationWitherEvents {

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
