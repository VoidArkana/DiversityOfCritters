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

public class DOCStatScreen extends AbstractContainerScreen<DOCStatsMenu> {

    public static final ResourceLocation BOOK_LOCATION = new ResourceLocation(DiversityOfCritters.MODID, "textures/gui/stats_gui.png");
    public static final ResourceLocation BARS_LOCATION = new ResourceLocation(DiversityOfCritters.MODID, "textures/gui/bars_gui.png");

    private static final ResourceLocation SUN_ICON     = new ResourceLocation(DiversityOfCritters.MODID, "textures/gui/sun_icon.png");
    private static final ResourceLocation MOON_ICON    = new ResourceLocation(DiversityOfCritters.MODID, "textures/gui/moon_icon.png");
    private static final ResourceLocation RELEASE_ICON = new ResourceLocation(DiversityOfCritters.MODID, "textures/gui/release_icon.png");

    private final DiverseCritter entity;

    private ImageButton sunBtn;
    private ImageButton moonBtn;
    private ImageButton releaseBtn;

    public DOCStatScreen(DOCStatsMenu container, Inventory inventory, DiverseCritter chicken) {
        super(container, inventory, chicken.getDisplayName());
        this.entity = chicken;
        this.imageWidth = 128;
        this.imageHeight = 128;
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();

        int k = (this.width - this.imageWidth) / 2;
        int l = (this.height - this.imageHeight) / 2;

        int buttonsY = l + 107;

        this.sunBtn = new ImageButton(
                k + 13, buttonsY, 16, 16,
                0, 0, 16, SUN_ICON, 16, 16,
                btn -> sendSetDiurnal(true),
                Component.translatable("screen.diversityofcritters.diurnal")
        );
        this.addRenderableWidget(this.sunBtn);

        this.moonBtn = new ImageButton(
                k + 33, buttonsY, 16, 16,
                0, 0, 16, MOON_ICON, 16, 16,
                btn -> sendSetDiurnal(false),
                Component.translatable("screen.diversityofcritters.nocturnal")
        );
        this.addRenderableWidget(this.moonBtn);

        this.releaseBtn = new ImageButton(
                k + 53, buttonsY, 16, 16,
                0, 0, 16, RELEASE_ICON, 16, 16,
                btn -> sendRelease(),
                Component.translatable("screen.diversityofcritters.release")
        );
        this.addRenderableWidget(this.releaseBtn);

        setDiurnalButtonsEnabled(canEditDiurnal());
        setReleaseButtonEnabled(canRelease());
    }

    private void sendSetDiurnal(boolean value) {
        DOCNetworkHandler.CHANNEL.sendToServer(new SetDiurnalMsg(this.entity.getId(), value));
    }

    private void sendRelease() {
        if (canRelease()) {
            DOCNetworkHandler.CHANNEL.sendToServer(new ReleaseCritterMsg(this.entity.getId()));
        }
    }

    // ... (Mantén los métodos de mouseClicked, mouseReleased, mouseDragged, etc. iguales) ...
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = false;
        if (canEditDiurnal()) {
            if (this.sunBtn != null && this.sunBtn.mouseClicked(mouseX, mouseY, button)) handled = true;
            if (this.moonBtn != null && this.moonBtn.mouseClicked(mouseX, mouseY, button)) handled = true;
        }
        if (canRelease()) {
            if (this.releaseBtn != null && this.releaseBtn.mouseClicked(mouseX, mouseY, button)) handled = true;
        }
        return handled || true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean handled = false;
        if (canEditDiurnal()) {
            if (this.sunBtn != null && this.sunBtn.mouseReleased(mouseX, mouseY, button)) handled = true;
            if (this.moonBtn != null && this.moonBtn.mouseReleased(mouseX, mouseY, button)) handled = true;
        }
        if (canRelease()) {
            if (this.releaseBtn != null && this.releaseBtn.mouseReleased(mouseX, mouseY, button)) handled = true;
        }
        return handled || true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        boolean handled = false;
        if (canEditDiurnal()) {
            if (this.sunBtn != null && this.sunBtn.mouseDragged(mouseX, mouseY, button, dragX, dragY)) handled = true;
            if (this.moonBtn != null && this.moonBtn.mouseDragged(mouseX, mouseY, button, dragX, dragY)) handled = true;
        }
        if (canRelease()) {
            if (this.releaseBtn != null && this.releaseBtn.mouseDragged(mouseX, mouseY, button, dragX, dragY)) handled = true;
        }
        return handled || true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) { return true; }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        return true;
    }

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
        if (this.sunBtn != null) {
            this.sunBtn.visible = enabled;
            this.sunBtn.active  = enabled;
        }
        if (this.moonBtn != null) {
            this.moonBtn.visible = enabled;
            this.moonBtn.active  = enabled;
        }
    }

    private void setReleaseButtonEnabled(boolean enabled) {
        if (this.releaseBtn != null) {
            this.releaseBtn.visible = enabled;
            this.releaseBtn.active  = enabled;
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int k = (this.width - this.imageWidth) / 2;
        int l = (this.height - this.imageHeight) / 2;

        float hungerPercent = ((float) entity.getHungerPercentage() / 100);
        float thirstPercent = ((float) entity.getThirstPercentage() / 100);
        // NUEVO: Porcentaje de Enrichment
        float enrichmentPercent = ((float) entity.getEnrichmentPercentage() / 100);

        int hungerWidth = (int) (102 * hungerPercent);
        int thirstWidth = (int) (102 * thirstPercent);
        // NUEVO: Ancho de barra
        int enrichmentWidth = (int) (102 * enrichmentPercent);

        graphics.blit(BOOK_LOCATION, k, l, 0, 0, 128, 128, 128, 128);

        // Barra Hambre (Y = 61)
        graphics.blit(BARS_LOCATION, k + 13, l + 61, 13, 61, hungerWidth, 13, 128, 128);

        // Barra Sed (Y = 77)
        graphics.blit(BARS_LOCATION, k + 13, l + 77, 13, 77, thirstWidth, 13, 128, 128);

        // NUEVO: Barra Enrichment (Y = 93)
        // 93 sale de sumar 16 píxeles a la posición de Sed (77 + 16 = 93)
        // El 4to argumento (13) es la X en la textura
        // El 5to argumento (93) es la Y en la textura bars_gui.png donde empieza la barra verde
        graphics.blit(BARS_LOCATION, k + 13, l + 93, 13, 93, enrichmentWidth, 13, 128, 128);

        InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, k + 30, l + 40, 50, -mouseX + k + 35, -mouseY + l + 30, entity);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        int xOffset = 35;
        Component none = Component.translatable("screen.diversityofcritters.none");

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.scale(0.5f, 0.5f, 0.5f);
        pose.translate(35, 40, 0);

        // ... (Labels existentes: Nombre, Salud, Especie, Sexo, Edad, Modo) ...

        Component name = Component.translatable("screen.diversityofcritters.name");
        String creatureName = name.getString() + ": " + (entity.getCustomName() == null ? none.getString() : entity.getCustomName().getString());
        graphics.drawString(this.font, creatureName , this.imageWidth / 2 - this.font.width(creatureName) / 2 + 38 + xOffset, 19 - 11 * 2, 0X667819, false);

        Component health = Component.translatable("screen.diversityofcritters.health");
        String currentHealth = health.getString() + ": " + entity.getHealth() + "/" + entity.getMaxHealth();
        graphics.drawString(this.font, currentHealth , this.imageWidth / 2 - this.font.width(currentHealth) / 2 + 38 + xOffset, 19 - 11, 0X667819, false);

        Component keySpecies = Component.translatable("screen.diversityofcritters.species");
        Component species = Component.translatable(entity.getType().getDescriptionId());
        String creaturespecies = keySpecies.getString() + ": " + species.getString();
        graphics.drawString(this.font, creaturespecies , this.imageWidth / 2 - this.font.width(creaturespecies) / 2 + 38 + xOffset, 19, 0X667819, false);

        Component sex = Component.translatable("screen.diversityofcritters.is_male." + entity.getIsMale());
        Component sexKey = Component.translatable("screen.diversityofcritters.sex");
        String animalSex = sexKey.getString() + ": " + sex.getString();
        graphics.drawString(this.font, animalSex, this.imageWidth / 2 - this.font.width(animalSex) / 2 + 38 + xOffset, 19 + (11), 0X667819, false);

        Component age = Component.translatable("screen.diversityofcritters.is_baby." + entity.isBaby());
        Component ageKey = Component.translatable("screen.diversityofcritters.age");
        String animalAge = ageKey.getString() + ": " + age.getString();
        graphics.drawString(this.font, animalAge, this.imageWidth / 2 - this.font.width(animalAge) / 2 + 38 + xOffset, 19 + (11 * 2), 0X667819, false);

        String mode = entity.isDiurnal() ? "Mode: Diurnal" : "Mode: Nocturnal";
        graphics.drawString(this.font, mode, this.imageWidth / 2 - this.font.width(mode) / 2 + 38 + xOffset, 19 + (11 * 3), 0X667819, false);

        // Hunger Label
        Component hungerKey = Component.translatable("screen.diversityofcritters.hunger");
        String hunger = hungerKey.getString();
        graphics.drawString(this.font, hunger, this.imageWidth / 2 - this.font.width(hunger) / 2 + xOffset - 5, 90, 0XFFFFFF, true);

        // Thirst Label (Pos 122)
        Component thirstKey = Component.translatable("screen.diversityofcritters.thirst");
        String thirst = thirstKey.getString();
        graphics.drawString(this.font, thirst, this.imageWidth / 2 - this.font.width(thirst) / 2 + xOffset - 5, 122, 0XFFFFFF, true);

        // NUEVO: Enrichment Label (Pos 154)
        // 154 sale de sumar 32 a la posición de Sed (122 + 32 = 154) debido a la escala de texto 0.5x
        Component enrichmentKey = Component.translatable("screen.diversityofcritters.enrichment");
        // Asegúrate de añadir "screen.diversityofcritters.enrichment": "Enrichment" en tu archivo en_us.json
        String enrichment = enrichmentKey.getString();
        graphics.drawString(this.font, enrichment, this.imageWidth / 2 - this.font.width(enrichment) / 2 + xOffset - 5, 154, 0XFFFFFF, true);

        pose.popPose();
    }
}