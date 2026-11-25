package com.aurelith.fancyl2d.client;

import com.google.gson.JsonObject;

record Live2DSettings(String model, double scale, double follow, boolean idle, double sway, int fps) {
    static final Live2DSettings DEFAULT = new Live2DSettings(ModelSource.DEFAULT_ID, 1, 0.65, true, 0, 60);

    JsonObject pageConfig(String modelUrl) {
        var config = new JsonObject();
        config.addProperty("model", modelUrl);
        config.addProperty("scale", scale);
        config.addProperty("follow", follow);
        config.addProperty("idle", idle);
        config.addProperty("sway", sway);
        config.addProperty("fps", fps);
        return config;
    }

    static double number(String input, double fallback, double min, double max) {
        if (input == null) return fallback;
        try {
            double value = Double.parseDouble(input);
            return Double.isFinite(value) ? Math.clamp(value, min, max) : fallback;
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    static int frameRate(String input, int fallback) {
        if (input == null) return fallback;
        try {
            return Math.clamp(Integer.parseInt(input.trim()), 15, 120);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }
}
