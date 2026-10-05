package com.spatialshift.gui;

import com.spatialshift.container.ContainerAnchor;
import com.spatialshift.data.AnchorAccess;
import com.spatialshift.network.PacketHandler;
import com.spatialshift.network.packet.PacketUpdateAnchor;
import com.spatialshift.tileentity.TileEntityAnchor;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

public class GuiAnchor extends GuiContainer {

    private final TileEntityAnchor anchor;
    private GuiTextField nameField;
    private GuiButton buttonAccess;
    private GuiButton buttonDone;
    private AnchorAccess currentAccess;

    public GuiAnchor(TileEntityAnchor anchor) {
        super(new ContainerAnchor(anchor));
        this.anchor = anchor;
        this.xSize = 176;
        this.ySize = 96;
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);

        int startX = (width - xSize) / 2;
        int startY = (height - ySize) / 2;

        nameField = new GuiTextField(0, fontRenderer, startX + 10, startY + 36, 156, 16);
        nameField.setText(anchor.getAnchorName());
        nameField.setMaxStringLength(32);
        nameField.setFocused(true);

        currentAccess = anchor.getAccess();

        buttonAccess = addButton(new GuiButton(1, startX + 10, startY + 62, 80, 20, getAccessButtonText()));
        buttonDone = addButton(new GuiButton(2, startX + 96, startY + 62, 70, 20, I18n.format("gui.done")));
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
    }

    private String getAccessButtonText() {
        return currentAccess == AnchorAccess.PUBLIC
            ? TextFormatting.GREEN + I18n.format("access.spatialshift.public")
            : TextFormatting.RED + I18n.format("access.spatialshift.private");
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 1) {
            currentAccess = currentAccess.next();
            buttonAccess.displayString = getAccessButtonText();
        } else if (button.id == 2) {
            saveAndClose();
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (nameField.textboxKeyTyped(typedChar, keyCode)) {
            return;
        }

        if (keyCode == 28 || keyCode == 156) {
            saveAndClose();
            return;
        }

        if (keyCode == 1) {
            mc.player.closeScreen();
            return;
        }

        super.keyTyped(typedChar, keyCode);
    }

    private void saveAndClose() {
        String text = nameField.getText().trim();
        if (text.isEmpty()) {
            text = "Anchor";
        }
        PacketHandler.sendToServer(new PacketUpdateAnchor(anchor.getPos(), text, currentAccess));
        mc.player.closeScreen();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        nameField.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        int startX = (width - xSize) / 2;
        int startY = (height - ySize) / 2;

        drawRect(startX, startY, startX + xSize, startY + ySize, 0xFFC6C6C6);

        drawRect(startX, startY, startX + xSize, startY + 1, 0xFFFFFFFF);
        drawRect(startX, startY, startX + 1, startY + ySize, 0xFFFFFFFF);
        drawRect(startX + xSize - 1, startY, startX + xSize, startY + ySize, 0xFF373737);
        drawRect(startX, startY + ySize - 1, startX + xSize, startY + ySize, 0xFF373737);

        nameField.drawTextBox();
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRenderer.drawString(I18n.format("gui.spatialshift.anchor_title"), 8, 6, 0x404040);
        String posText = I18n.format("gui.spatialshift.anchor_pos", anchor.getPos().getX(), anchor.getPos().getY(), anchor.getPos().getZ());
        fontRenderer.drawString(posText, 8, 22, 0x555555);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        renderHoveredToolTip(mouseX, mouseY);
    }
}
