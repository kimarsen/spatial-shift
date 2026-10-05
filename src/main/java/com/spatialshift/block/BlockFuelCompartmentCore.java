package com.spatialshift.block;

import com.spatialshift.SpatialShift;
import com.spatialshift.init.ModItems;
import com.spatialshift.multiblock.MultiblockValidator;
import com.spatialshift.tileentity.TileEntityFuelCompartment;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
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
import net.minecraft.world.Explosion;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class BlockFuelCompartmentCore extends Block implements ITileEntityProvider {

    public BlockFuelCompartmentCore() {
        super(Material.IRON);
        setRegistryName("fuel_compartment");
        setTranslationKey(SpatialShift.MODID + ".fuel_compartment");
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
        if (te instanceof TileEntityFuelCompartment) {
            TileEntityFuelCompartment comp = (TileEntityFuelCompartment) te;
            if (!MultiblockValidator.isValidFuelCompartmentPattern(world, pos)) {
                world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.BLOCK_DISPENSER_FAIL, SoundCategory.BLOCKS, 0.8F, 0.8F);
                player.sendStatusMessage(new TextComponentTranslation("message.spatialshift.multiblock_invalid"), true);
                return true;
            }

            ItemStack held = player.getHeldItem(hand);
            if (!held.isEmpty() && held.getItem() == ModItems.DIMENSIONAL_FUEL) {
                if (comp.getStoredFuel() < TileEntityFuelCompartment.MAX_FUEL) {
                    comp.addFuel(1000);
                    if (!player.isCreative()) {
                        held.shrink(1);
                    }
                    world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.BLOCK_BREWING_STAND_BREW, SoundCategory.BLOCKS, 0.8F, 1.2F);
                    player.sendStatusMessage(new TextComponentTranslation(
                        "message.spatialshift.fuel_added",
                        TextFormatting.GREEN + String.valueOf(comp.getStoredFuel()),
                        TextFormatting.DARK_GREEN + String.valueOf(TileEntityFuelCompartment.MAX_FUEL)
                    ), true);
                } else {
                    world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.BLOCK_DISPENSER_FAIL, SoundCategory.BLOCKS, 0.8F, 0.8F);
                    player.sendStatusMessage(new TextComponentTranslation("message.spatialshift.fuel_full"), true);
                }
            } else {
                world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.BLOCK_IRON_DOOR_OPEN, SoundCategory.BLOCKS, 0.8F, 1.2F);
                player.sendStatusMessage(new TextComponentTranslation(
                    "message.spatialshift.fuel_status",
                    TextFormatting.GREEN + String.valueOf(comp.getStoredFuel()),
                    TextFormatting.DARK_GREEN + String.valueOf(TileEntityFuelCompartment.MAX_FUEL)
                ), true);
            }
        }

        return true;
    }

    @Override
    public void onBlockExploded(World world, BlockPos pos, Explosion explosion) {
        TileEntity te = world.getTileEntity(pos);
        int stored = 0;
        if (te instanceof TileEntityFuelCompartment) {
            stored = ((TileEntityFuelCompartment) te).getStoredFuel();
        }
        super.onBlockExploded(world, pos, explosion);
        if (!world.isRemote && stored > 0) {
            float power = (stored / 1000.0F) * 3.0F;
            world.createExplosion(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, power, true);
        }
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityFuelCompartment();
    }
}
