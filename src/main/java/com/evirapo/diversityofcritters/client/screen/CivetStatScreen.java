package com.evirapo.diversityofcritters.client.screen;

import com.evirapo.diversityofcritters.DiversityOfCritters;
import com.evirapo.diversityofcritters.client.menu.DOCStatsMenu;
import com.evirapo.diversityofcritters.common.entity.custom.base.DiverseCritter;
import com.evirapo.diversityofcritters.network.DOCNetworkHandler;
import com.evirapo.diversityofcritters.network.SetDiurnalMsg;
import com.evirapo.diversityofcritters.network.ReleaseCritterMsg;
import com.evirapo.diversityofcritters.network.ToggleBreedMsg;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class CivetStatScreen extends AbstractContainerScreen<DOCStatsMenu> {

    // ===== TEXTURES =====
    private static final ResourceLocation GUI_TEXTURE  = new ResourceLocation(DiversityOfCritters.MODID, "textures/gui/gui_civet.png");
    private static final ResourceLocation BARS_TEXTURE = new ResourceLocation(DiversityOfCritters.MODID, "textures/gui/bars.png");
    private static final ResourceLocation SUN_ICON     = new ResourceLocation(DiversityOfCritters.MODID, "textures/gui/sun_icon.png");
    private static final ResourceLocation MOON_ICON    = new ResourceLocation(DiversityOfCritters.MODID, "textures/gui/moon_icon.png");
    private static final ResourceLocation RELEASE_ICON = new ResourceLocation(DiversityOfCritters.MODID, "textures/gui/release_icon.png");
    private static final ResourceLocation MALE_ICON    = new ResourceLocation(DiversityOfCritters.MODID, "textures/gui/male_icon.png");
    private static final ResourceLocation FEMALE_ICON  = new ResourceLocation(DiversityOfCritters.MODID, "textures/gui/female_icon.png");

    // TEXTURAS NUEVAS PARA LA FASE 4 (Corregidas según tus indicaciones)
    private static final ResourceLocation PREGNANCY_ICON = new ResourceLocation(DiversityOfCritters.MODID, "textures/gui/pregnancy_icon.png");
    private static final ResourceLocation PILL_ICON      = new ResourceLocation(DiversityOfCritters.MODID, "textures/gui/pill_icon.png");

    private static final int TEX_W = 338;
    private static final int TEX_H = 155;
    private static final int BAR_TEX_W = 57;
    private static final int BAR_TEX_H = 96;

    private static final float SCALE = 1.2f;
    private static final int GUI_OFFSET_Y = -6;

    private static final int SCALED_W = (int)(TEX_W * SCALE);
    private static final int SCALED_H = (int)(TEX_H * SCALE);

    private static final int BAR_Y_START = 44;
    private static final int BAR_HEIGHT = 96;
    private static final int[] BAR_SLOT_X = { 220, 230, 240, 250, 260, 270 };
    private static final int BAR_SLOT_WIDTH = 7;
    private static final int BAR_COLOR_WIDTH = 7;
    private static final int[] BAR_U = { 0, 10, 20, 30, 40, 50 };

    private static final int ENTITY_TEX_X = 115;
    private static final int ENTITY_TEX_Y = 100;
    private static final int ENTITY_SIZE = 54;
    private static final int ENTITY_OFFSET_X = -18;
    private static final int ENTITY_LOOK_Y = -40;

    private static final int BTN_TEX_X = 286;
    private static final int BTN_TEX_Y = 45;
    private static final int BTN_SPACING = 20;
    private static final int BTN_SIZE = 16;

    private static final int NAMETAG_OFFSET_X = -16;
    private static final int NAMETAG_OFFSET_Y = 20;
    private static final float NAMETAG_SCALE = 1.5f;
    private static final int NAMETAG_COLOR = 0xFFFFFF;

    private static final int TITLE_OFFSET_X = 20;
    private static final int TITLE_OFFSET_Y = 38;
    private static final int TITLE_COLOR = 0xFFFFFF;

    private static final float BAR_LABEL_SCALE = 0.55f;
    private static final int BAR_LABEL_COLOR = 0xFFFFFF;

    private static final int INFO_START_TEX_X = 145;
    private static final int INFO_START_TEX_Y = 60;
    private static final int INFO_LINE_SPACING = 16;
    private static final float INFO_TEXT_SCALE = 0.85f;
    private static final int INFO_TEXT_COLOR = 0xFFFFFF;

    // ╔════════════════════════════════════════════════════════════════════════╗
    // ║ ICONOS (GÉNERO Y EMBARAZO)                                          ║
    // ╚════════════════════════════════════════════════════════════════════════╝
    private static final int GENDER_ICON_X = 56;
    private static final int GENDER_ICON_Y = 110;
    private static final int GENDER_ICON_WIDTH = 30;
    private static final int GENDER_ICON_HEIGHT = 15;
    private static final float GENDER_ICON_SCALE = 1.0f;

    // Movido más abajo y 20 pixeles a la derecha respecto al de género
    private static final int PREGNANCY_ICON_X = GENDER_ICON_X + 10;
    private static final int PREGNANCY_ICON_Y = GENDER_ICON_Y + 15;
    private static final int PREGNANCY_ICON_WIDTH = 30;
    private static final int PREGNANCY_ICON_HEIGHT = 15;
    private static final float PREGNANCY_ICON_SCALE = 1.0f;

    // ===== Variables internas =====
    private final DiverseCritter entity;
    private ImageButton sunBtn;
    private ImageButton moonBtn;
    private ImageButton releaseBtn;

    // Botón único de Píldora
    private ImageButton pillBtn;

    public CivetStatScreen(DOCStatsMenu container, Inventory inventory, DiverseCritter critter) {
        super(container, inventory, critter.getDisplayName());
        this.entity = critter;
        this.imageWidth = SCALED_W;
        this.imageHeight = SCALED_H;
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();

        int guiLeft = (this.width - SCALED_W) / 2;
        int guiTop = (this.height - SCALED_H) / 2 + GUI_OFFSET_Y;
        this.topPos = guiTop;

        int btnX = guiLeft + (int)(BTN_TEX_X * SCALE);
        int btnY = guiTop + (int)(BTN_TEX_Y * SCALE);

        this.sunBtn = new ImageButton(btnX, btnY, BTN_SIZE, BTN_SIZE, 0, 0, BTN_SIZE, SUN_ICON, BTN_SIZE, BTN_SIZE, btn -> sendSetDiurnal(true), Component.translatable("screen.diversityofcritters.diurnal"));
        this.addRenderableWidget(this.sunBtn);

        this.moonBtn = new ImageButton(btnX, btnY + BTN_SPACING, BTN_SIZE, BTN_SIZE, 0, 0, BTN_SIZE, MOON_ICON, BTN_SIZE, BTN_SIZE, btn -> sendSetDiurnal(false), Component.translatable("screen.diversityofcritters.nocturnal"));
        this.addRenderableWidget(this.moonBtn);

        this.releaseBtn = new ImageButton(btnX, btnY + BTN_SPACING * 2, BTN_SIZE, BTN_SIZE, 0, 0, BTN_SIZE, RELEASE_ICON, BTN_SIZE, BTN_SIZE, btn -> sendRelease(), Component.translatable("screen.diversityofcritters.release"));
        this.addRenderableWidget(this.releaseBtn);

        this.pillBtn = new ImageButton(btnX, btnY + BTN_SPACING * 3, BTN_SIZE, BTN_SIZE, 0, 0, BTN_SIZE, PILL_ICON, BTN_SIZE, BTN_SIZE,
                btn -> sendToggleBreed(!this.entity.canBreed()),
                Component.translatable("screen.diversityofcritters.toggle_breed"));
        this.addRenderableWidget(this.pillBtn);

        updateButtonsVisibility();
    }

    // ==============================
    //       NETWORK
    // ==============================
    private void sendSetDiurnal(boolean value) {
        DOCNetworkHandler.CHANNEL.sendToServer(new SetDiurnalMsg(this.entity.getId(), value));
    }
    private void sendRelease() {
        if (canRelease()) DOCNetworkHandler.CHANNEL.sendToServer(new ReleaseCritterMsg(this.entity.getId()));
    }
    private void sendToggleBreed(boolean value) {
        DOCNetworkHandler.CHANNEL.sendToServer(new ToggleBreedMsg(this.entity.getId(), value));
    }

    // ==============================
    //       INPUT & TICK
    // ==============================
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = false;
        if (this.sunBtn != null && this.sunBtn.mouseClicked(mouseX, mouseY, button)) handled = true;
        if (this.moonBtn != null && this.moonBtn.mouseClicked(mouseX, mouseY, button)) handled = true;
        if (this.releaseBtn != null && this.releaseBtn.mouseClicked(mouseX, mouseY, button)) handled = true;

        // Clic para la píldora
        if (this.pillBtn != null && this.pillBtn.mouseClicked(mouseX, mouseY, button)) handled = true;
        return handled || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean handled = false;
        if (this.sunBtn != null && this.sunBtn.mouseReleased(mouseX, mouseY, button)) handled = true;
        if (this.moonBtn != null && this.moonBtn.mouseReleased(mouseX, mouseY, button)) handled = true;
        if (this.releaseBtn != null && this.releaseBtn.mouseReleased(mouseX, mouseY, button)) handled = true;
        if (this.pillBtn != null && this.pillBtn.mouseReleased(mouseX, mouseY, button)) handled = true;
        return handled || super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) return super.keyPressed(keyCode, scanCode, modifiers);
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateButtonsVisibility();
    }

    private boolean canEdit() {
        if (this.minecraft == null || this.minecraft.player == null) return false;
        return this.entity.isOwnedBy(this.minecraft.player);
    }
    private boolean canRelease() {
        return canEdit() && this.entity.isTame();
    }

    private void updateButtonsVisibility() {
        boolean isOwner = canEdit();

        if (this.sunBtn != null) { this.sunBtn.visible = isOwner; this.sunBtn.active = isOwner; }
        if (this.moonBtn != null) { this.moonBtn.visible = isOwner; this.moonBtn.active = isOwner; }
        if (this.releaseBtn != null) { this.releaseBtn.visible = canRelease(); this.releaseBtn.active = canRelease(); }

        // Lógica visual del botón Píldora
        if (this.pillBtn != null) {
            this.pillBtn.visible = isOwner;
            this.pillBtn.active = isOwner;
        }
    }

    // ==============================
    //          RENDER
    // ==============================
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int guiLeft = (this.width - SCALED_W) / 2;
        int guiTop = (this.height - SCALED_H) / 2 + GUI_OFFSET_Y;

        PoseStack pose = graphics.pose();

        pose.pushPose();
        pose.translate(guiLeft, guiTop, 0);
        pose.scale(SCALE, SCALE, 1.0f);

        graphics.blit(GUI_TEXTURE, 0, 0, 0, 0, TEX_W, TEX_H, TEX_W, TEX_H);

        float hungerPct     = entity.getHungerPercentage() / 100f;
        float thirstPct     = entity.getThirstPercentage() / 100f;
        float enrichmentPct = entity.getEnrichmentPercentage() / 100f;
        float temperaturePct = 1.0f;
        float humidityPct    = 1.0f;
        float hygienePct     = entity.getHygienePercentage() / 100f;

        float[] pcts = { hungerPct, thirstPct, enrichmentPct, temperaturePct, humidityPct, hygienePct };

        for (int i = 0; i < 6; i++) {
            int filled = (int)(BAR_HEIGHT * pcts[i]);
            if (filled <= 0) continue;
            int destX = BAR_SLOT_X[i];
            int destY = BAR_Y_START + (BAR_HEIGHT - filled);
            graphics.blit(BARS_TEXTURE, destX, destY, BAR_U[i], BAR_HEIGHT - filled, BAR_COLOR_WIDTH, filled, BAR_TEX_W, BAR_TEX_H);
        }

        // --- RENDER DEL ICONO DE GÉNERO ---
        ResourceLocation genderIcon = entity.getIsMale() ? MALE_ICON : FEMALE_ICON;
        pose.pushPose();
        pose.translate(GENDER_ICON_X, GENDER_ICON_Y, 0);
        pose.scale(GENDER_ICON_SCALE, GENDER_ICON_SCALE, 1.0f);
        graphics.blit(genderIcon, 0, 0, 0, 0, GENDER_ICON_WIDTH, GENDER_ICON_HEIGHT, GENDER_ICON_WIDTH, GENDER_ICON_HEIGHT);
        pose.popPose();

        // --- RENDER DEL ICONO DE EMBARAZO (Solo si es hembra y está preñada) ---
        if (!entity.getIsMale() && entity.isPregnant()) {
            pose.pushPose();
            pose.translate(PREGNANCY_ICON_X, PREGNANCY_ICON_Y, 0);
            pose.scale(PREGNANCY_ICON_SCALE, PREGNANCY_ICON_SCALE, 1.0f);
            graphics.blit(PREGNANCY_ICON, 0, 0, 0, 0, PREGNANCY_ICON_WIDTH, PREGNANCY_ICON_HEIGHT, PREGNANCY_ICON_WIDTH, PREGNANCY_ICON_HEIGHT);
            pose.popPose();
        }

        pose.popPose();

        // ===== Render de la entidad =====
        int entityX = guiLeft + (int)(ENTITY_TEX_X * SCALE);
        int entityY = guiTop + (int)(ENTITY_TEX_Y * SCALE);
        int entitySize = (int)(ENTITY_SIZE * SCALE);

        boolean wasNameVisible = entity.isCustomNameVisible();
        Component savedName = entity.getCustomName();
        entity.setCustomNameVisible(false);
        entity.setCustomName(null);

        InventoryScreen.renderEntityInInventoryFollowsMouse(
                graphics,
                entityX + ENTITY_OFFSET_X, entityY,
                entitySize,
                -mouseX + entityX, -mouseY + entityY + ENTITY_LOOK_Y,
                entity
        );

        entity.setCustomName(savedName);
        entity.setCustomNameVisible(wasNameVisible);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        PoseStack pose = graphics.pose();

        String nametag = entity.getCustomName() != null ? entity.getCustomName().getString() : "N/A";
        int nametagW = this.font.width(nametag);

        pose.pushPose();
        float nametagCenterX = (SCALED_W / 2.0f) + NAMETAG_OFFSET_X;
        pose.translate(nametagCenterX, NAMETAG_OFFSET_Y, 0);
        pose.scale(NAMETAG_SCALE, NAMETAG_SCALE, 1.0f);
        graphics.drawString(this.font, nametag, -nametagW / 2, 0, NAMETAG_COLOR, true);
        pose.popPose();

        String title = "The Banded Palm Civet";
        int titleW = this.font.width(title);
        graphics.drawString(this.font, title, ((SCALED_W - titleW) / 2) + TITLE_OFFSET_X, TITLE_OFFSET_Y, TITLE_COLOR, false);

        String[] barLabels = { "Hunger", "Thirst", "Enrichment", "Temperature", "Humidity", "Hygiene" };
        for (int i = 0; i < 6; i++) {
            int barCenterX = (int)((BAR_SLOT_X[i] + BAR_SLOT_WIDTH / 2.0f) * SCALE);
            int barTop = (int)(BAR_Y_START * SCALE);
            int barBottom = (int)((BAR_Y_START + BAR_HEIGHT) * SCALE);
            int barMidY = (barTop + barBottom) / 2;

            pose.pushPose();
            pose.translate(barCenterX, barMidY, 0);
            pose.scale(BAR_LABEL_SCALE, BAR_LABEL_SCALE, 1.0f);
            pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-90));
            int textW = this.font.width(barLabels[i]);
            graphics.drawString(this.font, barLabels[i], -textW / 2, -4, BAR_LABEL_COLOR, true);
            pose.popPose();
        }

        int startX = (int)(INFO_START_TEX_X * SCALE);
        int startY = (int)(INFO_START_TEX_Y * SCALE);

        pose.pushPose();
        pose.scale(INFO_TEXT_SCALE, INFO_TEXT_SCALE, 1.0f);

        int sx = (int)(startX / INFO_TEXT_SCALE);
        int sy = (int)(startY / INFO_TEXT_SCALE);
        int lineH = (int)(INFO_LINE_SPACING / INFO_TEXT_SCALE);

        String sci = "Hemigalus derbyanus";
        graphics.drawString(this.font, sci, sx, sy, INFO_TEXT_COLOR, true);

        String hp = "Health: " + (int)entity.getHealth() + "/" + (int)entity.getMaxHealth();
        graphics.drawString(this.font, hp, sx, sy + lineH, INFO_TEXT_COLOR, true);

        Component ageVal = Component.translatable("screen.diversityofcritters.is_baby." + entity.isBaby());
        Component ageKey = Component.translatable("screen.diversityofcritters.age");
        String age = ageKey.getString() + ": " + ageVal.getString();
        graphics.drawString(this.font, age, sx, sy + lineH * 2, INFO_TEXT_COLOR, true);

        String morph = "Morph: Gray (0)";
        graphics.drawString(this.font, morph, sx, sy + lineH * 3, INFO_TEXT_COLOR, true);

        String breedStatus = "Breeding: " + (entity.canBreed() ? "ON" : "OFF");
        graphics.drawString(this.font, breedStatus, sx, sy + lineH * 4, INFO_TEXT_COLOR, true);

        String status = "Status: " + (entity.isTame() ? "Tamed" : "Wild");
        graphics.drawString(this.font, status, sx, sy + lineH * 5, INFO_TEXT_COLOR, true);

        pose.popPose();
    }
}