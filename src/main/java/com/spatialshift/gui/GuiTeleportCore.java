package com.spatialshift.gui;

import com.spatialshift.SpatialShift;
import com.spatialshift.client.ClientAnchorCache;
import com.spatialshift.container.ContainerTeleportCore;
import com.spatialshift.data.AnchorData;
import com.spatialshift.data.TeleportMode;
import com.spatialshift.network.PacketHandler;
import com.spatialshift.network.packet.PacketTeleportRequest;
import com.spatialshift.tileentity.TileEntityTeleportCore;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiTeleportCore extends GuiContainer {

    private static final ResourceLocation GUI_TEXTURE = new ResourceLocation(SpatialShift.MODID, "textures/gui/teleport_core.png");

    private final TileEntityTeleportCore tileEntity;
    private final List<AnchorData> availableAnchors = new ArrayList<>();
    private int selectedAnchorIndex = -1;

    private boolean useAnchorTarget = false;
    private TeleportMode teleportMode = TeleportMode.LANDING;

    private GuiTextField xField;
    private GuiTextField yField;
    private GuiTextField zField;

    private GuiButton buttonMode;
    private GuiButton buttonTargetType;
    private GuiButton buttonPrevAnchor;
    private GuiButton buttonNextAnchor;
    private GuiButton buttonTeleport;

    public GuiTeleportCore(InventoryPlayer playerInv, TileEntityTeleportCore tileEntity) {
        super(new ContainerTeleportCore(playerInv, tileEntity));
        this.tileEntity = tileEntity;
        this.xSize = 176;
        this.ySize = 166;
    }

    @Override
    public void initGui() {
        super.initGui();

        int startX = (width - xSize) / 2;
        int startY = (height - ySize) / 2;

        xField = new GuiTextField(0, fontRenderer, startX + 10, startY + 20, 36, 12);
        yField = new GuiTextField(1, fontRenderer, startX + 50, startY + 20, 36, 12);
        zField = new GuiTextField(2, fontRenderer, startX + 90, startY + 20, 36, 12);

        BlockPos pos = tileEntity.getPos();
        xField.setText(String.valueOf(pos.getX()));
        yField.setText(String.valueOf(pos.getY()));
        zField.setText(String.valueOf(pos.getZ()));

        availableAnchors.clear();
        availableAnchors.addAll(ClientAnchorCache.getAnchors());
        if (!availableAnchors.isEmpty() && selectedAnchorIndex == -1) {
            selectedAnchorIndex = 0;
        }

        buttonTargetType = addButton(new GuiButton(10, startX + 130, startY + 18, 38, 16, getTargetTypeText()));
        buttonMode = addButton(new GuiButton(11, startX + 10, startY + 36, 60, 16, getModeText()));
        buttonPrevAnchor = addButton(new GuiButton(12, startX + 75, startY + 36, 16, 16, "<"));
        buttonNextAnchor = addButton(new GuiButton(13, startX + 152, startY + 36, 16, 16, ">"));
        buttonTeleport = addButton(new GuiButton(14, startX + 105, startY + 54, 63, 20, I18n.format("gui.spatialshift.button_teleport")));

        updateButtonStates();
    }

    public void updateAnchors(List<AnchorData> anchors) {
        availableAnchors.clear();
        availableAnchors.addAll(anchors);
        if (!availableAnchors.isEmpty()) {
            if (selectedAnchorIndex < 0 || selectedAnchorIndex >= availableAnchors.size()) {
                selectedAnchorIndex = 0;
            }
        } else {
            selectedAnchorIndex = -1;
        }
        updateButtonStates();
    }

    private String getTargetTypeText() {
        return useAnchorTarget ? I18n.format("gui.spatialshift.target_anchor") : I18n.format("gui.spatialshift.target_coords");
    }

    private String getModeText() {
        return teleportMode == TeleportMode.LANDING ? I18n.format("gui.spatialshift.mode_landing") : I18n.format("gui.spatialshift.mode_air");
    }

    private void updateButtonStates() {
        if (buttonTargetType != null) {
            buttonTargetType.displayString = getTargetTypeText();
        }
        if (buttonMode != null) {
            buttonMode.displayString = getModeText();
        }
        boolean hasAnchors = !availableAnchors.isEmpty() && useAnchorTarget;
        if (buttonPrevAnchor != null) {
            buttonPrevAnchor.enabled = hasAnchors;
        }
        if (buttonNextAnchor != null) {
            buttonNextAnchor.enabled = hasAnchors;
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 10) {
            useAnchorTarget = !useAnchorTarget;
            updateButtonStates();
        } else if (button.id == 11) {
            teleportMode = teleportMode.next();
            updateButtonStates();
        } else if (button.id == 12) {
            if (!availableAnchors.isEmpty()) {
                selectedAnchorIndex = (selectedAnchorIndex - 1 + availableAnchors.size()) % availableAnchors.size();
            }
        } else if (button.id == 13) {
            if (!availableAnchors.isEmpty()) {
                selectedAnchorIndex = (selectedAnchorIndex + 1) % availableAnchors.size();
            }
        } else if (button.id == 14) {
            executeTeleport();
        }
    }

    private void executeTeleport() {
        BlockPos targetPos;
        int targetDim = tileEntity.getWorld().provider.getDimension();

        if (useAnchorTarget && selectedAnchorIndex >= 0 && selectedAnchorIndex < availableAnchors.size()) {
            AnchorData anchor = availableAnchors.get(selectedAnchorIndex);
            targetPos = anchor.getPos();
            targetDim = anchor.getDimensionId();
        } else {
            try {
                int x = Integer.parseInt(xField.getText().trim());
                int y = Integer.parseInt(yField.getText().trim());
                int z = Integer.parseInt(zField.getText().trim());
                targetPos = new BlockPos(x, y, z);
            } catch (NumberFormatException e) {
                return;
            }
        }

        PacketHandler.sendToServer(new PacketTeleportRequest(
            tileEntity.getPos(),
            targetPos,
            targetDim,
            teleportMode
        ));

        mc.player.closeScreen();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (!useAnchorTarget) {
            if (xField.textboxKeyTyped(typedChar, keyCode) ||
                yField.textboxKeyTyped(typedChar, keyCode) ||
                zField.textboxKeyTyped(typedChar, keyCode)) {
                return;
            }
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (!useAnchorTarget) {
            xField.mouseClicked(mouseX, mouseY, mouseButton);
            yField.mouseClicked(mouseX, mouseY, mouseButton);
            zField.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(GUI_TEXTURE);
        int startX = (width - xSize) / 2;
        int startY = (height - ySize) / 2;
        drawTexturedModalRect(startX, startY, 0, 0, xSize, ySize);

        if (!useAnchorTarget) {
            xField.drawTextBox();
            yField.drawTextBox();
            zField.drawTextBox();
        } else {
            String anchorText = I18n.format("gui.spatialshift.no_anchors");
            if (selectedAnchorIndex >= 0 && selectedAnchorIndex < availableAnchors.size()) {
                anchorText = availableAnchors.get(selectedAnchorIndex).getName();
            }
            fontRenderer.drawString(anchorText, startX + 95, startY + 40, 0x404040);
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRenderer.drawString(I18n.format("gui.spatialshift.title"), 8, 6, 0x404040);
        fontRenderer.drawString(I18n.format("gui.spatialshift.fuel"), 54, 57, 0x404040);
    }
}
