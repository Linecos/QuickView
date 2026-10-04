package dev.quickview;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 全局设置，持久化在 {@code <gameDir>/config/quickview.json}。
 *
 * <p>字段名即 JSON 键名。读取失败（文件不存在 / 损坏 / 版本不认识）时全部退回默认值，
 * 不会因为配置文件问题让 Mod 起不来。
 */
public final class QuickViewConfig {
    /** 平滑过渡默认时长（毫秒），目前不开放给界面调整。 */
    public static final int DEFAULT_TRANSITION_MILLIS = 300;
    private static final int MIN_TRANSITION_MILLIS = 50;
    private static final int MAX_TRANSITION_MILLIS = 2000;

    private static final int SCHEMA_VERSION = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE =
            FabricLoader.getInstance().getConfigDir().resolve("quickview.json");

    private int version = SCHEMA_VERSION;
    private boolean quickAddEnabled = true;
    private boolean freeMoveEnabled = true;
    private boolean preferFreecam = true;
    private boolean smoothTransitionEnabled = false;
    private int smoothTransitionMillis = DEFAULT_TRANSITION_MILLIS;

    public static QuickViewConfig load() {
        if (!Files.exists(FILE)) {
            return new QuickViewConfig();
        }
        try (Reader reader = Files.newBufferedReader(FILE)) {
            QuickViewConfig loaded = GSON.fromJson(reader, QuickViewConfig.class);
            if (loaded == null) {
                return new QuickViewConfig();
            }
            loaded.smoothTransitionMillis =
                    clampMilliseconds(loaded.smoothTransitionMillis);
            return loaded;
        } catch (Exception e) {
            QuickViewClient.LOGGER.error("Failed to load config '{}': {}", FILE, e.getMessage());
            return new QuickViewConfig();
        }
    }

    public void save() {
        JsonFile.write(FILE, this, GSON, "config");
    }

    private static int clampMilliseconds(int value) {
        if (value < MIN_TRANSITION_MILLIS) {
            return DEFAULT_TRANSITION_MILLIS;
        }
        return Math.min(value, MAX_TRANSITION_MILLIS);
    }

    public int getVersion() {
        return version;
    }

    public boolean isQuickAddEnabled() {
        return quickAddEnabled;
    }

    public void setQuickAddEnabled(boolean quickAddEnabled) {
        this.quickAddEnabled = quickAddEnabled;
    }

    public boolean isFreeMoveEnabled() {
        return freeMoveEnabled;
    }

    public void setFreeMoveEnabled(boolean freeMoveEnabled) {
        this.freeMoveEnabled = freeMoveEnabled;
    }

    public boolean isPreferFreecam() {
        return preferFreecam;
    }

    public void setPreferFreecam(boolean preferFreecam) {
        this.preferFreecam = preferFreecam;
    }

    public boolean isSmoothTransitionEnabled() {
        return smoothTransitionEnabled;
    }

    public void setSmoothTransitionEnabled(boolean smoothTransitionEnabled) {
        this.smoothTransitionEnabled = smoothTransitionEnabled;
    }

    public int getSmoothTransitionMillis() {
        return smoothTransitionMillis;
    }
}
