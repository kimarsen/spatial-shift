package com.spatialshift.block;

import com.spatialshift.SpatialShift;
import com.spatialshift.multiblock.MultiblockValidator;
import com.spatialshift.tileentity.TileEntityHyperdriveCore;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class BlockHyperdriveCore extends Block implements ITileEntityProvider {

    public BlockHyperdriveCore() {
        super(Material.IRON);
        setRegistryName("hyperdrive_core");
        setTranslationKey(SpatialShift.MODID + ".hyperdrive_core");
        setHardness(5.0F);
        setResistance(15.0F);
        setCreativeTab(SpatialShift.CREATIVE_TAB);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            return true;
        }

        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityHyperdriveCore) {
            TileEntityHyperdriveCore hyperdrive = (TileEntityHyperdriveCore) te;
            hyperdrive.checkActiveState();
            if (!MultiblockValidator.isValidHyperdrivePattern(world, pos)) {
                world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.BLOCK_DISPENSER_FAIL, SoundCategory.BLOCKS, 0.8F, 0.8F);
                player.sendStatusMessage(new TextComponentTranslation("message.spatialshift.multiblock_invalid"), true);
            } else if (hyperdrive.isActive()) {
                world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.BLOCK_PORTAL_TRIGGER, SoundCategory.BLOCKS, 0.8F, 1.4F);
                player.sendStatusMessage(new TextComponentTranslation("message.spatialshift.hyperdrive_online"), true);
            } else {
                world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.BLOCKS, 0.8F, 0.8F);
                player.sendStatusMessage(new TextComponentTranslation("message.spatialshift.hyperdrive_offline"), true);
            }
        }

        return true;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityHyperdriveCore();
    }
}
