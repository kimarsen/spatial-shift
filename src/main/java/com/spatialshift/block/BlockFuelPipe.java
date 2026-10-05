package com.spatialshift.block;

import com.spatialshift.SpatialShift;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

public class BlockFuelPipe extends Block {

    public BlockFuelPipe() {
        super(Material.IRON);
        setRegistryName("fuel_pipe");
        setTranslationKey(SpatialShift.MODID + ".fuel_pipe");
        setHardness(2.0F);
        setResistance(8.0F);
        setCreativeTab(SpatialShift.CREATIVE_TAB);
    }
}
