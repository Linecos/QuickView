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
        // 拼音字典首次使用要初始化约 250ms，放后台线程预热，避免第一次敲搜索框时卡顿
        Thread pinyinWarmUp = new Thread(PinyinSearch::warmUp, "QuickView-PinyinWarmUp");
        pinyinWarmUp.setDaemon(true);
        pinyinWarmUp.start();

        QuickViewKeybindings.register();
        LOGGER.info("QuickView initialized.");
    }
}
