package com.spatialshift.block;

import com.spatialshift.SpatialShift;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

public class BlockHyperCable extends Block {

    public BlockHyperCable() {
        super(Material.IRON);
        setRegistryName("hyper_cable");
        setTranslationKey(SpatialShift.MODID + ".hyper_cable");
        setHardness(2.0F);
        setResistance(8.0F);
        setCreativeTab(SpatialShift.CREATIVE_TAB);
    }
}
