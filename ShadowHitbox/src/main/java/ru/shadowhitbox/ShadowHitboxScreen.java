package ru.shadowhitbox;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class ShadowHitboxScreen extends Screen {
    private enum Tab { COMBAT, VISUALS, SETTINGS }
    private Tab tab = Tab.COMBAT;
    private String dragging = null;
    private final int panelWidth = 430;
    private final int panelHeight = 430;

    public ShadowHitboxScreen() {
        super(Component.literal("ShadowHitbox PvP"));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        renderBackground(g, mouseX, mouseY, delta);
        int left = (width - panelWidth) / 2;
        int top = (height - panelHeight) / 2;
        int right = left + panelWidth;
        int bottom = top + panelHeight;

        g.fill(0, 0, width, height, 0x99000000);
        g.fill(left, top, right, bottom, 0xFF11131A);
        g.fill(left, top, right, top + 38, 0xFF191D28);
        g.fill(left, top, left + 3, bottom, 0xFF39BDF2);
        g.drawString(font, "SHADOW", left + 18, top + 13, 0xFFFFFFFF, false);
        g.drawString(font, "PVP", left + 82, top + 13, 0xFF42C7FF, false);

        drawTab(g, left + 18, top + 52, "COMBAT", Tab.COMBAT);
        drawTab(g, left + 112, top + 52, "VISUALS", Tab.VISUALS);
        drawTab(g, left + 206, top + 52, "SETTINGS", Tab.SETTINGS);

        int x = left + 20;
        int y = top + 96;
        if (tab == Tab.COMBAT) {
            drawToggle(g, x, y, "Aim Assist", HitboxConfig.aimAssistEnabled);
            drawToggle(g, x, y + 38, "Trigger Bot", HitboxConfig.triggerBotEnabled);
            g.drawString(font, "Trigger Bot mode: critical hits only", x, y + 81, 0xFF75D7A0, false);
            drawSlider(g, x, y + 118, "Combat Range", HitboxConfig.combatRange, 2.5f, 6.0f);
            drawSlider(g, x, y + 164, "Aim FOV", HitboxConfig.aimFov, 5.0f, 180.0f);
            drawSlider(g, x, y + 210, "Aim Smoothness", HitboxConfig.aimSmoothness, 1.0f, 20.0f);
            g.drawString(font, "Настройки применяются в мультиплеере.", x, bottom - 34, 0xFF75D7A0, false);
            g.drawString(font, "Клиент не меняет серверную регистрацию попаданий.", x, bottom - 22, 0xFF9AA4B5, false);
        } else if (tab == Tab.VISUALS) {
            drawToggle(g, x, y, "Hitbox ESP", HitboxConfig.espEnabled);
            drawToggle(g, x, y + 38, "Expand ESP frame", HitboxConfig.expandEnabled);
            drawToggle(g, x, y + 76, "Show Self", HitboxConfig.showSelf);
            drawSlider(g, x, y + 116, "Frame Width", HitboxConfig.width, 0.6f, 2.5f);
            drawSlider(g, x, y + 158, "Frame Height", HitboxConfig.height, 1.0f, 3.5f);
            drawSlider(g, x, y + 200, "Red", HitboxConfig.red, 0f, 1f);
            drawSlider(g, x, y + 242, "Green", HitboxConfig.green, 0f, 1f);
            drawSlider(g, x, y + 284, "Blue", HitboxConfig.blue, 0f, 1f);
            drawSlider(g, x, y + 326, "Opacity", HitboxConfig.alpha, 0.05f, 1f);
        } else {
            g.drawString(font, "Settings are saved automatically.", x, y, 0xFFD5D9E4, false);
            g.drawString(font, "Config: config/shadowhitbox.json", x, y + 24, 0xFF8D96A8, false);
            g.fill(x, y + 58, x + 112, y + 82, 0xFF293344);
            g.drawCenteredString(font, "Save now", x + 56, y + 66, 0xFFFFFFFF);
            g.drawString(font, "Target profile: AstrumMC", x, y + 106, 0xFF54C9FF, false);
            g.drawString(font, "Right Shift opens this menu.", x, y + 130, 0xFF9AA4B5, false);
        }

        g.drawString(font, "Right Shift  •  ESC to close", left + 18, bottom - 18, 0xFF778195, false);
        super.render(g, mouseX, mouseY, delta);
    }

    private void drawTab(GuiGraphics g, int x, int y, String label, Tab value) {
        int color = tab == value ? 0xFF263B50 : 0xFF1B202B;
        g.fill(x, y, x + 84, y + 25, color);
        g.drawCenteredString(font, label, x + 42, y + 8, tab == value ? 0xFF54C9FF : 0xFFB5BDCC);
    }

    private void drawToggle(GuiGraphics g, int x, int y, String label, boolean enabled) {
        g.drawString(font, label, x, y + 5, 0xFFE6EAF2, false);
        g.fill(x + 310, y, x + 370, y + 24, enabled ? 0xFF176C83 : 0xFF343B49);
        g.drawCenteredString(font, enabled ? "ON" : "OFF", x + 340, y + 8, 0xFFFFFFFF);
    }

    private void drawSlider(GuiGraphics g, int x, int y, String label, float value, float min, float max) {
        g.drawString(font, label, x, y, 0xFFE6EAF2, false);
        g.drawString(font, String.format(java.util.Locale.ROOT, "%.1f", value),
                x + 334, y, 0xFF54C9FF, false);
        int sy = y + 20;
        g.fill(x, sy, x + 370, sy + 5, 0xFF303847);
        int knob = x + Math.round((value - min) / (max - min) * 370);
        g.fill(x, sy, knob, sy + 5, 0xFF299BC4);
        g.fill(knob - 4, sy - 4, knob + 5, sy + 9, 0xFFEAF8FF);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        double mx = click.x();
        double my = click.y();
        if (click.button() != 0) return super.mouseClicked(click, doubled);

        int left = (width - panelWidth) / 2;
        int top = (height - panelHeight) / 2;
        int x = left + 20;
        int y = top + 96;

        if (inside(mx, my, left + 18, top + 52, 84, 25)) tab = Tab.COMBAT;
        else if (inside(mx, my, left + 112, top + 52, 84, 25)) tab = Tab.VISUALS;
        else if (inside(mx, my, left + 206, top + 52, 84, 25)) tab = Tab.SETTINGS;
        else if (tab == Tab.COMBAT) {
            if (inside(mx, my, x + 310, y, 60, 24)) HitboxConfig.aimAssistEnabled = !HitboxConfig.aimAssistEnabled;
            else if (inside(mx, my, x + 310, y + 38, 60, 24)) HitboxConfig.triggerBotEnabled = !HitboxConfig.triggerBotEnabled;
            else if (inside(mx, my, x, y + 114, 370, 22)) dragging = "range";
            else if (inside(mx, my, x, y + 160, 370, 22)) dragging = "fov";
            else if (inside(mx, my, x, y + 206, 370, 22)) dragging = "smooth";
        } else if (tab == Tab.VISUALS) {
            if (inside(mx, my, x + 310, y, 60, 24)) HitboxConfig.espEnabled = !HitboxConfig.espEnabled;
            else if (inside(mx, my, x + 310, y + 38, 60, 24)) HitboxConfig.expandEnabled = !HitboxConfig.expandEnabled;
            else if (inside(mx, my, x + 310, y + 76, 60, 24)) HitboxConfig.showSelf = !HitboxConfig.showSelf;
            else if (inside(mx, my, x, y + 112, 370, 22)) dragging = "width";
            else if (inside(mx, my, x, y + 154, 370, 22)) dragging = "height";
            else if (inside(mx, my, x, y + 196, 370, 22)) dragging = "red";
            else if (inside(mx, my, x, y + 238, 370, 22)) dragging = "green";
            else if (inside(mx, my, x, y + 280, 370, 22)) dragging = "blue";
            else if (inside(mx, my, x, y + 322, 370, 22)) dragging = "alpha";
        } else if (tab == Tab.SETTINGS && inside(mx, my, x, y + 58, 112, 24)) {
            HitboxConfig.save();
        }

        if (dragging != null) updateSlider(mx);
        HitboxConfig.save();
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double dragX, double dragY) {
        if (dragging != null && click.button() == 0) {
            updateSlider(click.x());
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

    private void updateSlider(double mouseX) {
        int x = (width - panelWidth) / 2 + 20;
        float t = HitboxConfig.clamp((float) (mouseX - x) / 370f, 0f, 1f);
        switch (dragging) {
            case "range" -> HitboxConfig.combatRange = 2.5f + t * 3.5f;
            case "fov" -> HitboxConfig.aimFov = 5.0f + t * 175.0f;
            case "smooth" -> HitboxConfig.aimSmoothness = 1.0f + t * 19.0f;
            case "width" -> HitboxConfig.width = 0.6f + t * 1.9f;
            case "height" -> HitboxConfig.height = 1.0f + t * 2.5f;
            case "red" -> HitboxConfig.red = t;
            case "green" -> HitboxConfig.green = t;
            case "blue" -> HitboxConfig.blue = t;
            case "alpha" -> HitboxConfig.alpha = 0.05f + t * 0.95f;
        }
    }

    private boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    @Override
    public void onClose() {
        HitboxConfig.save();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
