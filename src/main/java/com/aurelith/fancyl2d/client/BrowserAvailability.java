package com.aurelith.fancyl2d.client;

import com.aurelith.fancyl2d.FancyL2D;
import de.keksuccino.fancymenu.util.rinku.RinkuUtil;
import net.neoforged.fml.ModList;

/** Reports whether it is safe to create a browser without starting Rinku ourselves. */
final class BrowserAvailability {
    private static final long WAIT_LIMIT = 45_000_000_000L;
    private static long waitingSince;
    private static boolean watching;
    private static boolean probeFailed;

    static String unavailableMessage() {
        if (!ModList.get().isLoaded("rinku")) return "fancyl2d.no_rinku";
        if (RinkuUtil.rinku_critical_failure || probeFailed) return "fancyl2d.rinku_failed";
        if (!RinkuUtil.isRinkuLoaded()) return "fancyl2d.no_rinku";
        try {
            switch (RinkuView.startup()) {
                case READY -> {
                    watching = false;
                    return null;
                }
                case FAILED -> { return "fancyl2d.rinku_failed"; }
                case WAITING -> {
                    long now = System.nanoTime();
                    if (!watching) {
                        waitingSince = now;
                        watching = true;
                    }
                    return now - waitingSince >= WAIT_LIMIT ? "fancyl2d.rinku_timeout" : "fancyl2d.loading_rinku";
                }
            }
        } catch (RuntimeException | LinkageError failure) {
            probeFailed = true;
            FancyL2D.LOGGER.warn("Could not read Rinku startup state", failure);
            return "fancyl2d.rinku_failed";
        }
        throw new AssertionError("Unknown Rinku startup state");
    }

    enum Startup { READY, FAILED, WAITING }

    private BrowserAvailability() {}
}
