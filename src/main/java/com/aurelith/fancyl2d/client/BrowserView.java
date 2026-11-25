package com.aurelith.fancyl2d.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;

interface BrowserView extends AutoCloseable {
    void resize(int width, int height);
    void focus(double x, double y);
    void setAnimating(boolean running);
    boolean draw(GuiGraphicsExtractor graphics, int x, int y, int width, int height, float opacity);
    @Override void close();
}
