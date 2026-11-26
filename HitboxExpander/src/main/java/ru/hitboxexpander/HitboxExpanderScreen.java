package ru.hitboxexpander;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class HitboxExpanderScreen extends Screen {
    private static final int PANEL_W = 390;
    private static final int PANEL_H = 370;
    private String dragging;

    public HitboxExpanderScreen() {
        super(Component.literal("Hitbox Expander"));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        renderBackground(g, mouseX, mouseY, delta);
        int x = (width - PANEL_W) / 2;
        int y = (height - PANEL_H) / 2;
        g.fill(0, 0, width, height, 0xA8000000);
        g.fill(x, y, x + PANEL_W, y + PANEL_H, 0xFF11141D);
        g.fill(x, y, x + PANEL_W, y + 42, 0xFF1B2230);
        g.fill(x, y, x + 3, y + PANEL_H, 0xFF42BCEB);
        g.drawString(font, "HITBOX", x + 18, y + 15, 0xFFFFFFFF, false);
        g.drawString(font, "EXPANDER", x + 74, y + 15, 0xFF42BCEB, false);

        int row = y + 58;
        toggle(g, x + 18, row, "Hitbox Expander", HitboxConfig.enabled);
        toggle(g, x + 205, row, "Players", HitboxConfig.applyToPlayers);
        toggle(g, x + 18, row + 34, "Mobs", HitboxConfig.applyToMobs);

        slider(g, x + 18, row + 88, "Multiplier", HitboxConfig.multiplier);
        slider(g, x + 18, row + 136, "Width", HitboxConfig.widthMultiplier);
        slider(g, x + 18, row + 184, "Depth", HitboxConfig.depthMultiplier);
        slider(g, x + 18, row + 232, "Height", HitboxConfig.heightMultiplier);

        g.fill(x + 18, y + PANEL_H - 54, x + 104, y + PANEL_H - 27, 0xFF303B50);
        g.drawCenteredString(font, "RESET", x + 61, y + PANEL_H - 45, 0xFFFFFFFF);
        g.drawString(font, "H: toggle  •  Right Shift / ESC: close", x + 118, y + PANEL_H - 44, 0xFF9BA8BC, false);
        g.drawString(font, "Client-side only: server hitboxes are unchanged", x + 18, y + PANEL_H - 15, 0xFF78869B, false);
        super.render(g, mouseX, mouseY, delta);
    }

    private void toggle(GuiGraphics g, int x, int y, String label, boolean on) {
        g.drawString(font, label, x, y + 7, 0xFFE6EAF2, false);
        int bx = x + 115;
        g.fill(bx, y, bx + 54, y + 23, on ? 0xFF176D82 : 0xFF343B49);
        g.drawCenteredString(font, on ? "ON" : "OFF", bx + 27, y + 7, 0xFFFFFFFF);
    }

    private void slider(GuiGraphics g, int x, int y, String label, float value) {
        g.drawString(font, label, x, y, 0xFFE6EAF2, false);
        g.drawString(font, String.format(java.util.Locale.ROOT, "%.2f", value), x + 318, y, 0xFF50C9F4, false);
        int sy = y + 20;
        g.fill(x, sy, x + 336, sy + 5, 0xFF303949);
        int k = x + Math.round((value - 1.0f) / 4.0f * 336);
        g.fill(x, sy, k, sy + 5, 0xFF269DC5);
        g.fill(k - 4, sy - 4, k + 5, sy + 9, 0xFFE8F8FF);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        int x = (width - PANEL_W) / 2;
        int y = (height - PANEL_H) / 2 + 58;
        double mx = click.x(), my = click.y();

        if (hit(mx, my, x + 133, y, 54, 23)) HitboxConfig.enabled = !HitboxConfig.enabled;
        else if (hit(mx, my, x + 320, y, 54, 23)) HitboxConfig.applyToPlayers = !HitboxConfig.applyToPlayers;
        else if (hit(mx, my, x + 133, y + 34, 54, 23)) HitboxConfig.applyToMobs = !HitboxConfig.applyToMobs;
        else if (hit(mx, my, x + 18, y + 108, 336, 24)) dragging = "multiplier";
        else if (hit(mx, my, x + 18, y + 156, 336, 24)) dragging = "width";
        else if (hit(mx, my, x + 18, y + 204, 336, 24)) dragging = "depth";
        else if (hit(mx, my, x + 18, y + 252, 336, 24)) dragging = "height";
        else if (hit(mx, my, x + 18, y + PANEL_H - 54, 86, 27)) {
            HitboxConfig.reset();
            return true;
        } else return super.mouseClicked(click, doubled);

        if (dragging != null) updateSlider(click.x(), x);
        HitboxConfig.save();
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double dragX, double dragY) {
        if (dragging != null && click.button() == 0) {
            updateSlider(click.x(), (width - PANEL_W) / 2);
            return true;
        }
        return super.mouseDragged(click, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        if (dragging != null) {
            dragging = null;
            HitboxConfig.save();
            return true;
        }
        return super.mouseReleased(click);
    }

    private void updateSlider(double mouseX, int panelX) {
        float t = HitboxConfig.clamp((float) (mouseX - (panelX + 18)) / 336.0f, 0f, 1f);
        float v = 1.0f + t * 4.0f;
        switch (dragging) {
            case "multiplier" -> HitboxConfig.multiplier = v;
            case "width" -> HitboxConfig.widthMultiplier = v;
            case "depth" -> HitboxConfig.depthMultiplier = v;
            case "height" -> HitboxConfig.heightMultiplier = v;
        }
    }

    private boolean hit(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    @Override
    public void onClose() {
        HitboxConfig.save();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
