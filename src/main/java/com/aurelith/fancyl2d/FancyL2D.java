package com.aurelith.fancyl2d;

import com.aurelith.fancyl2d.client.ClientIntegration;
import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(value = FancyL2D.MODID, dist = Dist.CLIENT)
public final class FancyL2D {
    public static final String MODID = "fancyl2d";
    public static final Logger LOGGER = LogUtils.getLogger();

    public FancyL2D(IEventBus modEventBus) {
        ClientIntegration.register(modEventBus);
    }
}
