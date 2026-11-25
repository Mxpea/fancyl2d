package com.aurelith.fancyl2d.client;

import de.keksuccino.rinku.Rinku;
import de.keksuccino.rinku.RinkuBrowser;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import org.cef.CefApp;
import org.cef.browser.CefPaintElementType;

// No Rinku types escape this class so it is only linked on the installed/ready path.
final class RinkuView implements BrowserView {
    private final RinkuBrowser browser;

    static BrowserAvailability.Startup startup() {
        return switch (CefApp.getState()) {
            case INITIALIZATION_FAILED, SHUTTING_DOWN, TERMINATED -> BrowserAvailability.Startup.FAILED;
            default -> Rinku.isInitialized() ? BrowserAvailability.Startup.READY : BrowserAvailability.Startup.WAITING;
        };
    }

    RinkuView(String url, int width, int height, int fps) {
        browser = Rinku.createBrowser(url, true, width, height);
        try {
            browser.setWindowlessFrameRate(fps);
        } catch (RuntimeException | LinkageError failure) {
            browser.close();
            throw failure;
        }
    }

    @Override public void resize(int width, int height) { browser.resize(width, height); }

    @Override public void focus(double x, double y) {
        browser.executeJavaScript("window.fancyL2dFocus?.(" + x + "," + y + ");", browser.getURL(), 0);
    }

    @Override public void setAnimating(boolean running) {
        browser.executeJavaScript("window.fancyL2dAnimate?.(" + running + ");", browser.getURL(), 0);
        if (running) browser.invalidate(CefPaintElementType.PET_VIEW);
    }

    @Override public boolean draw(GuiGraphicsExtractor graphics, int x, int y, int width, int height, float opacity) {
        if (!browser.isTextureReady()) return false;
        int textureWidth = browser.getRenderer().getTextureWidth();
        int textureHeight = browser.getRenderer().getTextureHeight();
        if (textureWidth <= 0 || textureHeight <= 0) return false;
        int color = ((int) (Math.clamp(opacity, 0, 1) * 255) << 24) | 0xffffff;
        graphics.blit(RenderPipelines.GUI_TEXTURED, browser.getTextureIdentifier(), x, y, 0, 0,
            width, height, textureWidth, textureHeight, textureWidth, textureHeight, color);
        return true;
    }

    @Override public void close() { browser.close(); }
}
