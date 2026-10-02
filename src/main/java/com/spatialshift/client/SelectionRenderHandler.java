package com.spatialshift.client;

import com.spatialshift.capability.IPlayerSelection;
import com.spatialshift.capability.PlayerSelection;
import com.spatialshift.init.ModItems;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class SelectionRenderHandler {

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.player;
        if (player == null) {
            return;
        }

        ItemStack held = player.getHeldItemMainhand();
        if (held.getItem() != ModItems.SELECTION_WAND) {
            return;
        }

        IPlayerSelection selection = player.getCapability(PlayerSelection.CAPABILITY, null);
        if (selection == null) {
            return;
        }

        LongSet positions = selection.getSelectedPositions();
        if (positions.isEmpty()) {
            return;
        }

        double interpX = player.lastTickPosX + (player.posX - player.lastTickPosX) * event.getPartialTicks();
        double interpY = player.lastTickPosY + (player.posY - player.lastTickPosY) * event.getPartialTicks();
        double interpZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * event.getPartialTicks();

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GlStateManager.disableTexture2D();
        GlStateManager.depthMask(false);
        GlStateManager.glLineWidth(2.0F);

        LongIterator iterator = positions.iterator();
        while (iterator.hasNext()) {
            long packed = iterator.nextLong();
            BlockPos pos = BlockPos.fromLong(packed);

            double minX = pos.getX() - interpX;
            double minY = pos.getY() - interpY;
            double minZ = pos.getZ() - interpZ;
            AxisAlignedBB box = new AxisAlignedBB(minX, minY, minZ, minX + 1.0, minY + 1.0, minZ + 1.0);

            RenderGlobal.renderFilledBox(box, 0.2F, 0.9F, 0.3F, 0.35F);
            RenderGlobal.drawSelectionBoundingBox(box, 0.1F, 0.7F, 0.2F, 0.8F);
        }

        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }
}
