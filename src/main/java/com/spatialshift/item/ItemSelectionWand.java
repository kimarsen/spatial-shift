package com.spatialshift.item;

import com.spatialshift.SpatialShift;
import com.spatialshift.capability.IPlayerSelection;
import com.spatialshift.capability.PlayerSelection;
import com.spatialshift.data.WandMode;
import com.spatialshift.network.PacketHandler;
import com.spatialshift.network.packet.PacketSyncSelection;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

@Mod.EventBusSubscriber(modid = SpatialShift.MODID)
public class ItemSelectionWand extends Item {

    public ItemSelectionWand() {
        setRegistryName("selection_wand");
        setTranslationKey(SpatialShift.MODID + ".selection_wand");
        setMaxStackSize(1);
        setCreativeTab(SpatialShift.CREATIVE_TAB);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (player.isSneaking()) {
            if (!world.isRemote) {
                IPlayerSelection selection = player.getCapability(PlayerSelection.CAPABILITY, null);
                if (selection != null) {
                    WandMode nextMode = selection.getMode().next();
                    selection.setMode(nextMode);
                    syncSelection(player, selection);

                    TextComponentTranslation msg = new TextComponentTranslation(
                        "message.spatialshift.mode_changed",
                        TextFormatting.LIGHT_PURPLE + nextMode.name()
                    );
                    player.sendStatusMessage(msg, true);
                }
            }
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }
        return new ActionResult<>(EnumActionResult.PASS, stack);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (player.isSneaking()) {
            return EnumActionResult.PASS;
        }

        if (!world.isRemote) {
            IPlayerSelection selection = player.getCapability(PlayerSelection.CAPABILITY, null);
            if (selection != null) {
                if (selection.getMode() == WandMode.BOX) {
                    selection.setSecondaryPos(pos);
                    selection.rebuildFromBox();
                    notifyPos(player, "message.spatialshift.pos2_set", pos, selection.getSelectedPositions().size());
                } else {
                    if (selection.containsPosition(pos)) {
                        selection.removePosition(pos);
                        notifyPos(player, "message.spatialshift.block_removed", pos, selection.getSelectedPositions().size());
                    } else {
                        selection.addPosition(pos);
                        notifyPos(player, "message.spatialshift.block_added", pos, selection.getSelectedPositions().size());
                    }
                }
                syncSelection(player, selection);
            }
        }
        return EnumActionResult.SUCCESS;
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        EntityPlayer player = event.getEntityPlayer();
        ItemStack held = player.getHeldItemMainhand();
        if (held.getItem() instanceof ItemSelectionWand) {
            if (!event.getWorld().isRemote) {
                IPlayerSelection selection = player.getCapability(PlayerSelection.CAPABILITY, null);
                if (selection != null) {
                    if (selection.getMode() == WandMode.BOX) {
                        selection.setPrimaryPos(event.getPos());
                        selection.rebuildFromBox();
                        notifyPos(player, "message.spatialshift.pos1_set", event.getPos(), selection.getSelectedPositions().size());
                    } else {
                        if (selection.containsPosition(event.getPos())) {
                            selection.removePosition(event.getPos());
                            notifyPos(player, "message.spatialshift.block_removed", event.getPos(), selection.getSelectedPositions().size());
                        } else {
                            selection.addPosition(event.getPos());
                            notifyPos(player, "message.spatialshift.block_added", event.getPos(), selection.getSelectedPositions().size());
                        }
                    }
                    syncSelection(player, selection);
                }
            }
            event.setUseBlock(Event.Result.DENY);
            event.setUseItem(Event.Result.DENY);
            event.setCanceled(true);
        }
    }

    private static void notifyPos(EntityPlayer player, String translationKey, BlockPos pos, int count) {
        TextComponentTranslation msg = new TextComponentTranslation(
            translationKey,
            pos.getX(), pos.getY(), pos.getZ(),
            TextFormatting.GREEN + String.valueOf(count)
        );
        player.sendStatusMessage(msg, true);
    }

    public static void syncSelection(EntityPlayer player, IPlayerSelection selection) {
        if (player instanceof EntityPlayerMP) {
            PacketHandler.sendTo(
                new PacketSyncSelection(selection.getMode(), selection.getSelectedPositions().toLongArray()),
                (EntityPlayerMP) player
            );
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(TextFormatting.GRAY + new TextComponentTranslation("tooltip.spatialshift.wand.help1").getFormattedText());
        tooltip.add(TextFormatting.GRAY + new TextComponentTranslation("tooltip.spatialshift.wand.help2").getFormattedText());
        tooltip.add(TextFormatting.DARK_PURPLE + new TextComponentTranslation("tooltip.spatialshift.wand.help3").getFormattedText());
    }
}
