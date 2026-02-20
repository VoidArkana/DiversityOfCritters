package com.evirapo.diversityofcritters.client.screen;

import com.evirapo.diversityofcritters.DiversityOfCritters;
import com.evirapo.diversityofcritters.client.menu.DOCStatsMenu;
import com.evirapo.diversityofcritters.common.entity.custom.base.DiverseCritter;
import com.evirapo.diversityofcritters.network.DOCNetworkHandler;
import com.evirapo.diversityofcritters.network.SetDiurnalMsg;
import com.evirapo.diversityofcritters.network.ReleaseCritterMsg;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class CivetStatScreen extends AbstractContainerScreen<DOCStatsMenu> {

    // ===== TEXTURES (no tocar) =====
    private static final ResourceLocation GUI_TEXTURE  = new ResourceLocation(DiversityOfCritters.MODID, "textures/gui/gui_civet.png");
    private static final ResourceLocation BARS_TEXTURE = new ResourceLocation(DiversityOfCritters.MODID, "textures/gui/bars.png");
    private static final ResourceLocation SUN_ICON     = new ResourceLocation(DiversityOfCritters.MODID, "textures/gui/sun_icon.png");
    private static final ResourceLocation MOON_ICON    = new ResourceLocation(DiversityOfCritters.MODID, "textures/gui/moon_icon.png");
    private static final ResourceLocation RELEASE_ICON = new ResourceLocation(DiversityOfCritters.MODID, "textures/gui/release_icon.png");
    private static final ResourceLocation MALE_ICON    = new ResourceLocation(DiversityOfCritters.MODID, "textures/gui/male_icon.png");
    private static final ResourceLocation FEMALE_ICON  = new ResourceLocation(DiversityOfCritters.MODID, "textures/gui/female_icon.png");

    // ===== Tamaño original de las texturas (no tocar) =====
    private static final int TEX_W = 338;       // ancho de gui_civet.png
    private static final int TEX_H = 155;       // alto de gui_civet.png
    private static final int BAR_TEX_W = 57;    // ancho de bars.png
    private static final int BAR_TEX_H = 96;    // alto de bars.png

    // ╔════════════════════════════════════════════════════════════════════════╗
    // ║ [A] ESCALA GENERAL Y POSICIÓN DE LA GUI EN PANTALLA                 ║
    // ║     SCALE = agrandar/achicar todo (1.0=original, 1.2=20% más grande)║
    // ║     GUI_OFFSET_Y = mover la GUI entera arriba/abajo                 ║
    // ║                    Positivo = más abajo | Negativo = más arriba     ║
    // ╚════════════════════════════════════════════════════════════════════════╝
    private static final float SCALE = 1.2f;
    private static final int GUI_OFFSET_Y = -6;

    // Dimensiones escaladas (se calculan solas, no tocar)
    private static final int SCALED_W = (int)(TEX_W * SCALE);
    private static final int SCALED_H = (int)(TEX_H * SCALE);

    // ╔════════════════════════════════════════════════════════════════════════╗
    // ║ [B] BARRAS DE STATS - posiciones dentro de la textura original       ║
    // ║     Todas estas coordenadas son en pixeles de la textura (pre-escala)║
    // ║     BAR_Y_START  = Y donde empieza el área de barras (arriba)       ║
    // ║     BAR_HEIGHT   = alto total disponible para llenar (96=bars.png)   ║
    // ║     BAR_SLOT_X   = posición X de cada una de las 6 barras           ║
    // ║                    Orden: Hunger,Thirst,Enrichment,Temp,Humid,Hygiene║
    // ║     BAR_SLOT_WIDTH = ancho de cada ranura en la GUI                  ║
    // ║     BAR_COLOR_WIDTH = ancho del color que se dibuja                  ║
    // ║     BAR_U         = posición U (horizontal) en bars.png para cada   ║
    // ╚════════════════════════════════════════════════════════════════════════╝
    private static final int BAR_Y_START = 44;
    private static final int BAR_HEIGHT = 96;
    private static final int[] BAR_SLOT_X = { 220, 230, 240, 250, 260, 270 };
    private static final int BAR_SLOT_WIDTH = 7;
    private static final int BAR_COLOR_WIDTH = 7;
    private static final int[] BAR_U = { 0, 10, 20, 30, 40, 50 };

    // ╔════════════════════════════════════════════════════════════════════════╗
    // ║ [C] RENDER DE LA ENTIDAD (el animal 3D)                             ║
    // ║     Coordenadas en textura original (se multiplican x SCALE)        ║
    // ║     ENTITY_TEX_X  = posición X (más grande = más a la derecha)      ║
    // ║     ENTITY_TEX_Y  = posición Y del pie (más grande = más abajo)     ║
    // ║     ENTITY_SIZE   = tamaño del modelo (más grande = animal más big) ║
    // ║     ENTITY_OFFSET_X = ajuste fino horizontal en pantalla            ║
    // ║     ENTITY_LOOK_Y   = offset de dónde mira la cabeza               ║
    // ╚════════════════════════════════════════════════════════════════════════╝
    private static final int ENTITY_TEX_X = 115;
    private static final int ENTITY_TEX_Y = 100;
    private static final int ENTITY_SIZE = 54;
    private static final int ENTITY_OFFSET_X = -18;
    private static final int ENTITY_LOOK_Y = -40;

    // ╔════════════════════════════════════════════════════════════════════════╗
    // ║ [D] BOTONES (sol, luna, release) - posición en textura original      ║
    // ║     Se multiplican x SCALE automaticamente                          ║
    // ║     BTN_TEX_X   = posición X (290 = justo al borde derecho del gui) ║
    // ║     BTN_TEX_Y   = posición Y del primer botón                       ║
    // ║     BTN_SPACING = separación vertical entre cada botón              ║
    // ║     BTN_SIZE    = tamaño de cada botón (pixeles de pantalla)         ║
    // ╚════════════════════════════════════════════════════════════════════════╝
    private static final int BTN_TEX_X = 286;
    private static final int BTN_TEX_Y = 45;
    private static final int BTN_SPACING = 20;
    private static final int BTN_SIZE = 16;

    // ╔════════════════════════════════════════════════════════════════════════╗
    // ║ [E] NAMETAG + TÍTULO - arriba de la GUI                             ║
    // ║     *_OFFSET_X = empujar a los lados (0=centrado, -=izq, +=der)     ║
    // ║     *_OFFSET_Y = posición arriba/abajo                              ║
    // ║     Para que estén alineados, pon el mismo valor en ambos OFFSET_X  ║
    // ╚════════════════════════════════════════════════════════════════════════╝
    // NAMETAG
    private static final int NAMETAG_OFFSET_X = -16;   // Mueve el Nametag en X
    private static final int NAMETAG_OFFSET_Y = 20;  // Mueve el Nametag en Y
    private static final float NAMETAG_SCALE = 1.5f;
    private static final int NAMETAG_COLOR = 0xFFFFFF;

    // TÍTULO DE LA ESPECIE
    private static final int TITLE_OFFSET_X = 20;     // Mueve el Título en X
    private static final int TITLE_OFFSET_Y = 38;    // Mueve el Título en Y
    private static final int TITLE_COLOR = 0xFFFFFF;

    // ╔════════════════════════════════════════════════════════════════════════╗
    // ║ [F] TEXTOS DE LAS BARRAS (Hunger, Thirst, etc - texto vertical)     ║
    // ║     BAR_LABEL_SCALE = escala del texto (0.55 = 55% del normal)      ║
    // ║     BAR_LABEL_COLOR = color (0xFFFFFF = blanco)                     ║
    // ║     Los labels se centran automáticamente en cada barra              ║
    // ╚════════════════════════════════════════════════════════════════════════╝
    private static final float BAR_LABEL_SCALE = 0.55f;
    private static final int BAR_LABEL_COLOR = 0xFFFFFF;

    // ╔════════════════════════════════════════════════════════════════════════╗
    // ║ [G] TEXTOS DE INFORMACIÓN CENTRAL (Scientific name, Health, etc)    ║
    // ║     Coordenadas en textura original (se multiplican x SCALE)        ║
    // ║     INFO_START_TEX_X  = posición X donde empieza el texto (izq)     ║
    // ║     INFO_START_TEX_Y  = posición Y de la primera línea              ║
    // ║     INFO_LINE_SPACING = separación entre cada línea de texto         ║
    // ║     INFO_TEXT_SCALE   = escala del texto (0.7 = 70% del normal)     ║
    // ║     INFO_TEXT_COLOR   = color (0xFFFFFF = blanco)                    ║
    // ╚════════════════════════════════════════════════════════════════════════╝
    private static final int INFO_START_TEX_X = 145;
    private static final int INFO_START_TEX_Y = 60;
    private static final int INFO_LINE_SPACING = 16;
    private static final float INFO_TEXT_SCALE = 0.85f;
    private static final int INFO_TEXT_COLOR = 0xFFFFFF;

    // ╔════════════════════════════════════════════════════════════════════════╗
    // ║ [H] ICONO DE GÉNERO (Macho / Hembra)                                ║
    // ║     GENDER_ICON_X = Mover en X (65 es a la izq del animal)          ║
    // ║     GENDER_ICON_Y = Mover en Y (110 es un poco más abajo)           ║
    // ║     GENDER_ICON_WIDTH = Ancho del png en pixeles (ej. 30)           ║
    // ║     GENDER_ICON_HEIGHT = Alto del png en pixeles (ej. 15)           ║
    // ║     GENDER_ICON_SCALE = Para hacerlo más grande o más pequeño       ║
    // ╚════════════════════════════════════════════════════════════════════════╝
    private static final int GENDER_ICON_X = 50;
    private static final int GENDER_ICON_Y = 110;
    private static final int GENDER_ICON_WIDTH = 30;   // <-- Ancho de tu imagen
    private static final int GENDER_ICON_HEIGHT = 15;  // <-- Alto de tu imagen
    private static final float GENDER_ICON_SCALE = 1.0f; // <-- Cambia a 0.5f para mitad de tamaño, 1.5f para más grande, etc.

    // ===== Variables internas =====
    private final DiverseCritter entity;
    private ImageButton sunBtn;
    private ImageButton moonBtn;
    private ImageButton releaseBtn;

    public CivetStatScreen(DOCStatsMenu container, Inventory inventory, DiverseCritter critter) {
        super(container, inventory, critter.getDisplayName());
        this.entity = critter;
        this.imageWidth = SCALED_W;
        this.imageHeight = SCALED_H;
    }

    // ==============================
    //           INIT
    // ==============================
    @Override
    protected void init() {
        super.init();
        this.clearWidgets();

        int guiLeft = (this.width - SCALED_W) / 2;
        int guiTop = (this.height - SCALED_H) / 2 + GUI_OFFSET_Y;

        // Actualizar topPos para que renderLabels use el offset
        this.topPos = guiTop;

        // Posición de botones: BTN_TEX_X/Y * SCALE + offset de la gui en pantalla
        int btnX = guiLeft + (int)(BTN_TEX_X * SCALE);
        int btnY = guiTop + (int)(BTN_TEX_Y * SCALE);

        this.sunBtn = new ImageButton(
                btnX, btnY, BTN_SIZE, BTN_SIZE,
                0, 0, BTN_SIZE, SUN_ICON, BTN_SIZE, BTN_SIZE,
                btn -> sendSetDiurnal(true),
                Component.translatable("screen.diversityofcritters.diurnal")
        );
        this.addRenderableWidget(this.sunBtn);

        this.moonBtn = new ImageButton(
                btnX, btnY + BTN_SPACING, BTN_SIZE, BTN_SIZE,
                0, 0, BTN_SIZE, MOON_ICON, BTN_SIZE, BTN_SIZE,
                btn -> sendSetDiurnal(false),
                Component.translatable("screen.diversityofcritters.nocturnal")
        );
        this.addRenderableWidget(this.moonBtn);

        this.releaseBtn = new ImageButton(
                btnX, btnY + BTN_SPACING * 2, BTN_SIZE, BTN_SIZE,
                0, 0, BTN_SIZE, RELEASE_ICON, BTN_SIZE, BTN_SIZE,
                btn -> sendRelease(),
                Component.translatable("screen.diversityofcritters.release")
        );
        this.addRenderableWidget(this.releaseBtn);

        // ╔══════════════════════════════════════════════════════════════════╗
        // ║ BOTONES: siempre visibles por ahora para posicionar.           ║
        // ║ Cuando termines de ajustar, descomenta estas 2 líneas          ║
        // ║ para que solo se muestren si el animal es tuyo (tamed+owned):  ║
        // ║   setDiurnalButtonsEnabled(canEditDiurnal());                  ║
        // ║   setReleaseButtonEnabled(canRelease());                       ║
        // ╚══════════════════════════════════════════════════════════════════╝
    }

    // ==============================
    //       NETWORK
    // ==============================
    private void sendSetDiurnal(boolean value) {
        DOCNetworkHandler.CHANNEL.sendToServer(new SetDiurnalMsg(this.entity.getId(), value));
    }

    private void sendRelease() {
        if (canRelease()) {
            DOCNetworkHandler.CHANNEL.sendToServer(new ReleaseCritterMsg(this.entity.getId()));
        }
    }

    // ==============================
    //       INPUT
    // ==============================
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = false;
        if (this.sunBtn != null && this.sunBtn.mouseClicked(mouseX, mouseY, button)) handled = true;
        if (this.moonBtn != null && this.moonBtn.mouseClicked(mouseX, mouseY, button)) handled = true;
        if (this.releaseBtn != null && this.releaseBtn.mouseClicked(mouseX, mouseY, button)) handled = true;
        return handled || true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean handled = false;
        if (this.sunBtn != null && this.sunBtn.mouseReleased(mouseX, mouseY, button)) handled = true;
        if (this.moonBtn != null && this.moonBtn.mouseReleased(mouseX, mouseY, button)) handled = true;
        if (this.releaseBtn != null && this.releaseBtn.mouseReleased(mouseX, mouseY, button)) handled = true;
        return handled || true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        boolean handled = false;
        if (this.sunBtn != null && this.sunBtn.mouseDragged(mouseX, mouseY, button, dragX, dragY)) handled = true;
        if (this.moonBtn != null && this.moonBtn.mouseDragged(mouseX, mouseY, button, dragX, dragY)) handled = true;
        if (this.releaseBtn != null && this.releaseBtn.mouseDragged(mouseX, mouseY, button, dragX, dragY)) handled = true;
        return handled || true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) { return true; }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) return super.keyPressed(keyCode, scanCode, modifiers);
        return true;
    }

    // ==============================
    //        TICK
    // ==============================
    @Override
    protected void containerTick() {
        super.containerTick();
        setDiurnalButtonsEnabled(canEditDiurnal());
        setReleaseButtonEnabled(canRelease());
    }

    private boolean canEditDiurnal() {
        if (this.minecraft == null || this.minecraft.player == null) return false;
        return this.entity.isOwnedBy(this.minecraft.player);
    }

    private boolean canRelease() {
        if (this.minecraft == null || this.minecraft.player == null) return false;
        return this.entity.isTame() && this.entity.isOwnedBy(this.minecraft.player);
    }

    private void setDiurnalButtonsEnabled(boolean enabled) {
        if (this.sunBtn != null) { this.sunBtn.visible = enabled; this.sunBtn.active = enabled; }
        if (this.moonBtn != null) { this.moonBtn.visible = enabled; this.moonBtn.active = enabled; }
    }

    private void setReleaseButtonEnabled(boolean enabled) {
        if (this.releaseBtn != null) { this.releaseBtn.visible = enabled; this.releaseBtn.active = enabled; }
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

        // ===== Dibujar fondo GUI + barras (todo escalado con SCALE) =====
        pose.pushPose();
        pose.translate(guiLeft, guiTop, 0);
        pose.scale(SCALE, SCALE, 1.0f);

        // Fondo
        graphics.blit(GUI_TEXTURE, 0, 0, 0, 0, TEX_W, TEX_H, TEX_W, TEX_H);

        // Barras de stats (se llenan de abajo hacia arriba)
        float hungerPct     = entity.getHungerPercentage() / 100f;
        float thirstPct     = entity.getThirstPercentage() / 100f;
        float enrichmentPct = entity.getEnrichmentPercentage() / 100f;
        float temperaturePct = 1.0f;  // estática por ahora
        float humidityPct    = 1.0f;  // estática por ahora
        float hygienePct     = entity.getHygienePercentage() / 100f;

        float[] pcts = { hungerPct, thirstPct, enrichmentPct, temperaturePct, humidityPct, hygienePct };

        for (int i = 0; i < 6; i++) {
            int filled = (int)(BAR_HEIGHT * pcts[i]);
            if (filled <= 0) continue;
            int destX = BAR_SLOT_X[i];
            int destY = BAR_Y_START + (BAR_HEIGHT - filled);
            graphics.blit(BARS_TEXTURE, destX, destY, BAR_U[i], BAR_HEIGHT - filled, BAR_COLOR_WIDTH, filled, BAR_TEX_W, BAR_TEX_H);
        }

        // ---------------------------------------------------------
        // ===== [H] RENDER DEL ICONO DE GÉNERO =====
        // ---------------------------------------------------------
        ResourceLocation genderIcon = entity.getIsMale() ? MALE_ICON : FEMALE_ICON;

        pose.pushPose();
        pose.translate(GENDER_ICON_X, GENDER_ICON_Y, 0);
        pose.scale(GENDER_ICON_SCALE, GENDER_ICON_SCALE, 1.0f);

        // Ahora usa el Width y Height originales para que no se deforme
        graphics.blit(genderIcon, 0, 0, 0, 0, GENDER_ICON_WIDTH, GENDER_ICON_HEIGHT, GENDER_ICON_WIDTH, GENDER_ICON_HEIGHT);
        pose.popPose();
        // ---------------------------------------------------------

        pose.popPose(); // <- Cierra la escala del fondo y de la GUI

        // ===== Render de la entidad =====
        int entityX = guiLeft + (int)(ENTITY_TEX_X * SCALE);
        int entityY = guiTop + (int)(ENTITY_TEX_Y * SCALE);
        int entitySize = (int)(ENTITY_SIZE * SCALE);

        // Ocultar nametag durante el render en la GUI
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

        // Restaurar nametag
        entity.setCustomName(savedName);
        entity.setCustomNameVisible(wasNameVisible);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    // ==============================
    //        LABELS / TEXT
    // ==============================
    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        PoseStack pose = graphics.pose();

        // ===== [E] Nametag + Título arriba de la GUI =====

        // 1. Nametag (con escala y offsets personalizados)
        String nametag = entity.getCustomName() != null ? entity.getCustomName().getString() : "N/A";
        int nametagW = this.font.width(nametag);

        pose.pushPose();
        // Calculamos el centro y le sumamos el Offset en X del Nametag
        float nametagCenterX = (SCALED_W / 2.0f) + NAMETAG_OFFSET_X;
        pose.translate(nametagCenterX, NAMETAG_OFFSET_Y, 0);
        pose.scale(NAMETAG_SCALE, NAMETAG_SCALE, 1.0f);
        graphics.drawString(this.font, nametag, -nametagW / 2, 0, NAMETAG_COLOR, true);
        pose.popPose();

        // 2. Título de la especie (con offsets personalizados)
        String title = "The Banded Palm Civet";
        int titleW = this.font.width(title);
        graphics.drawString(this.font, title, ((SCALED_W - titleW) / 2) + TITLE_OFFSET_X, TITLE_OFFSET_Y, TITLE_COLOR, false);

        // ===== [F] Labels verticales de las barras =====
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

        // ===== [G] Info central (Scientific name, Health, Age, Color, Status) =====
        int startX = (int)(INFO_START_TEX_X * SCALE);
        int startY = (int)(INFO_START_TEX_Y * SCALE);

        pose.pushPose();
        pose.scale(INFO_TEXT_SCALE, INFO_TEXT_SCALE, 1.0f);

        int sx = (int)(startX / INFO_TEXT_SCALE);
        int sy = (int)(startY / INFO_TEXT_SCALE);
        int lineH = (int)(INFO_LINE_SPACING / INFO_TEXT_SCALE);

        // Línea 1: Nombre científico
        String sci = "Hemigalus derbyanus";
        graphics.drawString(this.font, sci, sx, sy, INFO_TEXT_COLOR, true);

        // Línea 2: Health
        String hp = "Health: " + (int)entity.getHealth() + "/" + (int)entity.getMaxHealth();
        graphics.drawString(this.font, hp, sx, sy + lineH, INFO_TEXT_COLOR, true);

        // Línea 3: Age
        Component ageVal = Component.translatable("screen.diversityofcritters.is_baby." + entity.isBaby());
        Component ageKey = Component.translatable("screen.diversityofcritters.age");
        String age = ageKey.getString() + ": " + ageVal.getString();
        graphics.drawString(this.font, age, sx, sy + lineH * 2, INFO_TEXT_COLOR, true);

        // Línea 4: Morph
        String morph = "Morph: (Gray 0)";
        graphics.drawString(this.font, morph, sx, sy + lineH * 3, INFO_TEXT_COLOR, true);

        // Línea 5: Status
        String status = "Status: " + (entity.isTame() ? "Tamed" : "Wild");
        graphics.drawString(this.font, status, sx, sy + lineH * 4, INFO_TEXT_COLOR, true);

        pose.popPose();
    }
}
