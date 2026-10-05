package com.spatialshift.tileentity;

import com.spatialshift.data.TeleportMode;
import com.spatialshift.engine.TeleportEngine;
import com.spatialshift.init.ModItems;
import com.spatialshift.multiblock.ShipNetworkScanner;
import com.spatialshift.network.PacketHandler;
import com.spatialshift.network.packet.PacketSyncCoreState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class TileEntityTeleportCore extends TileEntity implements ITickable {

    public static final float SAFE_HEAT_THRESHOLD = 75.0F;
    public static final int BASE_FUEL_CAPACITY = 10000;

    private float currentHeat = 0.0F;
    private boolean safetyLockEnabled = true;
    private int internalFuel = 0;

    private boolean hyperdriveActive = false;
    private int networkCompartmentFuel = 0;
    private int networkCompartmentMax = 0;

    private int warmupTicks = 0;
    private int totalWarmupTicks = 0;
    private BlockPos pendingTarget = null;
    private int pendingDimension = 0;
    private TeleportMode pendingMode = null;
    private boolean pendingAnchorTarget = false;
    private UUID pendingPlayerUuid = null;

    private final ItemStackHandler fuelInventory = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getItem() == ModItems.DIMENSIONAL_FUEL;
        }

        @Override
        protected void onContentsChanged(int slot) {
            markDirty();
        }
    };

    @Override
    public void update() {
        if (world == null) {
            return;
        }

        if (world.isRemote) {
            if (warmupTicks > 0) {
                warmupTicks--;
                spawnWarmupParticles();
            }
            return;
        }

        if (world.getTotalWorldTime() % 20L == 0L) {
            updateNetworkState();
        }

        if (world.getTotalWorldTime() % 40L == 0L && currentHeat > 0.0F) {
            reduceHeat(1.0F);
        }

        if (warmupTicks > 0) {
            warmupTicks--;
            spawnWarmupParticles();

            if (warmupTicks % 20 == 0) {
                float pitch = 1.0F + (1.0F - (float) warmupTicks / (float) Math.max(1, totalWarmupTicks)) * 0.5F;
                world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.BLOCK_PORTAL_TRIGGER, SoundCategory.BLOCKS, 1.0F, pitch);
            }

            if (warmupTicks == 0) {
                executeWarmupTeleport();
            }
        }
    }

    private void updateNetworkState() {
        hyperdriveActive = ShipNetworkScanner.isHyperdriveConnectedAndActive(world, pos);
        List<TileEntityFuelCompartment> compartments = ShipNetworkScanner.findConnectedFuelCompartments(world, pos);
        int compFuel = 0;
        for (TileEntityFuelCompartment comp : compartments) {
            compFuel += comp.getStoredFuel();
        }
        networkCompartmentFuel = compFuel;
        networkCompartmentMax = compartments.size() * TileEntityFuelCompartment.MAX_FUEL;

        ItemStack stack = fuelInventory.getStackInSlot(0);
        if (!stack.isEmpty() && stack.getItem() == ModItems.DIMENSIONAL_FUEL) {
            if (internalFuel + 1000 <= BASE_FUEL_CAPACITY) {
                internalFuel += 1000;
                fuelInventory.extractItem(0, 1, false);
                markDirty();
            }
        }
    }

    private void spawnWarmupParticles() {
        if (world instanceof WorldServer) {
            WorldServer ws = (WorldServer) world;
            for (int i = 0; i < 4; i++) {
                double px = pos.getX() + 0.5 + (ws.rand.nextDouble() - 0.5) * 2.0;
                double py = pos.getY() + 1.0 + ws.rand.nextDouble() * 2.0;
                double pz = pos.getZ() + 0.5 + (ws.rand.nextDouble() - 0.5) * 2.0;
                ws.spawnParticle(EnumParticleTypes.PORTAL, px, py, pz, 1, 0, 0, 0, 0.05);
            }
        }
    }

    public void startWarmup(EntityPlayerMP player, BlockPos target, int dimension, TeleportMode mode, boolean isAnchorTarget) {
        updateNetworkState();
        this.pendingPlayerUuid = player.getUniqueID();
        this.pendingTarget = target;
        this.pendingDimension = dimension;
        this.pendingMode = mode;
        this.pendingAnchorTarget = isAnchorTarget;

        this.warmupTicks = hyperdriveActive ? 100 : 1200;
        this.totalWarmupTicks = this.warmupTicks;
        markDirty();

        syncToPlayer(player);
        player.sendStatusMessage(new TextComponentTranslation("message.spatialshift.warmup_started", warmupTicks / 20), true);
    }

    private void executeWarmupTeleport() {
        if (pendingPlayerUuid != null && world instanceof WorldServer) {
            EntityPlayerMP player = (EntityPlayerMP) world.getPlayerEntityByUUID(pendingPlayerUuid);
            if (player != null && pendingTarget != null) {
                TeleportEngine.executeTeleport(
                    (WorldServer) world,
                    player,
                    this,
                    pendingTarget,
                    pendingDimension,
                    pendingMode,
                    pendingAnchorTarget
                );
            }
        }
        pendingTarget = null;
        pendingPlayerUuid = null;
        totalWarmupTicks = 0;
        markDirty();
    }

    public boolean applyCoolant(ItemStack held, EntityPlayer player) {
        if (held.isEmpty()) {
            return false;
        }

        Item item = held.getItem();
        if (item == Item.getItemFromBlock(Blocks.ICE)) {
            float cooling = Math.max(1.0F, 15.0F * (currentHeat / 100.0F));
            reduceHeat(cooling);
            if (!player.isCreative()) {
                held.shrink(1);
            }
            playCoolingEffects(1.2F);
            return true;
        }

        if (item == Item.getItemFromBlock(Blocks.PACKED_ICE)) {
            float cooling = Math.max(2.0F, 35.0F * (currentHeat / 100.0F));
            reduceHeat(cooling);
            if (!player.isCreative()) {
                held.shrink(1);
            }
            playCoolingEffects(0.9F);
            return true;
        }

        if (item == Items.WATER_BUCKET) {
            float cooling = Math.max(2.0F, 20.0F * (currentHeat / 100.0F));
            reduceHeat(cooling);
            if (!player.isCreative()) {
                player.setHeldItem(player.getActiveHand() == null ? net.minecraft.util.EnumHand.MAIN_HAND : player.getActiveHand(), new ItemStack(Items.BUCKET));
            }
            playCoolingEffects(1.0F);
            return true;
        }

        if (item == Items.LAVA_BUCKET) {
            addHeat(30.0F);
            if (!player.isCreative()) {
                player.setHeldItem(player.getActiveHand() == null ? net.minecraft.util.EnumHand.MAIN_HAND : player.getActiveHand(), new ItemStack(Items.BUCKET));
            }
            world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.BLOCK_LAVA_EXTINGUISH, SoundCategory.BLOCKS, 1.0F, 1.0F);
            return true;
        }

        if (item.getRegistryName() != null) {
            String name = item.getRegistryName().toString();
            if (name.contains("coolant") || name.contains("heat_storage")) {
                reduceHeat(70.0F);
                if (!player.isCreative()) {
                    held.shrink(1);
                }
                playCoolingEffects(0.7F);
                return true;
            }
        }

        return false;
    }

    private void playCoolingEffects(float pitch) {
        world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.8F, pitch);
        if (world instanceof WorldServer) {
            WorldServer ws = (WorldServer) world;
            for (int i = 0; i < 15; i++) {
                double px = pos.getX() + 0.5 + (ws.rand.nextDouble() - 0.5);
                double py = pos.getY() + 1.0 + ws.rand.nextDouble();
                double pz = pos.getZ() + 0.5 + (ws.rand.nextDouble() - 0.5);
                ws.spawnParticle(EnumParticleTypes.CLOUD, px, py, pz, 1, 0, 0.05, 0, 0.02);
            }
        }
    }

    public boolean hasFuel(int requiredUnits) {
        return getTotalFuel() >= requiredUnits;
    }

    public boolean hasFuel() {
        return getTotalFuel() > 0;
    }

    public boolean consumeFuel(int requiredUnits) {
        if (!hasFuel(requiredUnits)) {
            return false;
        }

        int remaining = requiredUnits;

        if (internalFuel > 0) {
            int take = Math.min(internalFuel, remaining);
            internalFuel -= take;
            remaining -= take;
        }

        while (remaining > 0) {
            ItemStack stack = fuelInventory.getStackInSlot(0);
            if (!stack.isEmpty() && stack.getItem() == ModItems.DIMENSIONAL_FUEL) {
                fuelInventory.extractItem(0, 1, false);
                int gained = 1000;
                int take = Math.min(gained, remaining);
                remaining -= take;
                internalFuel += (gained - take);
            } else {
                break;
            }
        }

        if (remaining > 0) {
            List<TileEntityFuelCompartment> compartments = ShipNetworkScanner.findConnectedFuelCompartments(world, pos);
            for (TileEntityFuelCompartment comp : compartments) {
                if (remaining <= 0) {
                    break;
                }
                int taken = comp.extractFuel(remaining);
                remaining -= taken;
            }
        }

        markDirty();
        return remaining <= 0;
    }

    public boolean consumeFuel() {
        return consumeFuel(1000);
    }

    public int getTotalFuel() {
        int invFuel = fuelInventory.getStackInSlot(0).getCount() * 1000;
        return internalFuel + invFuel + networkCompartmentFuel;
    }

    public int getMaxFuel() {
        return BASE_FUEL_CAPACITY + networkCompartmentMax;
    }

    public float getCurrentHeat() {
        return currentHeat;
    }

    public void addHeat(float amount) {
        currentHeat = Math.min(100.0F, currentHeat + amount);
        markDirty();
    }

    public void reduceHeat(float amount) {
        currentHeat = Math.max(0.0F, currentHeat - amount);
        markDirty();
    }

    public boolean isSafetyLockEnabled() {
        return safetyLockEnabled;
    }

    public void toggleSafetyLock() {
        safetyLockEnabled = !safetyLockEnabled;
        markDirty();
    }

    public boolean isHyperdriveActive() {
        return hyperdriveActive;
    }

    public int getWarmupTicks() {
        return warmupTicks;
    }

    public int getTotalWarmupTicks() {
        return totalWarmupTicks;
    }

    public void syncToPlayer(EntityPlayerMP player) {
        updateNetworkState();
        PacketHandler.sendTo(
            new PacketSyncCoreState(pos, currentHeat, safetyLockEnabled, hyperdriveActive, getTotalFuel(), getMaxFuel(), warmupTicks, totalWarmupTicks),
            player
        );
    }

    public void setClientSyncedState(float heat, boolean lock, boolean hyper, int fuel, int maxFuel, int warmup, int totalWarmup) {
        this.currentHeat = heat;
        this.safetyLockEnabled = lock;
        this.hyperdriveActive = hyper;
        this.internalFuel = fuel;
        this.networkCompartmentFuel = 0;
        this.networkCompartmentMax = Math.max(0, maxFuel - BASE_FUEL_CAPACITY);
        this.warmupTicks = warmup;
        this.totalWarmupTicks = totalWarmup;
    }

    public ItemStackHandler getFuelInventory() {
        return fuelInventory;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        if (compound.hasKey("FuelInventory")) {
            fuelInventory.deserializeNBT(compound.getCompoundTag("FuelInventory"));
        }
        if (compound.hasKey("CurrentHeat")) {
            currentHeat = compound.getFloat("CurrentHeat");
        }
        if (compound.hasKey("SafetyLock")) {
            safetyLockEnabled = compound.getBoolean("SafetyLock");
        }
        if (compound.hasKey("InternalFuel")) {
            internalFuel = compound.getInteger("InternalFuel");
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setTag("FuelInventory", fuelInventory.serializeNBT());
        compound.setFloat("CurrentHeat", currentHeat);
        compound.setBoolean("SafetyLock", safetyLockEnabled);
        compound.setInteger("InternalFuel", internalFuel);
        return compound;
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(fuelInventory);
        }
        return super.getCapability(capability, facing);
    }
}
