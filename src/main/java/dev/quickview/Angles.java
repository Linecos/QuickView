package dev.quickview;

/**
 * 角度工具。刻意做成不依赖 Minecraft / Fabric 的纯函数，便于单元测试。
 */
public final class Angles {
    private Angles() {
    }

    /**
     * 两个偏航角之间的最短角度差，返回 {@code [-180, 180)}，避免转身时绕远路。
     * <p>
     * Java 的 {@code %} 取负时结果仍为负，所以循环会把 {@code (to - from)} 折回区间：
     * 例如 {@code from=170, to=-170} 返回 {@code +20}（顺时针 20°，而非 −340°）。
     */
    public static float shortestDelta(float from, float to) {
        float delta = (to - from) % 360.0f;
        if (delta >= 180.0f) {
            delta -= 360.0f;
        } else if (delta < -180.0f) {
            delta += 360.0f;
        }
        return delta;
    }
}
