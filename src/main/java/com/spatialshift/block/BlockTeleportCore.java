package com.spatialshift.block;

import com.spatialshift.SpatialShift;
import com.spatialshift.gui.GuiHandler;
import com.spatialshift.multiblock.MultiblockValidator;
import com.spatialshift.tileentity.TileEntityTeleportCore;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class BlockTeleportCore extends Block implements ITileEntityProvider {

    public BlockTeleportCore() {
        super(Material.IRON);
        setRegistryName("teleport_core");
        setTranslationKey(SpatialShift.MODID + ".teleport_core");
        setHardness(5.0F);
        setResistance(15.0F);
        setCreativeTab(SpatialShift.CREATIVE_TAB);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            return true;
        }

        if (MultiblockValidator.isValidPattern(world, pos)) {
            player.openGui(SpatialShift.instance, GuiHandler.GUI_CORE, world, pos.getX(), pos.getY(), pos.getZ());
        } else {
            player.sendStatusMessage(new TextComponentTranslation("message.spatialshift.multiblock_invalid"), true);
        }

        return true;
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityTeleportCore) {
            TileEntityTeleportCore core = (TileEntityTeleportCore) te;
            ItemStack stack = core.getFuelInventory().getStackInSlot(0);
            if (!stack.isEmpty()) {
                InventoryHelper.spawnItemStack(world, pos.getX(), pos.getY(), pos.getZ(), stack);
            }
        }
        super.breakBlock(world, pos, state);
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityTeleportCore();
    }
}
