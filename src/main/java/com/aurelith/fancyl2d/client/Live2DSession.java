package com.aurelith.fancyl2d.client;

import com.aurelith.fancyl2d.FancyL2D;
import java.io.IOException;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Owns one page/CEF pair independently of the lifetime of a FancyMenu element object. */
final class Live2DSession implements AutoCloseable {
    private static final long LOAD_TIMEOUT = 30_000_000_000L;
    final Live2DSettings settings;
    private final Live2DPage page;
    private final BrowserView view;
    private final long started = System.nanoTime();
    private int pixelWidth;
    private int pixelHeight;
    private long lastCursor;
    private double lastX = Double.NaN;
    private double lastY = Double.NaN;
    private Live2DElement owner;
    private boolean running = true; // The page starts its tickers as it loads.
    private boolean closed;

    Live2DSession(Live2DSettings settings, int width, int height) throws IOException {
        this.settings = settings;
        var source = ModelSource.resolve(Live2DPage.MODELS, settings.model());
        page = new Live2DPage(source, settings);
        try {
            view = new RinkuView(page.url(), width, height, settings.fps());
        } catch (RuntimeException | LinkageError failure) {
            page.close();
            throw failure;
        }
        pixelWidth = width;
        pixelHeight = height;
        FancyL2D.LOGGER.debug("Opened Live2D browser for {} ({} FPS requested)", settings.model(), settings.fps());
    }

    boolean isOwnedBy(Live2DElement element) { return owner == element; }

    void attach(Live2DElement element) {
        owner = element;
        lastX = lastY = Double.NaN;
    }

    void detach(Live2DElement element) {
        if (owner == element) owner = null;
    }

    boolean ready() { return page.status() == Live2DPage.Status.READY; }

    boolean failed() {
        return page.status() == Live2DPage.Status.ERROR
            || (page.status() == Live2DPage.Status.LOADING && System.nanoTime() - started > LOAD_TIMEOUT);
    }

    void updateAnimation(boolean keepHidden) {
        if (page.status() != Live2DPage.Status.READY) return;
        boolean shouldRun = owner != null && !keepHidden;
        if (shouldRun != running) {
            view.setAnimating(shouldRun);
            running = shouldRun;
        }
    }

    void render(GuiGraphicsExtractor graphics, int x, int y, int width, int height, float opacity,
                   int surfaceWidth, int surfaceHeight, int mouseX, int mouseY) {
        if (surfaceWidth != pixelWidth || surfaceHeight != pixelHeight) {
            view.resize(surfaceWidth, surfaceHeight);
            pixelWidth = surfaceWidth;
            pixelHeight = surfaceHeight;
        }
        if (!ready()) return;
        updateAnimation(false);
        if (settings.follow() > 0) {
            double targetX = Math.clamp(2.0 * (mouseX - x) / width - 1, -1, 1);
            double targetY = Math.clamp(1 - 2.0 * (mouseY - y) / height, -1, 1);
            long now = System.nanoTime();
            if ((targetX != lastX || targetY != lastY) && now - lastCursor >= 1_000_000_000L / Math.min(settings.fps(), 60)) {
                view.focus(targetX, targetY);
                lastCursor = now;
                lastX = targetX;
                lastY = targetY;
            }
        }
        view.draw(graphics, x, y, width, height, opacity);
    }

    @Override public void close() {
        if (closed) return;
        closed = true;
        owner = null;
        try {
            view.close();
        } catch (RuntimeException | LinkageError failure) {
            FancyL2D.LOGGER.warn("Could not close Live2D browser", failure);
        } finally {
            page.close();
            FancyL2D.LOGGER.debug("Closed Live2D browser session for {}", settings.model());
        }
    }
}
