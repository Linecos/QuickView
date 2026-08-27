package dev.quickview;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuickViewClient implements ClientModInitializer {
    public static final String MOD_ID = "quickview";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("QuickView initializing...");
        QuickViewKeybindings.register();
        LOGGER.info("QuickView initialized.");
    }
}
