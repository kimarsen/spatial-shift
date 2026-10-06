package com.spatialshift.item;

import com.spatialshift.SpatialShift;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

public class ItemDivineSightShard extends Item {

    public ItemDivineSightShard() {
        setRegistryName("divine_sight_shard");
        setTranslationKey(SpatialShift.MODID + ".divine_sight_shard");
        setMaxStackSize(16);
        setCreativeTab(SpatialShift.CREATIVE_TAB);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(TextFormatting.AQUA + new TextComponentTranslation("tooltip.spatialshift.divine_sight_shard.desc").getFormattedText());
    }
}
