package com.aurelith.fancyl2d.client;

import de.keksuccino.fancymenu.customization.element.ElementRegistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.GameShuttingDownEvent;

public final class ClientIntegration {
    public static void register(IEventBus bus) {
        bus.addListener(ClientIntegration::setup);
        NeoForge.EVENT_BUS.addListener(ClientIntegration::shutdown);
        NeoForge.EVENT_BUS.addListener(ClientIntegration::tick);
    }

    private static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ElementRegistry.register(new Live2DElementBuilder()));
    }

    private static void shutdown(GameShuttingDownEvent event) {
        Live2DEditorPreview.clear();
        Live2DSessions.clear();
    }

    private static void tick(ClientTickEvent.Post event) {
        Live2DSessions.tick();
        Live2DEditorPreview.tick();
    }
    private ClientIntegration() {}
}
