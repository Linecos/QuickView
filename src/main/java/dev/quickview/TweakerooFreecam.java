package dev.quickview;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 可选地把相机交给 Tweakeroo 的「灵魂出窍」（Free Camera）来驱动。
 *
 * <p><b>为什么用反射而不是编译期依赖</b>：Tweakeroo 是玩家侧的可选 mod，若加编译期依赖，
 * 没装 Tweakeroo 的构建（以及 CI）都会失败。反射在类初始化时一次性解析全部签名，
 * 任何一处对不上就整体降级（{@link #isAvailable()} = false），由调用方回落到 QuickView 自带的
 * 自由视角 —— 不会崩、也不会半残。
 *
 * <p>用到的 API（按 1.21.11 / Tweakeroo 0.27.15 的实际签名核对过）：
 * <ul>
 *   <li>{@code FeatureToggle.TWEAK_FREE_CAMERA.setBooleanValue(boolean)}：开关本身。
 *       它的回调里会调 {@code CameraEntity.setCameraState(...)} 创建/销毁相机，
 *       并把状态写进 Tweakeroo 自己的配置（玩家在 Tweakeroo 设置里能看到它被打开）</li>
 *   <li>{@code CameraEntity.updatePositionAtPreset(CameraPreset)}：把相机摆到指定坐标与朝向
 *       （内部会处理 chunk 重建；维度不匹配时自动跳过）</li>
 *   <li>{@code CameraEntity.getCamera()}：用来确认相机是否真的建起来了</li>
 * </ul>
 */
public final class TweakerooFreecam {
    private static final String CLAZZ_FEATURE_TOGGLE = "fi.dy.masa.tweakeroo.config.FeatureToggle";
    private static final String CLAZZ_CAMERA_ENTITY = "fi.dy.masa.tweakeroo.util.CameraEntity";
    private static final String CLAZZ_CAMERA_PRESET = "fi.dy.masa.tweakeroo.util.CameraPreset";

    /** 传给 {@code CameraPreset} 的 id：只要 &gt; -1 就是合法 id（这个预设不注册，用完即丢）。 */
    private static final int PRESET_ID = 0;

    private static boolean available;
    private static Object freeCameraToggle;
    private static Method getToggleValue;
    private static Method setToggleValue;
    private static Method updatePositionAtPreset;
    private static Method getCamera;
    private static Constructor<?> presetConstructor;

    static {
        if (FabricLoader.getInstance().isModLoaded("tweakeroo")) {
            try {
                Class<?> featureToggleClass = Class.forName(CLAZZ_FEATURE_TOGGLE);
                Field field = featureToggleClass.getField("TWEAK_FREE_CAMERA");
                freeCameraToggle = field.get(null);
                getToggleValue = freeCameraToggle.getClass().getMethod("getBooleanValue");
                setToggleValue = freeCameraToggle.getClass().getMethod("setBooleanValue", boolean.class);

                Class<?> cameraEntityClass = Class.forName(CLAZZ_CAMERA_ENTITY);
                Class<?> cameraPresetClass = Class.forName(CLAZZ_CAMERA_PRESET);
                updatePositionAtPreset = cameraEntityClass.getMethod("updatePositionAtPreset", cameraPresetClass);
                getCamera = cameraEntityClass.getMethod("getCamera");
                presetConstructor = cameraPresetClass.getConstructor(int.class, String.class, Identifier.class,
                        Vec3d.class, float.class, float.class);
                available = true;
            } catch (Throwable t) {
                // 版本不匹配就走自带实现，不影响 QuickView 本身能用
                QuickViewClient.LOGGER.warn("Tweakeroo 的自由相机 API 不匹配，已禁用该选项: {}", t.toString());
            }
        }
    }

    private TweakerooFreecam() {
    }

    /** 是否成功接通 Tweakeroo（装了且签名对得上）。 */
    public static boolean isAvailable() {
        return available;
    }

    /** Tweakeroo 那边当前是否开着自由相机（可能是玩家自己在 Tweakeroo 里开的）。 */
    public static boolean isEnabled() {
        if (!available) {
            return false;
        }
        try {
            return (Boolean) getToggleValue.invoke(freeCameraToggle);
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * 打开 Tweakeroo 的自由相机，并把它摆到书签的坐标与朝向。
     *
     * @return 成功为 true；未安装 / API 不匹配 / 世界未就绪（相机没建起来）为 false —— 调用方应回落
     */
    public static boolean enableAt(Viewpoint viewpoint) {
        if (!available || viewpoint == null) {
            return false;
        }
        // dimension 可能因 JSON 缺字段未被归一化（如直接构造的实例）而为 null，
        // tryParse → trySplitOn 里直接 id.indexOf(':')，null 必 NPE；先挡一道兜底
        String dimensionId = viewpoint.getDimension();
        if (dimensionId == null) {
            return false;
        }
        // 维度非法（不在任何命名空间里 / 空）时 tryParse 返回 null：调用方回落自带实现
        Identifier dimension = Identifier.tryParse(dimensionId);
        if (dimension == null) {
            return false;
        }
        try {
            // 先开开关：Tweakeroo 的回调会创建相机实体（此时仍在玩家位置）
            setToggleValue.invoke(freeCameraToggle, true);
            if (getCamera.invoke(null) == null) {
                // 世界/玩家还没就绪，它自己放弃了；把开关退回去，避免留下半开状态
                setToggleValue.invoke(freeCameraToggle, false);
                return false;
            }
            // 再把它摆到书签位置/朝向。预设不注册进 Tweakeroo 的预设列表，用完即丢
            Object preset = presetConstructor.newInstance(PRESET_ID, "quickview", dimension,
                    new Vec3d(viewpoint.getX(), viewpoint.getY(), viewpoint.getZ()),
                    viewpoint.getYaw(), viewpoint.getPitch());
            updatePositionAtPreset.invoke(null, preset);
            return true;
        } catch (Throwable t) {
            QuickViewClient.LOGGER.warn("切换到 Tweakeroo 自由相机失败: {}", t.toString());
            // 可能已经打开了开关才失败：尽量再关掉，避免和调用方回落的自带相机同时生效
            try {
                setToggleValue.invoke(freeCameraToggle, false);
            } catch (Throwable ignored) {
                // 关不掉也只能记日志了，至少别把异常再抛出去
            }
            return false;
        }
    }

    /** 关掉 Tweakeroo 的自由相机（已经是关的就什么都不做，免得白写它的配置）。 */
    public static void disable() {
        if (!available || !isEnabled()) {
            return;
        }
        try {
            setToggleValue.invoke(freeCameraToggle, false);
        } catch (Throwable t) {
            QuickViewClient.LOGGER.warn("关闭 Tweakeroo 自由相机失败: {}", t.toString());
        }
    }
}
