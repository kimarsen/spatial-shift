package com.spatialshift.gui;

import com.spatialshift.SpatialShift;
import com.spatialshift.client.ClientAnchorCache;
import com.spatialshift.container.ContainerTeleportCore;
import com.spatialshift.data.AnchorData;
import com.spatialshift.data.TeleportMode;
import com.spatialshift.init.ModItems;
import com.spatialshift.network.PacketHandler;
import com.spatialshift.network.packet.PacketActivateRadarArtifact;
import com.spatialshift.network.packet.PacketTeleportRequest;
import com.spatialshift.network.packet.PacketToggleSafetyLock;
import com.spatialshift.tileentity.TileEntityTeleportCore;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.biome.Biome;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GuiTeleportCore extends GuiContainer {

    private static final ResourceLocation GUI_TEXTURE = new ResourceLocation(SpatialShift.MODID, "textures/gui/teleport_core.png");

    private final TileEntityTeleportCore tileEntity;
    private final List<AnchorData> availableAnchors = new ArrayList<>();
    private int selectedAnchorIndex = -1;

    private final List<String> onlinePlayers = new ArrayList<>();
    private int selectedPlayerIndex = 0;

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
    private GuiButton buttonSafetyLock;

    private GuiButton buttonPrevPlayer;
    private GuiButton buttonNextPlayer;
    private GuiButton buttonArtifactAction;

    public GuiTeleportCore(InventoryPlayer playerInv, TileEntityTeleportCore tileEntity) {
        super(new ContainerTeleportCore(playerInv, tileEntity));
        this.tileEntity = tileEntity;
        this.xSize = 176;
        this.ySize = 238;
    }

    @Override
    public void initGui() {
        super.initGui();

        int startX = (width - xSize) / 2;
        int startY = (height - ySize) / 2;

        xField = new GuiTextField(0, fontRenderer, startX + 10, startY + 80, 36, 12);
        yField = new GuiTextField(1, fontRenderer, startX + 50, startY + 80, 36, 12);
        zField = new GuiTextField(2, fontRenderer, startX + 90, startY + 80, 36, 12);

        BlockPos pos = tileEntity.getPos();
        xField.setText(String.valueOf(pos.getX()));
        yField.setText(String.valueOf(pos.getY()));
        zField.setText(String.valueOf(pos.getZ()));

        availableAnchors.clear();
        availableAnchors.addAll(ClientAnchorCache.getAnchors());
        if (!availableAnchors.isEmpty() && selectedAnchorIndex == -1) {
            selectedAnchorIndex = 0;
        }

        refreshOnlinePlayers();

        buttonTargetType = addButton(new GuiButton(10, startX + 130, startY + 78, 38, 16, getTargetTypeText()));
        buttonMode = addButton(new GuiButton(11, startX + 10, startY + 95, 60, 16, getModeText()));
        buttonPrevAnchor = addButton(new GuiButton(12, startX + 74, startY + 95, 16, 16, "<"));
        buttonNextAnchor = addButton(new GuiButton(13, startX + 150, startY + 95, 16, 16, ">"));
        buttonSafetyLock = addButton(new GuiButton(15, startX + 10, startY + 134, 74, 18, getSafetyLockText()));
        buttonTeleport = addButton(new GuiButton(14, startX + 92, startY + 134, 74, 18, I18n.format("gui.spatialshift.button_teleport")));

        buttonPrevPlayer = addButton(new GuiButton(20, startX + 14, startY + 16, 14, 14, "<"));
        buttonNextPlayer = addButton(new GuiButton(21, startX + 88, startY + 16, 14, 14, ">"));
        buttonArtifactAction = addButton(new GuiButton(22, startX + 104, startY + 16, 38, 14, "..."));

        updateButtonStates();
    }

    private void refreshOnlinePlayers() {
        onlinePlayers.clear();
        if (mc.getConnection() != null) {
            for (NetworkPlayerInfo info : mc.getConnection().getPlayerInfoMap()) {
                if (info.getGameProfile() != null && info.getGameProfile().getName() != null) {
                    onlinePlayers.add(info.getGameProfile().getName());
                }
            }
        }
        if (!onlinePlayers.isEmpty()) {
            if (selectedPlayerIndex < 0 || selectedPlayerIndex >= onlinePlayers.size()) {
                selectedPlayerIndex = 0;
            }
        } else {
            selectedPlayerIndex = 0;
        }
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

    private String getSafetyLockText() {
        return tileEntity.isSafetyLockEnabled()
            ? TextFormatting.GREEN + I18n.format("gui.spatialshift.safety_on")
            : TextFormatting.RED + I18n.format("gui.spatialshift.safety_off");
    }

    private void updateButtonStates() {
        if (buttonTargetType != null) {
            buttonTargetType.displayString = getTargetTypeText();
        }
        if (buttonMode != null) {
            buttonMode.displayString = getModeText();
        }
        if (buttonSafetyLock != null) {
            buttonSafetyLock.displayString = getSafetyLockText();
        }
        boolean hasAnchors = !availableAnchors.isEmpty() && useAnchorTarget;
        if (buttonPrevAnchor != null) {
            buttonPrevAnchor.enabled = hasAnchors;
        }
        if (buttonNextAnchor != null) {
            buttonNextAnchor.enabled = hasAnchors;
        }
        if (buttonTeleport != null) {
            buttonTeleport.enabled = !useAnchorTarget || !availableAnchors.isEmpty();
        }

        ItemStack artifact = tileEntity.getArtifactInventory().getStackInSlot(0);
        boolean hasShard = !artifact.isEmpty() && artifact.getItem() == ModItems.DIVINE_SIGHT_SHARD;
        boolean hasEye = !artifact.isEmpty() && artifact.getItem() == ModItems.IMPERIAL_EYE;
        boolean trackingActive = tileEntity.getTrackingTicks() > 0;
        boolean eyeActive = tileEntity.getImperialEyeTicks() > 0;
        boolean showArtifactControls = hasShard || hasEye || trackingActive || eyeActive;

        if (buttonPrevPlayer != null && buttonNextPlayer != null && buttonArtifactAction != null) {
            buttonPrevPlayer.visible = showArtifactControls && !trackingActive && !eyeActive;
            buttonNextPlayer.visible = showArtifactControls && !trackingActive && !eyeActive;
            buttonArtifactAction.visible = showArtifactControls;

            if (trackingActive) {
                buttonArtifactAction.enabled = false;
                buttonArtifactAction.displayString = (tileEntity.getTrackingTicks() / 20) + "с";
            } else if (eyeActive) {
                buttonArtifactAction.enabled = false;
                buttonArtifactAction.displayString = (tileEntity.getImperialEyeTicks() / 20) + "с";
            } else if (hasShard) {
                buttonArtifactAction.enabled = !onlinePlayers.isEmpty();
                buttonArtifactAction.displayString = I18n.format("gui.spatialshift.btn_track");
            } else if (hasEye) {
                buttonArtifactAction.enabled = !onlinePlayers.isEmpty();
                buttonArtifactAction.displayString = I18n.format("gui.spatialshift.btn_summon");
            }
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
                AnchorData a = availableAnchors.get(selectedAnchorIndex);
                xField.setText(String.valueOf(a.getPos().getX()));
                yField.setText(String.valueOf(a.getPos().getY()));
                zField.setText(String.valueOf(a.getPos().getZ()));
            }
        } else if (button.id == 13) {
            if (!availableAnchors.isEmpty()) {
                selectedAnchorIndex = (selectedAnchorIndex + 1) % availableAnchors.size();
                AnchorData a = availableAnchors.get(selectedAnchorIndex);
                xField.setText(String.valueOf(a.getPos().getX()));
                yField.setText(String.valueOf(a.getPos().getY()));
                zField.setText(String.valueOf(a.getPos().getZ()));
            }
        } else if (button.id == 14) {
            executeTeleport();
        } else if (button.id == 15) {
            tileEntity.toggleSafetyLock();
            PacketHandler.sendToServer(new PacketToggleSafetyLock(tileEntity.getPos()));
            updateButtonStates();
        } else if (button.id == 20) {
            if (!onlinePlayers.isEmpty()) {
                selectedPlayerIndex = (selectedPlayerIndex - 1 + onlinePlayers.size()) % onlinePlayers.size();
                updateButtonStates();
            }
        } else if (button.id == 21) {
            if (!onlinePlayers.isEmpty()) {
                selectedPlayerIndex = (selectedPlayerIndex + 1) % onlinePlayers.size();
                updateButtonStates();
            }
        } else if (button.id == 22) {
            ItemStack artifact = tileEntity.getArtifactInventory().getStackInSlot(0);
            if (!onlinePlayers.isEmpty() && selectedPlayerIndex >= 0 && selectedPlayerIndex < onlinePlayers.size()) {
                String target = onlinePlayers.get(selectedPlayerIndex);
                if (artifact.getItem() == ModItems.DIVINE_SIGHT_SHARD) {
                    PacketHandler.sendToServer(new PacketActivateRadarArtifact(tileEntity.getPos(), 1, target));
                } else if (artifact.getItem() == ModItems.IMPERIAL_EYE) {
                    PacketHandler.sendToServer(new PacketActivateRadarArtifact(tileEntity.getPos(), 2, target));
                }
            }
        }
    }

    private void executeTeleport() {
        BlockPos targetPos;
        int targetDim = tileEntity.getWorld().provider.getDimension();

        if (useAnchorTarget) {
            if (availableAnchors.isEmpty() || selectedAnchorIndex < 0 || selectedAnchorIndex >= availableAnchors.size()) {
                return;
            }
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
            teleportMode,
            useAnchorTarget
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

    private double calculateViewRadius() {
        BlockPos corePos = tileEntity.getPos();
        double maxDist = 1000.0;

        if (useAnchorTarget) {
            if (selectedAnchorIndex >= 0 && selectedAnchorIndex < availableAnchors.size()) {
                AnchorData a = availableAnchors.get(selectedAnchorIndex);
                if (a.getDimensionId() == tileEntity.getWorld().provider.getDimension()) {
                    double dist = Math.hypot(a.getPos().getX() - corePos.getX(), a.getPos().getZ() - corePos.getZ());
                    if (dist > maxDist) {
                        maxDist = dist;
                    }
                }
            }
        } else {
            try {
                int tx = Integer.parseInt(xField.getText().trim());
                int tz = Integer.parseInt(zField.getText().trim());
                double dist = Math.hypot(tx - corePos.getX(), tz - corePos.getZ());
                if (dist > maxDist) {
                    maxDist = dist;
                }
            } catch (NumberFormatException ignored) {
            }
        }

        int currentDim = tileEntity.getWorld().provider.getDimension();
        for (AnchorData a : availableAnchors) {
            if (a.getDimensionId() == currentDim) {
                double dist = Math.hypot(a.getPos().getX() - corePos.getX(), a.getPos().getZ() - corePos.getZ());
                if (dist > maxDist) {
                    maxDist = dist;
                }
            }
        }

        return maxDist * 1.15;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        if (!useAnchorTarget) {
            xField.mouseClicked(mouseX, mouseY, mouseButton);
            yField.mouseClicked(mouseX, mouseY, mouseButton);
            zField.mouseClicked(mouseX, mouseY, mouseButton);
        }

        int startX = (width - xSize) / 2;
        int startY = (height - ySize) / 2;
        int mapX = startX + 10;
        int mapY = startY + 14;
        int mapW = 156;
        int mapH = 62;

        if (mouseButton == 0 && mouseX >= mapX && mouseX <= mapX + mapW && mouseY >= mapY && mouseY <= mapY + mapH) {
            if (mouseX >= startX + 145 && mouseX <= startX + 163 && mouseY >= startY + 15 && mouseY <= startY + 33) {
                return;
            }

            ItemStack artifact = tileEntity.getArtifactInventory().getStackInSlot(0);
            boolean hasShard = !artifact.isEmpty() && artifact.getItem() == ModItems.DIVINE_SIGHT_SHARD;
            boolean hasEye = !artifact.isEmpty() && artifact.getItem() == ModItems.IMPERIAL_EYE;
            if ((hasShard || hasEye) && mouseX >= startX + 14 && mouseX <= startX + 144 && mouseY >= startY + 15 && mouseY <= startY + 31) {
                return;
            }

            double radius = calculateViewRadius();
            int centerX = mapX + 78;
            int centerY = mapY + 31;
            double scaleX = (mapW / 2.0) / radius;
            double scaleZ = (mapH / 2.0) / radius;
            BlockPos corePos = tileEntity.getPos();
            int currentDim = tileEntity.getWorld().provider.getDimension();

            for (int i = 0; i < availableAnchors.size(); i++) {
                AnchorData a = availableAnchors.get(i);
                if (a.getDimensionId() == currentDim) {
                    int ax = centerX + (int) Math.round((a.getPos().getX() - corePos.getX()) * scaleX);
                    int ay = centerY + (int) Math.round((a.getPos().getZ() - corePos.getZ()) * scaleZ);
                    if (Math.hypot(mouseX - ax, mouseY - ay) <= 4.5) {
                        useAnchorTarget = true;
                        selectedAnchorIndex = i;
                        xField.setText(String.valueOf(a.getPos().getX()));
                        yField.setText(String.valueOf(a.getPos().getY()));
                        zField.setText(String.valueOf(a.getPos().getZ()));
                        updateButtonStates();
                        mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.2F));
                        return;
                    }
                }
            }

            int clickedX = corePos.getX() + (int) Math.round((mouseX - centerX) / scaleX);
            int clickedZ = corePos.getZ() + (int) Math.round((mouseY - centerY) / scaleZ);
            useAnchorTarget = false;
            xField.setText(String.valueOf(clickedX));
            zField.setText(String.valueOf(clickedZ));
            updateButtonStates();
            mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(GUI_TEXTURE);
        int startX = (width - xSize) / 2;
        int startY = (height - ySize) / 2;
        drawTexturedModalRect(startX, startY, 0, 0, xSize, ySize);

        drawMinimap(startX, startY);

        if (!useAnchorTarget) {
            xField.drawTextBox();
            yField.drawTextBox();
            zField.drawTextBox();
        } else {
            String anchorText = I18n.format("gui.spatialshift.no_anchors");
            if (selectedAnchorIndex >= 0 && selectedAnchorIndex < availableAnchors.size()) {
                anchorText = availableAnchors.get(selectedAnchorIndex).getName();
            }
            int textWidth = fontRenderer.getStringWidth(anchorText);
            fontRenderer.drawString(anchorText, startX + 112 - textWidth / 2, startY + 99, 0x404040);
        }
    }

    private void drawMinimap(int startX, int startY) {
        int mapX = startX + 10;
        int mapY = startY + 14;
        int mapW = 156;
        int mapH = 62;
        int centerX = mapX + 78;
        int centerY = mapY + 31;

        double radius = calculateViewRadius();
        double scaleX = (mapW / 2.0) / radius;
        double scaleZ = (mapH / 2.0) / radius;
        BlockPos corePos = tileEntity.getPos();

        drawRect(mapX, mapY, mapX + mapW, mapY + mapH, 0xFF0C121A);

        drawRect(mapX, centerY, mapX + mapW, centerY + 1, 0x25336688);
        drawRect(centerX, mapY, centerX + 1, mapY + mapH, 0x25336688);

        if (mc.world != null) {
            for (int px = mapX + 2; px < mapX + mapW - 2; px += 4) {
                for (int py = mapY + 2; py < mapY + mapH - 2; py += 4) {
                    int bx = corePos.getX() + (int) Math.round((px - centerX) / scaleX);
                    int bz = corePos.getZ() + (int) Math.round((py - centerY) / scaleZ);
                    BlockPos bp = new BlockPos(bx, 64, bz);
                    if (mc.world.isBlockLoaded(bp)) {
                        Biome biome = mc.world.getBiome(bp);
                        int dotColor = 0x20387030;
                        if (net.minecraftforge.common.BiomeDictionary.hasType(biome, net.minecraftforge.common.BiomeDictionary.Type.WATER) || biome.getBaseHeight() < 0.0F) {
                            dotColor = 0x251E4E79;
                        } else if (biome.getBaseHeight() > 0.6F) {
                            dotColor = 0x256B7280;
                        } else if (biome.getDefaultTemperature() > 1.0F) {
                            dotColor = 0x25C2B280;
                        } else if (biome.isSnowyBiome()) {
                            dotColor = 0x25E0E8E8;
                        }
                        drawRect(px, py, px + 3, py + 3, dotColor);
                    }
                }
            }
        }

        int r1000 = (int) Math.round(1000.0 * scaleZ);
        if (r1000 > 2) {
            int circleColor = tileEntity.isHyperdriveActive() ? 0x6000FF99 : 0x60FFAA00;
            drawCircle(centerX, centerY, r1000, circleColor);
        }

        try {
            int tx = Integer.parseInt(xField.getText().trim());
            int tz = Integer.parseInt(zField.getText().trim());
            int targetScreenX = centerX + (int) Math.round((tx - corePos.getX()) * scaleX);
            int targetScreenY = centerY + (int) Math.round((tz - corePos.getZ()) * scaleZ);

            if (targetScreenX >= mapX && targetScreenX <= mapX + mapW && targetScreenY >= mapY && targetScreenY <= mapY + mapH) {
                drawLine(centerX, centerY, targetScreenX, targetScreenY, 0x60FF4444);
                drawRect(targetScreenX - 2, targetScreenY, targetScreenX + 3, targetScreenY + 1, 0xFFFF2222);
                drawRect(targetScreenX, targetScreenY - 2, targetScreenX + 1, targetScreenY + 3, 0xFFFF2222);
            }
        } catch (NumberFormatException ignored) {
        }

        int currentDim = tileEntity.getWorld().provider.getDimension();
        for (int i = 0; i < availableAnchors.size(); i++) {
            AnchorData a = availableAnchors.get(i);
            if (a.getDimensionId() == currentDim) {
                int ax = centerX + (int) Math.round((a.getPos().getX() - corePos.getX()) * scaleX);
                int ay = centerY + (int) Math.round((a.getPos().getZ() - corePos.getZ()) * scaleZ);

                if (ax >= mapX + 2 && ax <= mapX + mapW - 2 && ay >= mapY + 2 && ay <= mapY + mapH - 2) {
                    if (useAnchorTarget && selectedAnchorIndex == i) {
                        drawRect(ax - 3, ay - 3, ax + 4, ay + 4, 0x9000FFFF);
                        drawRect(ax - 2, ay - 2, ax + 3, ay + 3, 0xFFFFDD00);
                        drawRect(ax - 1, ay - 1, ax + 2, ay + 2, 0xFFFFFFFF);
                    } else {
                        drawRect(ax - 2, ay - 2, ax + 3, ay + 3, 0xFFCC9900);
                        drawRect(ax - 1, ay - 1, ax + 2, ay + 2, 0xFFFFDD33);
                    }
                }
            }
        }

        if (tileEntity.getTrackingTicks() > 0) {
            if (tileEntity.getTrackedPlayerDim() == currentDim) {
                double targetDx = tileEntity.getTrackedPlayerX() - corePos.getX();
                double targetDz = tileEntity.getTrackedPlayerZ() - corePos.getZ();
                double targetDist = Math.hypot(targetDx, targetDz);

                int screenTargetX = centerX + (int) Math.round(targetDx * scaleX);
                int screenTargetY = centerY + (int) Math.round(targetDz * scaleZ);

                int clampedX = Math.max(mapX + 4, Math.min(mapX + mapW - 4, screenTargetX));
                int clampedY = Math.max(mapY + 4, Math.min(mapY + mapH - 4, screenTargetY));

                if (screenTargetX < mapX + 4 || screenTargetX > mapX + mapW - 4 || screenTargetY < mapY + 4 || screenTargetY > mapY + mapH - 4) {
                    double angle = Math.atan2(targetDz, targetDx);
                    double borderDist = Math.min((mapW / 2.0 - 6) / Math.max(0.001, Math.abs(Math.cos(angle))), (mapH / 2.0 - 6) / Math.max(0.001, Math.abs(Math.sin(angle))));
                    clampedX = centerX + (int) Math.round(Math.cos(angle) * borderDist);
                    clampedY = centerY + (int) Math.round(Math.sin(angle) * borderDist);
                }

                drawLine(centerX, centerY, clampedX, clampedY, 0xEE00FFEE);

                drawRect(clampedX - 2, clampedY - 2, clampedX + 3, clampedY + 3, 0xFF00FFEE);
                drawRect(clampedX - 1, clampedY - 1, clampedX + 2, clampedY + 2, 0xFFFFFFFF);

                String trackInfo = I18n.format("gui.spatialshift.radar_tracking", tileEntity.getTrackedPlayerName(), (int) targetDist, tileEntity.getTrackingTicks() / 20);
                fontRenderer.drawString(trackInfo, mapX + 4, mapY + mapH - 10, 0xFF00FFCC);
            } else {
                String dimInfo = I18n.format("gui.spatialshift.radar_tracking_other_dim", tileEntity.getTrackedPlayerName(), tileEntity.getTrackingTicks() / 20);
                fontRenderer.drawString(dimInfo, mapX + 4, mapY + mapH - 10, 0xFFAA88FF);
            }
        }

        if (tileEntity.getImperialEyeTicks() > 0) {
            String eyeInfo = I18n.format("gui.spatialshift.radar_eye_channeling", tileEntity.getEyeTargetPlayerName(), tileEntity.getImperialEyeTicks() / 20);
            fontRenderer.drawString(eyeInfo, mapX + 4, mapY + mapH - 10, 0xFFFF55AA);

            int pulseR = 4 + (tileEntity.getImperialEyeTicks() % 20);
            drawCircle(centerX, centerY, pulseR, 0x80FF00AA);
        }

        ItemStack artifact = tileEntity.getArtifactInventory().getStackInSlot(0);
        boolean hasShard = !artifact.isEmpty() && artifact.getItem() == ModItems.DIVINE_SIGHT_SHARD;
        boolean hasEye = !artifact.isEmpty() && artifact.getItem() == ModItems.IMPERIAL_EYE;
        boolean trackingActive = tileEntity.getTrackingTicks() > 0;
        boolean eyeActive = tileEntity.getImperialEyeTicks() > 0;

        if ((hasShard || hasEye) && !trackingActive && !eyeActive) {
            String pName = onlinePlayers.isEmpty() ? "---" : onlinePlayers.get(selectedPlayerIndex);
            if (fontRenderer.getStringWidth(pName) > 54) {
                pName = fontRenderer.trimStringToWidth(pName, 48) + "..";
            }
            int pw = fontRenderer.getStringWidth(pName);
            fontRenderer.drawString(pName, startX + 57 - pw / 2, startY + 19, 0xFFFFFFFF);
        }

        drawRect(centerX - 2, centerY - 2, centerX + 3, centerY + 3, 0xFF00AAFF);
        drawRect(centerX - 1, centerY - 1, centerX + 2, centerY + 2, 0xFFFFFFFF);

        drawRect(mapX + 135, mapY + 1, mapX + 155, mapY + 21, 0xAA0A1018);
        drawRect(mapX + 135, mapY + 1, mapX + 155, mapY + 2, 0x80336688);
        drawRect(mapX + 135, mapY + 20, mapX + 155, mapY + 21, 0x80336688);
        drawRect(mapX + 135, mapY + 1, mapX + 136, mapY + 21, 0x80336688);
        drawRect(mapX + 154, mapY + 1, mapX + 155, mapY + 21, 0x80336688);

        if (artifact.isEmpty()) {
            fontRenderer.drawString("A", mapX + 142, mapY + 7, 0x40336688);
        }

        fontRenderer.drawString("N", centerX - 2, mapY + 2, 0x6088AACC);
        fontRenderer.drawString("S", centerX - 2, mapY + mapH - 8, 0x6088AACC);
        fontRenderer.drawString("W", mapX + 3, centerY - 3, 0x6088AACC);
        fontRenderer.drawString("E", mapX + mapW - 7, centerY - 3, 0x6088AACC);
    }

    private void drawCircle(int cx, int cy, int radius, int argb) {
        float a = (float) (argb >> 24 & 255) / 255.0F;
        float r = (float) (argb >> 16 & 255) / 255.0F;
        float g = (float) (argb >> 8 & 255) / 255.0F;
        float b = (float) (argb & 255) / 255.0F;

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.color(r, g, b, a);

        GL11.glLineWidth(1.0F);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int i = 0; i < 32; i++) {
            double angle = 2.0 * Math.PI * i / 32;
            GL11.glVertex2d(cx + Math.cos(angle) * radius, cy + Math.sin(angle) * radius);
        }
        GL11.glEnd();

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    private void drawLine(int x1, int y1, int x2, int y2, int argb) {
        float a = (float) (argb >> 24 & 255) / 255.0F;
        float r = (float) (argb >> 16 & 255) / 255.0F;
        float g = (float) (argb >> 8 & 255) / 255.0F;
        float b = (float) (argb & 255) / 255.0F;

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.color(r, g, b, a);

        GL11.glLineWidth(1.5F);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2d(x1, y1);
        GL11.glVertex2d(x2, y2);
        GL11.glEnd();

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRenderer.drawString(I18n.format("gui.spatialshift.title"), 8, 4, 0x404040);

        String rangeStr = I18n.format("gui.spatialshift.radar_range", (int) calculateViewRadius());
        fontRenderer.drawString(rangeStr, 168 - fontRenderer.getStringWidth(rangeStr), 4, 0x6088AACC);

        int heatColor = tileEntity.getCurrentHeat() > 75.0F ? 0xCC2222 : (tileEntity.getCurrentHeat() > 50.0F ? 0xBB9900 : 0x228822);
        String heatLabel = I18n.format("gui.spatialshift.heat_label");
        String heatVal = String.format("%d%%", (int) tileEntity.getCurrentHeat());
        fontRenderer.drawString(heatLabel, 32, 114, 0x404040);
        fontRenderer.drawString(heatVal, 32, 124, heatColor);

        String fuelLabel = I18n.format("gui.spatialshift.fuel_label");
        String fuelVal = tileEntity.getTotalFuel() + " / " + tileEntity.getMaxFuel();
        fontRenderer.drawString(fuelLabel, 108, 114, 0x404040);
        int fuelValX = Math.min(108, 172 - fontRenderer.getStringWidth(fuelVal));
        fontRenderer.drawString(fuelVal, fuelValX, 124, 0x404040);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        renderHoveredToolTip(mouseX, mouseY);

        int startX = (width - xSize) / 2;
        int startY = (height - ySize) / 2;
        int mapX = startX + 10;
        int mapY = startY + 14;
        int mapW = 156;
        int mapH = 62;

        if (mouseX >= startX + 145 && mouseX <= startX + 163 && mouseY >= startY + 15 && mouseY <= startY + 33) {
            ItemStack artifact = tileEntity.getArtifactInventory().getStackInSlot(0);
            if (artifact.isEmpty()) {
                drawHoveringText(Arrays.asList(
                    TextFormatting.AQUA + I18n.format("gui.spatialshift.tooltip.artifact_slot"),
                    TextFormatting.GRAY + I18n.format("gui.spatialshift.tooltip.artifact_slot_desc")
                ), mouseX, mouseY);
                return;
            }
        }

        if (mouseX >= mapX && mouseX <= mapX + mapW && mouseY >= mapY && mouseY <= mapY + mapH) {
            double radius = calculateViewRadius();
            int centerX = mapX + 78;
            int centerY = mapY + 31;
            double scaleX = (mapW / 2.0) / radius;
            double scaleZ = (mapH / 2.0) / radius;
            BlockPos corePos = tileEntity.getPos();
            int currentDim = tileEntity.getWorld().provider.getDimension();

            for (AnchorData a : availableAnchors) {
                if (a.getDimensionId() == currentDim) {
                    int ax = centerX + (int) Math.round((a.getPos().getX() - corePos.getX()) * scaleX);
                    int ay = centerY + (int) Math.round((a.getPos().getZ() - corePos.getZ()) * scaleZ);
                    if (Math.hypot(mouseX - ax, mouseY - ay) <= 4.5) {
                        drawHoveringText(Arrays.asList(
                            TextFormatting.GOLD + a.getName(),
                            TextFormatting.GRAY + "[" + a.getPos().getX() + ", " + a.getPos().getY() + ", " + a.getPos().getZ() + "]",
                            TextFormatting.YELLOW + I18n.format("gui.spatialshift.tooltip.anchor_click")
                        ), mouseX, mouseY);
                        return;
                    }
                }
            }

            if (Math.hypot(mouseX - centerX, mouseY - centerY) <= 4.5) {
                drawHoveringText(Arrays.asList(
                    TextFormatting.AQUA + I18n.format("gui.spatialshift.tooltip.core", corePos.getX(), corePos.getY(), corePos.getZ())
                ), mouseX, mouseY);
                return;
            }

            try {
                int tx = Integer.parseInt(xField.getText().trim());
                int tz = Integer.parseInt(zField.getText().trim());
                int targetScreenX = centerX + (int) Math.round((tx - corePos.getX()) * scaleX);
                int targetScreenY = centerY + (int) Math.round((tz - corePos.getZ()) * scaleZ);
                if (Math.hypot(mouseX - targetScreenX, mouseY - targetScreenY) <= 4.5) {
                    double dist = Math.hypot(tx - corePos.getX(), tz - corePos.getZ());
                    drawHoveringText(Arrays.asList(
                        TextFormatting.RED + I18n.format("gui.spatialshift.tooltip.target", tx, Integer.parseInt(yField.getText().trim()), tz, (int) dist)
                    ), mouseX, mouseY);
                    return;
                }
            } catch (NumberFormatException ignored) {
            }

            int hoverX = corePos.getX() + (int) Math.round((mouseX - centerX) / scaleX);
            int hoverZ = corePos.getZ() + (int) Math.round((mouseY - centerY) / scaleZ);
            drawHoveringText(Arrays.asList(
                TextFormatting.WHITE + "[" + hoverX + ", ~ , " + hoverZ + "]",
                TextFormatting.DARK_AQUA + I18n.format("gui.spatialshift.tooltip.map_click")
            ), mouseX, mouseY);
            return;
        }

        if (buttonMode != null && buttonMode.isMouseOver()) {
            if (teleportMode == TeleportMode.LANDING) {
                drawHoveringText(I18n.format("gui.spatialshift.tooltip.mode_landing"), mouseX, mouseY);
            } else {
                drawHoveringText(I18n.format("gui.spatialshift.tooltip.mode_air"), mouseX, mouseY);
            }
        } else if (buttonSafetyLock != null && buttonSafetyLock.isMouseOver()) {
            drawHoveringText(Arrays.asList(
                I18n.format("gui.spatialshift.tooltip.safety1"),
                I18n.format("gui.spatialshift.tooltip.safety2")
            ), mouseX, mouseY);
        } else if (mouseX >= startX + 10 && mouseX <= startX + 28 && mouseY >= startY + 114 && mouseY <= startY + 132) {
            drawHoveringText(I18n.format("gui.spatialshift.coolant"), mouseX, mouseY);
        } else if (mouseX >= startX + 88 && mouseX <= startX + 106 && mouseY >= startY + 114 && mouseY <= startY + 132) {
            drawHoveringText(I18n.format("gui.spatialshift.fuel"), mouseX, mouseY);
        }
    }
}
