package com.spatialshift.container;

import com.spatialshift.tileentity.TileEntityAnchor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;

public class ContainerAnchor extends Container {

    private final TileEntityAnchor anchor;

    public ContainerAnchor(TileEntityAnchor anchor) {
        this.anchor = anchor;
    }

    public TileEntityAnchor getAnchor() {
        return anchor;
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return playerIn.getDistanceSq(anchor.getPos()) <= 64.0;
    }
}
