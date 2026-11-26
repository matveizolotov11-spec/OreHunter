package ru.shadowhitbox;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class ShadowHitboxScreen extends Screen {
    private enum Tab { COMBAT, VISUALS, SETTINGS }
    private Tab tab = Tab.COMBAT;
    private String dragging = null;
    private final int panelWidth = 430;
    private final int panelHeight = 390;

    public ShadowHitboxScreen() {
        super(Component.literal("ShadowHitbox"));
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
        g.drawString(font, "HITBOX", left + 75, top + 13, 0xFF42C7FF, false);

        drawTab(g, left + 18, top + 52, "COMBAT", Tab.COMBAT);
        drawTab(g, left + 112, top + 52, "VISUALS", Tab.VISUALS);
        drawTab(g, left + 206, top + 52, "SETTINGS", Tab.SETTINGS);

        int x = left + 20;
        int y = top + 96;
        if (tab == Tab.COMBAT) {
            drawToggle(g, x, y, "Hitbox Expand", HitboxConfig.expandEnabled); y += 43;
            drawSlider(g, x, y, "Hitbox Width", HitboxConfig.width, 0.6f, 2.5f, "width"); y += 52;
            drawSlider(g, x, y, "Hitbox Height", HitboxConfig.height, 1.0f, 3.5f, "height");
            if (!minecraft.hasSingleplayerServer()) {
                g.drawString(font, "Размеры применяются только в одиночном мире.", x, bottom - 34, 0xFFFFC56B, false);
            }
        } else if (tab == Tab.VISUALS) {
            drawToggle(g, x, y, "Hitbox ESP", HitboxConfig.espEnabled); y += 43;
            drawToggle(g, x, y, "Show Self", HitboxConfig.showSelf); y += 43;
            drawSlider(g, x, y, "Red", HitboxConfig.red, 0f, 1f, "red"); y += 43;
            drawSlider(g, x, y, "Green", HitboxConfig.green, 0f, 1f, "green"); y += 43;
            drawSlider(g, x, y, "Blue", HitboxConfig.blue, 0f, 1f, "blue"); y += 43;
            drawSlider(g, x, y, "Opacity", HitboxConfig.alpha, 0.05f, 1f, "alpha");
        } else {
            g.drawString(font, "Settings are saved automatically.", x, y, 0xFFD5D9E4, false);
            g.drawString(font, "Config: config/shadowhitbox.json", x, y + 24, 0xFF8D96A8, false);
            g.fill(x, y + 58, x + 112, y + 82, 0xFF293344);
            g.drawCenteredString(font, "Save now", x + 56, y + 66, 0xFFFFFFFF);
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

    private void drawSlider(GuiGraphics g, int x, int y, String label, float value, float min, float max, String id) {
        g.drawString(font, label, x, y, 0xFFE6EAF2, false);
        String shown = String.format(java.util.Locale.ROOT, "%.2f", value);
        g.drawString(font, shown, x + 334, y, 0xFF54C9FF, false);
        int sx = x;
        int sy = y + 20;
        g.fill(sx, sy, sx + 370, sy + 5, 0xFF303847);
        int knob = sx + Math.round((value - min) / (max - min) * 370);
        g.fill(sx, sy, knob, sy + 5, 0xFF299BC4);
        g.fill(knob - 4, sy - 4, knob + 5, sy + 9, 0xFFEAF8FF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        int left = (width - panelWidth) / 2;
        int top = (height - panelHeight) / 2;
        int x = left + 20;
        int y = top + 96;

        if (inside(mouseX, mouseY, left + 18, top + 52, 84, 25)) tab = Tab.COMBAT;
        else if (inside(mouseX, mouseY, left + 112, top + 52, 84, 25)) tab = Tab.VISUALS;
        else if (inside(mouseX, mouseY, left + 206, top + 52, 84, 25)) tab = Tab.SETTINGS;
        else if (tab == Tab.COMBAT && inside(mouseX, mouseY, x + 310, y, 60, 24)) {
            HitboxConfig.expandEnabled = !HitboxConfig.expandEnabled; HitboxConfig.save();
        } else if (tab == Tab.COMBAT && inside(mouseX, mouseY, x, y + 16, 370, 18)) dragging = "width";
        else if (tab == Tab.COMBAT && inside(mouseX, mouseY, x, y + 68, 370, 18)) dragging = "height";
        else if (tab == Tab.VISUALS && inside(mouseX, mouseY, x + 310, y, 60, 24)) {
            HitboxConfig.espEnabled = !HitboxConfig.espEnabled; HitboxConfig.save();
        } else if (tab == Tab.VISUALS && inside(mouseX, mouseY, x + 310, y + 43, 60, 24)) {
            HitboxConfig.showSelf = !HitboxConfig.showSelf; HitboxConfig.save();
        } else if (tab == Tab.VISUALS) {
            if (inside(mouseX, mouseY, x, y + 102, 370, 18)) dragging = "red";
            else if (inside(mouseX, mouseY, x, y + 145, 370, 18)) dragging = "green";
            else if (inside(mouseX, mouseY, x, y + 188, 370, 18)) dragging = "blue";
            else if (inside(mouseX, mouseY, x, y + 231, 370, 18)) dragging = "alpha";
        } else if (tab == Tab.SETTINGS && inside(mouseX, mouseY, x, y + 58, 112, 24)) HitboxConfig.save();

        if (dragging != null) updateSlider(mouseX);
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging != null && button == 0) { updateSlider(mouseX); return true; }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging != null) { dragging = null; HitboxConfig.save(); return true; }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void updateSlider(double mouseX) {
        int x = (width - panelWidth) / 2 + 20;
        float t = HitboxConfig.clamp((float) (mouseX - x) / 370f, 0f, 1f);
        switch (dragging) {
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
