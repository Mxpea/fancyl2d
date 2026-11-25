package com.aurelith.fancyl2d.client;

import com.aurelith.fancyl2d.FancyL2D;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** A single opt-in preview, scoped to the currently open layout editor. */
final class Live2DEditorPreview {
    private static final int MAX_PREVIEW_SURFACE = 1024;
    private static final long RENDER_GRACE = 250_000_000L;
    private static LayoutEditorScreen editor;
    private static String selectedElement;
    private static Live2DSession session;
    private static Live2DSettings failedSettings;
    private static long lastRender;

    static boolean selected(LayoutEditorScreen layoutEditor, Live2DElement element) {
        return editor == layoutEditor && element.getInstanceIdentifier().equals(selectedElement);
    }

    static void toggle(LayoutEditorScreen layoutEditor, Live2DElement element) {
        boolean wasSelected = selected(layoutEditor, element);
        clear();
        if (!wasSelected) {
            editor = layoutEditor;
            selectedElement = element.getInstanceIdentifier();
        }
    }

    static void render(Live2DElement element, GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        var current = LayoutEditorScreen.getCurrentInstance();
        if (current == null || !selected(current, element) || Minecraft.getInstance().screen != current) {
            element.placeholder(graphics, "fancyl2d.preview");
            return;
        }
        String message = renderSelected(element, graphics, mouseX, mouseY);
        if (message != null) element.placeholder(graphics, message);
    }

    private static String renderSelected(Live2DElement element, GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int width = element.getAbsoluteWidth();
        int height = element.getAbsoluteHeight();
        if (width <= 0 || height <= 0) return null;
        var settings = element.settings();
        if (session != null && !session.settings.equals(settings)) closeSession();
        if (settings.model().isBlank()) return "fancyl2d.no_model";
        if (settings.equals(failedSettings)) return "fancyl2d.load_error";
        String unavailable = BrowserAvailability.unavailableMessage();
        if (unavailable != null) {
            closeSession();
            return unavailable;
        }

        double scale = Live2DElement.surfaceScale(width, height, MAX_PREVIEW_SURFACE);
        int surfaceWidth = Math.max(1, (int) Math.round(width * scale));
        int surfaceHeight = Math.max(1, (int) Math.round(height * scale));
        try {
            if (session == null) {
                String coreStatus = CubismCoreDownload.unavailableMessage();
                if (coreStatus != null) return coreStatus;
                session = new Live2DSession(settings, surfaceWidth, surfaceHeight);
            }
            if (session.failed()) {
                FancyL2D.LOGGER.warn("Live2D editor preview failed or timed out for {}", settings.model());
                closeSession();
                failedSettings = settings;
                return "fancyl2d.load_error";
            }
            lastRender = System.nanoTime();
            if (!session.isOwnedBy(element)) session.attach(element);
            if (!session.ready()) return "fancyl2d.loading";
            session.render(graphics, element.getAbsoluteX(), element.getAbsoluteY(), width, height, 1,
                surfaceWidth, surfaceHeight, mouseX, mouseY);
            return null;
        } catch (IOException | RuntimeException | LinkageError failure) {
            closeSession();
            failedSettings = settings;
            FancyL2D.LOGGER.warn("Could not render Live2D editor preview for {}", settings.model(), failure);
            return "fancyl2d.load_error";
        }
    }

    static void tick() {
        if (editor == null) return;
        if (LayoutEditorScreen.getCurrentInstance() != editor) {
            clear();
            return;
        }
        boolean editorVisible = Minecraft.getInstance().screen == editor;
        if (editorVisible && !(editor.getElementByInstanceIdentifier(selectedElement) instanceof Live2DEditorElement)) {
            clear();
            return;
        }
        if (session != null && (!editorVisible || System.nanoTime() - lastRender > RENDER_GRACE)) {
            try {
                session.updateAnimation(true);
            } catch (RuntimeException | LinkageError failure) {
                failedSettings = session.settings;
                closeSession();
                FancyL2D.LOGGER.warn("Could not pause Live2D editor preview", failure);
            }
        }
    }

    static void clear() {
        closeSession();
        editor = null;
        selectedElement = null;
        failedSettings = null;
    }

    private static void closeSession() {
        if (session == null) return;
        var old = session;
        session = null;
        old.close();
    }

    private Live2DEditorPreview() {}
}
