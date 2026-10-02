package com.spatialshift.block;

import com.spatialshift.SpatialShift;
import com.spatialshift.multiblock.MultiblockValidator;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockCoreFrame extends Block {

    public BlockCoreFrame() {
        super(Material.IRON);
        setRegistryName("core_frame");
        setTranslationKey(SpatialShift.MODID + ".core_frame");
        setHardness(3.0F);
        setResistance(10.0F);
        setCreativeTab(SpatialShift.CREATIVE_TAB);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        BlockPos centerPos = MultiblockValidator.findCenterCore(world, pos);
        if (centerPos != null) {
            IBlockState centerState = world.getBlockState(centerPos);
            return centerState.getBlock().onBlockActivated(world, centerPos, centerState, player, hand, facing, hitX, hitY, hitZ);
        }
        return false;
    }
}
