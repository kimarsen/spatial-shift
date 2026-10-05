package com.spatialshift.block;

import com.spatialshift.SpatialShift;
import com.spatialshift.data.AnchorAccess;
import com.spatialshift.data.AnchorSavedData;
import com.spatialshift.tileentity.TileEntityAnchor;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class BlockAnchor extends Block implements ITileEntityProvider {

    public BlockAnchor() {
        super(Material.IRON);
        setRegistryName("spatial_anchor");
        setTranslationKey(SpatialShift.MODID + ".spatial_anchor");
        setHardness(4.0F);
        setResistance(20.0F);
        setCreativeTab(SpatialShift.CREATIVE_TAB);
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(world, pos, state, placer, stack);
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityAnchor && placer instanceof EntityPlayer) {
            TileEntityAnchor anchor = (TileEntityAnchor) te;
            anchor.setOwnerUuid(placer.getUniqueID());
            anchor.setAnchorName(placer.getName() + "'s Anchor");
        }
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            return true;
        }

        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityAnchor) {
            TileEntityAnchor anchor = (TileEntityAnchor) te;
            if (anchor.getOwnerUuid().equals(player.getUniqueID())) {
                world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.BLOCK_IRON_DOOR_OPEN, SoundCategory.BLOCKS, 0.8F, 1.1F);
                player.openGui(SpatialShift.instance, com.spatialshift.gui.GuiHandler.GUI_ANCHOR, world, pos.getX(), pos.getY(), pos.getZ());
            } else {
                world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.BLOCK_NOTE_BASS, SoundCategory.BLOCKS, 0.8F, 0.5F);
                player.sendStatusMessage(new TextComponentTranslation("message.spatialshift.anchor_not_owner"), true);
            }
        }
        return true;
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        if (!world.isRemote) {
            AnchorSavedData.get(world).removeAnchor(pos.toLong());
        }
        super.breakBlock(world, pos, state);
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityAnchor();
    }
}
