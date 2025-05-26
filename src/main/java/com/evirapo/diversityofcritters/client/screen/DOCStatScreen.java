package com.evirapo.diversityofcritters.client.screen;

import com.evirapo.diversityofcritters.DiversityOfCritters;
import com.evirapo.diversityofcritters.client.menu.DOCStatsMenu;
import com.evirapo.diversityofcritters.common.entity.custom.base.DiverseCritter;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class DOCStatScreen extends AbstractContainerScreen<DOCStatsMenu> {

    public static final ResourceLocation BOOK_LOCATION = new ResourceLocation(DiversityOfCritters.MODID, "textures/gui/stats_gui.png");
    public static final ResourceLocation BARS_LOCATION = new ResourceLocation(DiversityOfCritters.MODID, "textures/gui/bars_gui.png");

    private final DiverseCritter chicken;

    protected static final int X = 390;
    protected static final int Y = 245;

    public DOCStatScreen(DOCStatsMenu container, Inventory inventory, DiverseCritter chicken) {
        super(container, inventory, chicken.getDisplayName());
        this.chicken = chicken;
        this.imageWidth = 128;
        this.imageHeight = 128;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int pMouseX, int pMouseY, float partialTicks) {

        this.renderBackground(guiGraphics);

        PoseStack pose = guiGraphics.pose();

        int k = (this.width - this.imageWidth)/2;
        int l = (this.height - this.imageHeight)/2;

        pose.pushPose();
        pose.scale(1.5f, 1.5f, 1.5f);
        pose.translate(-k/2.5, -l/1.5, 0);
            super.render(guiGraphics, pMouseX, pMouseY, partialTicks);
            this.renderTooltip(guiGraphics, pMouseX, pMouseY);
        pose.popPose();

    }

    @Override
    protected void init() {
        super.init();
        this.renderables.clear();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float pPartialTick, int mouseX, int mouseY) {

        int k = (this.width - this.imageWidth)/2;
        int l = (this.height - this.imageHeight)/2;

        float hungerPercent = ((float) chicken.getHungerPercentage() /100);
        float thirstPercent = ((float) chicken.getThirstPercentage() /100);
        int hungerWidth = (int) (102 * hungerPercent);
        int thirstWidth = (int) (102 * thirstPercent);

        this.renderBackground(graphics);
            graphics.blit(BOOK_LOCATION, k, l, 0, 0, 128, 128, 128, 128);

            graphics.blit(BARS_LOCATION, k+13, l+61, 13, 61, hungerWidth, 13, 128, 128);
            graphics.blit(BARS_LOCATION, k+13, l+77, 13, 77, thirstWidth, 13, 128, 128);

        InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, k + 30, l + 40, 50, -mouseX+k+35, -mouseY+l+30, chicken);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        int xOffset = 35;
        int yOffset = 40;
        Component none = Component.translatable("screen.diversityofcritters.none");

        PoseStack pose = graphics.pose();
        int k = (this.width - this.imageWidth)/2;
        int l = (this.height - this.imageHeight)/2;

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

            Component hungerKey = Component.translatable("screen.diversityofcritters.hunger");
            String hunger =  hungerKey.getString();
            graphics.drawString(this.font, hunger, this.imageWidth / 2 - this.font.width(hunger) / 2 + xOffset - 5, 90, 0XFFFFFF, true);

            Component thirstKey = Component.translatable("screen.diversityofcritters.thirst");
            String thirst =  thirstKey.getString();
            graphics.drawString(this.font, thirst, this.imageWidth / 2 - this.font.width(thirst) / 2 + xOffset - 5, 122, 0XFFFFFF, true);

        pose.popPose();
    }

}
