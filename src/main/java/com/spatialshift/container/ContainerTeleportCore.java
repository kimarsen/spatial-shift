package com.spatialshift.container;

import com.spatialshift.init.ModItems;
import com.spatialshift.tileentity.TileEntityTeleportCore;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

public class ContainerTeleportCore extends Container {

    private final TileEntityTeleportCore tileEntity;

    public ContainerTeleportCore(InventoryPlayer playerInv, TileEntityTeleportCore tileEntity) {
        this.tileEntity = tileEntity;

        addSlotToContainer(new SlotItemHandler(tileEntity.getFuelInventory(), 0, 89, 115) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return stack.getItem() == ModItems.DIMENSIONAL_FUEL && tileEntity.getTotalFuel() < tileEntity.getMaxFuel();
            }

            @Override
            public int getItemStackLimit(ItemStack stack) {
                int remaining = Math.max(0, tileEntity.getMaxFuel() - tileEntity.getTotalFuel());
                int allowed = remaining / 1000;
                int current = getStack().getCount();
                return Math.min(64, current + allowed);
            }
        });

        addSlotToContainer(new SlotItemHandler(tileEntity.getCoolantInventory(), 0, 11, 115) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return TileEntityTeleportCore.isCoolantItem(stack);
            }
        });

        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                addSlotToContainer(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 156 + row * 18));
            }
        }

        for (int col = 0; col < 9; ++col) {
            addSlotToContainer(new Slot(playerInv, col, 8 + col * 18, 214));
        }
    }

    public TileEntityTeleportCore getTileEntity() {
        return tileEntity;
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return playerIn.getDistanceSq(tileEntity.getPos()) <= 64.0;
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        for (net.minecraft.inventory.IContainerListener listener : listeners) {
            if (listener instanceof net.minecraft.entity.player.EntityPlayerMP) {
                tileEntity.syncToPlayer((net.minecraft.entity.player.EntityPlayerMP) listener);
            }
        }
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = inventorySlots.get(index);

        if (slot != null && slot.getHasStack()) {
            ItemStack current = slot.getStack();
            itemstack = current.copy();

            if (index < 2) {
                if (!mergeItemStack(current, 2, 38, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (current.getItem() == ModItems.DIMENSIONAL_FUEL && tileEntity.getTotalFuel() < tileEntity.getMaxFuel()) {
                    if (!mergeItemStack(current, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (TileEntityTeleportCore.isCoolantItem(current)) {
                    if (!mergeItemStack(current, 1, 2, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index < 29) {
                    if (!mergeItemStack(current, 29, 38, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!mergeItemStack(current, 2, 29, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (current.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }

            if (current.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(playerIn, current);
        }

        return itemstack;
    }
}
