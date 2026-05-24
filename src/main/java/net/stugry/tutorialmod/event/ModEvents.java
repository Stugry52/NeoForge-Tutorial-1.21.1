package net.stugry.tutorialmod.event;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.stugry.tutorialmod.TutorialMod;
import net.stugry.tutorialmod.item.custom.HammerItem;

import java.util.HashSet;
import java.util.Set;

@EventBusSubscriber(modid = TutorialMod.MOD_ID)
public class ModEvents {
    private static final Set<BlockPos> HARVEST_BLOCKS = new HashSet<>();

    @SubscribeEvent
    public static void onHammerUsage(BlockEvent.BreakEvent event){
        Player player = event.getPlayer();
        ItemStack mainHandItem = player.getMainHandItem();

        if(mainHandItem.getItem() instanceof HammerItem hammer && player instanceof ServerPlayer serverPlayer){
            BlockPos initialBlock = event.getPos();
            if (HARVEST_BLOCKS.contains(initialBlock)){
                return;
            }

            for (BlockPos pos : HammerItem.getBlocksToBeDestroyed(1, initialBlock, serverPlayer)){
                if (pos == initialBlock || !hammer.isCorrectToolForDrops(mainHandItem, event.getLevel().getBlockState(pos))){
                    continue;
                }

                HARVEST_BLOCKS.add(pos);
                serverPlayer.gameMode.destroyBlock(pos);
                HARVEST_BLOCKS.remove(pos);
            }
        }
    }


}
