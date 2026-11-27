package ru.bowrapidfire;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public final class BowRapidFireScreen extends Screen {
    public BowRapidFireScreen() {
        super(Component.literal("BowRapidFire"));
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;
        addRenderableWidget(Button.builder(
                Component.literal("Стрельба: " + (BowRapidFireClient.isEnabled() ? "ВКЛ" : "ВЫКЛ")),
                button -> {
                    BowRapidFireClient.setEnabled(!BowRapidFireClient.isEnabled());
                    button.setMessage(Component.literal("Стрельба: "
                            + (BowRapidFireClient.isEnabled() ? "ВКЛ" : "ВЫКЛ")));
                }).bounds(cx - 90, cy - 10, 180, 24).build());
        addRenderableWidget(Button.builder(Component.literal("Закрыть"),
                button -> onClose()).bounds(cx - 90, cy + 24, 180, 20).build());
    }

    @Override
    public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackground(graphics, mouseX, mouseY, delta);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 45, 0xFFFFFF);
        graphics.drawCenteredString(font, "Удерживай Mouse4 — лук стреляет с зарядом 1 тик",
                width / 2, height / 2 - 27, 0xAAAAAA);
        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
