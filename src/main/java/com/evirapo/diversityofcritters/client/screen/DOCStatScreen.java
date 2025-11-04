package com.evirapo.diversityofcritters.client.screen;

import com.evirapo.diversityofcritters.DiversityOfCritters;
import com.evirapo.diversityofcritters.client.menu.DOCStatsMenu;
import com.evirapo.diversityofcritters.common.entity.custom.base.DiverseCritter;
import com.evirapo.diversityofcritters.network.DOCNetworkHandler;
import com.evirapo.diversityofcritters.network.SetDiurnalMsg;
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

    // Íconos (16x16)
    private static final ResourceLocation SUN_ICON  = new ResourceLocation(DiversityOfCritters.MODID, "textures/gui/sun_icon.png");
    private static final ResourceLocation MOON_ICON = new ResourceLocation(DiversityOfCritters.MODID, "textures/gui/moon_icon.png");

    private final DiverseCritter chicken;

    public DOCStatScreen(DOCStatsMenu container, Inventory inventory, DiverseCritter chicken) {
        super(container, inventory, chicken.getDisplayName());
        this.chicken = chicken;
        this.imageWidth = 128;
        this.imageHeight = 128;
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets(); // limpia renderables y children de forma segura

        // Centro del background
        int k = (this.width - this.imageWidth)/2;
        int l = (this.height - this.imageHeight)/2;

        // Barras en: y = l+61 (hunger) y l+77 (thirst)
        // Colocamos los botones DEBAJO de las barras
        int buttonsY = l + 96;

        // Sol: diurno = true (duerme de noche)
        ImageButton sunBtn = new ImageButton(
                k + 13, buttonsY, 16, 16,
                0, 0, 16, SUN_ICON, 16, 16,
                btn -> {
                    System.out.println("[GUI][CLIENT] Click SUN → set diurnal=true for entityId=" + chicken.getId());
                    sendSetDiurnal(true);
                },
                Component.translatable("screen.diversityofcritters.diurnal")
        );
        this.addRenderableWidget(sunBtn);

        // Luna: diurno = false (duerme de día)
        ImageButton moonBtn = new ImageButton(
                k + 33, buttonsY, 16, 16,
                0, 0, 16, MOON_ICON, 16, 16,
                btn -> {
                    System.out.println("[GUI][CLIENT] Click MOON → set diurnal=false for entityId=" + chicken.getId());
                    sendSetDiurnal(false);
                },
                Component.translatable("screen.diversityofcritters.nocturnal")
        );
        this.addRenderableWidget(moonBtn);
    }

    private void sendSetDiurnal(boolean value) {
        DOCNetworkHandler.CHANNEL.sendToServer(new SetDiurnalMsg(this.chicken.getId(), value));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        // NO ESCALAR aquí: permite que los botones clickeables funcionen bien
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int k = (this.width - this.imageWidth)/2;
        int l = (this.height - this.imageHeight)/2;

        float hungerPercent = ((float) chicken.getHungerPercentage() /100);
        float thirstPercent = ((float) chicken.getThirstPercentage() /100);
        int hungerWidth = (int) (102 * hungerPercent);
        int thirstWidth = (int) (102 * thirstPercent);

        graphics.blit(BOOK_LOCATION, k, l, 0, 0, 128, 128, 128, 128);

        graphics.blit(BARS_LOCATION, k+13, l+61, 13, 61, hungerWidth, 13, 128, 128);
        graphics.blit(BARS_LOCATION, k+13, l+77, 13, 77, thirstWidth, 13, 128, 128);

        // Render del mob (no escalamos los widgets, sólo el entity preview)
        InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, k + 30, l + 40, 50, -mouseX+k+35, -mouseY+l+30, chicken);
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

        Component name = Component.translatable("screen.diversityofcritters.name");
        String creatureName = name.getString() + ": " + (chicken.getCustomName() == null ? none.getString() : chicken.getCustomName().getString());
        graphics.drawString(this.font, creatureName , this.imageWidth / 2 - this.font.width(creatureName) / 2 + 38 + xOffset, 19-11*2, 0X667819, false);

        Component health = Component.translatable("screen.diversityofcritters.health");
        String currentHealth = health.getString() + ": " + chicken.getHealth() + "/" + chicken.getMaxHealth();
        graphics.drawString(this.font, currentHealth , this.imageWidth / 2 - this.font.width(currentHealth) / 2 + 38 + xOffset, 19-11, 0X667819, false);

        Component keySpecies = Component.translatable("screen.diversityofcritters.species");
        Component species = Component.translatable(chicken.getType().getDescriptionId());
        String creaturespecies = keySpecies.getString() + ": " + species.getString();
        graphics.drawString(this.font, creaturespecies , this.imageWidth / 2 - this.font.width(creaturespecies) / 2 + 38 + xOffset, 19, 0X667819, false);

        Component sex = Component.translatable("screen.diversityofcritters.is_male."+chicken.getIsMale());
        Component sexKey = Component.translatable("screen.diversityofcritters.sex");
        String animalSex =  sexKey.getString() + ": " + sex.getString();
        graphics.drawString(this.font, animalSex, this.imageWidth / 2 - this.font.width(animalSex) / 2 + 38 + xOffset, 19+(11), 0X667819, false);

        Component age = Component.translatable("screen.diversityofcritters.is_baby."+chicken.isBaby());
        Component ageKey = Component.translatable("screen.diversityofcritters.age");
        String animalAge =  ageKey.getString() + ": " + age.getString();
        graphics.drawString(this.font, animalAge, this.imageWidth / 2 - this.font.width(animalAge) / 2 + 38 + xOffset, 19+(11*2), 0X667819, false);

        String mode = chicken.isDiurnal()
                ? "Mode: Diurnal"
                : "Mode: Nocturnal";
        graphics.drawString(this.font, mode, this.imageWidth / 2 - this.font.width(mode) / 2 + 38 + xOffset, 19+(11*3), 0X667819, false);

        Component hungerKey = Component.translatable("screen.diversityofcritters.hunger");
        String hunger =  hungerKey.getString();
        graphics.drawString(this.font, hunger, this.imageWidth / 2 - this.font.width(hunger) / 2 + xOffset - 5, 90, 0XFFFFFF, true);

        Component thirstKey = Component.translatable("screen.diversityofcritters.thirst");
        String thirst =  thirstKey.getString();
        graphics.drawString(this.font, thirst, this.imageWidth / 2 - this.font.width(thirst) / 2 + xOffset - 5, 122, 0XFFFFFF, true);

        pose.popPose();
    }
}
