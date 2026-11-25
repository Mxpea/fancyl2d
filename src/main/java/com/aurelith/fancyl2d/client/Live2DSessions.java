package com.aurelith.fancyl2d.client;

import com.aurelith.fancyl2d.FancyL2D;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;

/** Client-thread cache. Before joining a world, detaching is not destruction. */
final class Live2DSessions {
    record Key(String layout, String element) {}

    private static final Map<Key, Live2DSession> SESSIONS = new HashMap<>();
    private static boolean inWorld;

    static Live2DSession acquire(Key key, Live2DElement owner, Live2DSettings settings, int width, int height) throws IOException {
        syncWorld();
        var session = SESSIONS.get(key);
        if (session != null && !session.settings.equals(settings)) {
            discard(key, session);
            session = null;
        }
        if (session == null) {
            session = new Live2DSession(settings, width, height);
            SESSIONS.put(key, session);
            FancyL2D.LOGGER.debug("Cached Live2D browser for layout {}, element {}", key.layout(), key.element());
        } else if (!session.isOwnedBy(owner)) {
            FancyL2D.LOGGER.debug("Reused Live2D browser for {} (layout {}, element {})", settings.model(), key.layout(), key.element());
        }
        if (!session.isOwnedBy(owner)) session.attach(owner);
        return session;
    }

    static boolean canReuse(Key key, Live2DSettings settings) {
        syncWorld();
        var session = SESSIONS.get(key);
        return session != null && session.settings.equals(settings);
    }

    static void detach(Key key, Live2DSession session, Live2DElement owner) {
        if (session == null || !session.isOwnedBy(owner)) return;
        session.detach(owner);
        if (inWorld) discard(key, session);
    }

    static void discard(Key key, Live2DSession session) {
        if (session != null && SESSIONS.remove(key, session)) session.close();
    }

    static void discard(Key key) { discard(key, SESSIONS.get(key)); }

    static void tick() {
        syncWorld();
        boolean editorOpen = LayoutEditorScreen.getCurrentInstance() != null;
        var it = SESSIONS.values().iterator();
        while (it.hasNext()) {
            var session = it.next();
            try {
                session.updateAnimation(editorOpen);
            } catch (RuntimeException | LinkageError failure) {
                it.remove();
                session.close();
                FancyL2D.LOGGER.warn("Could not update Live2D browser visibility", failure);
            }
        }
    }

    private static void syncWorld() {
        boolean hasWorld = Minecraft.getInstance().level != null;
        if (inWorld != hasWorld) {
            inWorld = hasWorld;
            FancyL2D.LOGGER.debug("Clearing {} Live2D sessions: {} world", SESSIONS.size(), hasWorld ? "entered" : "left");
            clear();
        }
    }

    static void clear() {
        for (var session : SESSIONS.values()) session.close();
        SESSIONS.clear();
    }

    private Live2DSessions() {}
}
