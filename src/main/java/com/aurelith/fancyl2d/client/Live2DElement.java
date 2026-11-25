package com.aurelith.fancyl2d.client;

import com.aurelith.fancyl2d.FancyL2D;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class Live2DElement extends AbstractElement {
    private static final int MAX_SURFACE = 2048;

    String model = Live2DSettings.DEFAULT.model();
    double scale = Live2DSettings.DEFAULT.scale();
    double follow = Live2DSettings.DEFAULT.follow();
    boolean idle = Live2DSettings.DEFAULT.idle();
    double sway = Live2DSettings.DEFAULT.sway();
    int fps = Live2DSettings.DEFAULT.fps();
    private Live2DSessions.Key attachedKey;
    private Live2DSession attached;
    private Live2DSettings failedSettings;
    private String error;

    Live2DElement(Live2DElementBuilder builder) {
        super(builder);
        baseWidth = 160;
        baseHeight = 200;
    }

    Live2DSettings settings() { return new Live2DSettings(model, scale, follow, idle, sway, fps); }

    private Live2DSessions.Key sessionKey() {
        var layout = getParentLayout();
        String namespace = layout == null ? "no-layout" : layout.layoutFile == null ? layout.runtimeLayoutIdentifier
            : layout.layoutFile.toPath().toAbsolutePath().normalize().toString();
        return new Live2DSessions.Key(namespace, getInstanceIdentifier());
    }

    @Override public boolean takesMousePressAt(double x, double y) { return false; }

    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        if (!shouldRender()) { detach(); return; }
        if (isEditor()) {
            detach();
            Live2DEditorPreview.render(this, graphics, mouseX, mouseY);
            return;
        }
        int width = getAbsoluteWidth(), height = getAbsoluteHeight();
        if (width <= 0 || height <= 0) { detach(); return; }
        var key = sessionKey();
        var settings = settings();
        if (error != null && settings.equals(failedSettings)) { placeholder(graphics, error); return; }
        if (model.isBlank()) {
            detach();
            Live2DSessions.discard(key);
            placeholder(graphics, "fancyl2d.no_model");
            return;
        }
        var unavailable = BrowserAvailability.unavailableMessage();
        if (unavailable != null) {
            detach();
            Live2DSessions.discard(key);
            placeholder(graphics, unavailable);
            return;
        }

        double pixelScale = surfaceScale(width, height, MAX_SURFACE);
        int w = Math.max(1, (int) Math.round(width * pixelScale));
        int h = Math.max(1, (int) Math.round(height * pixelScale));
        try {
            if (attachedKey != null && !key.equals(attachedKey)) detach();
            if (!Live2DSessions.canReuse(key, settings)) {
                String coreStatus = CubismCoreDownload.unavailableMessage();
                if (coreStatus != null) {
                    detach();
                    Live2DSessions.discard(key);
                    placeholder(graphics, coreStatus);
                    return;
                }
            }
            attached = Live2DSessions.acquire(key, this, settings, w, h);
            attachedKey = key;
            if (attached.failed()) {
                FancyL2D.LOGGER.warn("Live2D page failed or timed out for {}", model);
                fail(key, settings, "fancyl2d.load_error");
            } else if (attached.ready()) {
                // A resize or repaint may defer the texture, but the model has already loaded.
                attached.render(graphics, getAbsoluteX(), getAbsoluteY(), width, height, opacity, w, h, mouseX, mouseY);
                return;
            }
        } catch (IOException | RuntimeException | LinkageError failure) {
            FancyL2D.LOGGER.warn("Could not render Live2D model {}", model, failure);
            fail(key, settings, "fancyl2d.load_error");
        }
        placeholder(graphics, error != null && settings.equals(failedSettings) ? error : "fancyl2d.loading");
    }

    static double surfaceScale(int width, int height, int maxSurface) {
        int guiScale = Minecraft.getInstance().getWindow().getGuiScale();
        return Math.min(guiScale, maxSurface / (double) Math.max(width, height));
    }

    void placeholder(GuiGraphicsExtractor graphics, String key) {
        int x = getAbsoluteX(), y = getAbsoluteY(), w = getAbsoluteWidth(), h = getAbsoluteHeight();
        if (w <= 0 || h <= 0) return;
        graphics.enableScissor(x, y, x + w, y + h);
        try {
            graphics.fill(x, y, x + w, y + h, 0x80302b47);
            graphics.centeredText(Minecraft.getInstance().font, Component.translatable(key), x + w / 2, y + (h - 8) / 2, 0xffe9e0ff);
        } finally {
            graphics.disableScissor();
        }
    }

    private void fail(Live2DSessions.Key key, Live2DSettings settings, String message) {
        Live2DSessions.discard(key);
        detach();
        failedSettings = settings;
        error = message;
    }

    private void detach() {
        Live2DSessions.detach(attachedKey, attached, this);
        attached = null;
        attachedKey = null;
    }

    @Override public void onCloseScreen(Screen previous, Screen next) { detach(); }
    @Override public void onCloseScreen() { detach(); }
    @Override public void onDestroyElement() { detach(); }
    @Override public void onBecomeInvisible() { detach(); }
    @Override public void onRenderInternalSkipped() { detach(); }
    @Override public void onOpenScreen() { error = null; }
}
