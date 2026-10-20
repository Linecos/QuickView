package dev.quickview;

import java.util.regex.Pattern;

/**
 * 存储路径名清洗：把「服务器地址 / 存档名 / 维度 ID」这类任意字符串转成安全的文件/目录名。
 * <p>
 * 刻意做成不依赖 Minecraft / Fabric 的纯函数，便于单元测试（原先是 {@link ViewpointStorage} 的私有方法，
 * 但那个类的静态初始化会访问 FabricLoader，导致无法在测试里直接调用）。
 */
public final class StorageNames {
    /** Windows 非法字符与控制符 → 下划线；尾部空格/点（Windows 会吞掉）也会被处理。 */
    private static final Pattern ILLEGAL_CHARS = Pattern.compile("[\\\\/:*?\"<>|\\p{Cntrl}]");
    private static final Pattern TRAILING_DOTS_SPACES = Pattern.compile("[. ]+$");
    private static final Pattern RESERVED_NUMBERED = Pattern.compile("(COM|LPT)[1-9]");

    private StorageNames() {
    }

    /** 清洗单个路径片段；空 / 非法 / Windows 保留名都回退或加前缀，保证结果可用作文件名。 */
    public static String sanitize(String name) {
        if (name == null) {
            return "unknown";
        }
        String safe = ILLEGAL_CHARS.matcher(name).replaceAll("_").trim();
        safe = TRAILING_DOTS_SPACES.matcher(safe).replaceAll("");
        if (safe.isEmpty() || safe.equals(".") || safe.equals("..")) {
            safe = "unknown";
        }
        if (isReservedWindowsName(safe)) {
            safe = "_" + safe;
        }
        return safe;
    }

    private static boolean isReservedWindowsName(String name) {
        int dot = name.indexOf('.');
        String base = dot >= 0 ? name.substring(0, dot) : name;
        String upper = base.toUpperCase();
        if (upper.equals("CON") || upper.equals("PRN") || upper.equals("AUX") || upper.equals("NUL")) {
            return true;
        }
        return RESERVED_NUMBERED.matcher(upper).matches();
    }
}
