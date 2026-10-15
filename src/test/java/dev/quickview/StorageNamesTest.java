package dev.quickview;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 存储路径名清洗的回归测试（Windows 非法字符、保留名、尾点空格）。
 */
class StorageNamesTest {

    @Test
    void nullAndBlankFallBackToUnknown() {
        assertEquals("unknown", StorageNames.sanitize(null));
        assertEquals("unknown", StorageNames.sanitize(""));
        assertEquals("unknown", StorageNames.sanitize("   "));
        assertEquals("unknown", StorageNames.sanitize("."));
        assertEquals("unknown", StorageNames.sanitize(".."));
    }

    @Test
    void illegalCharsBecomeUnderscore() {
        assertEquals("a_b_c_d_e_f_g_h_i_j", StorageNames.sanitize("a/b\\c:d*e?f\"g<h>i|j"));
        assertEquals("a_b", StorageNames.sanitize("a\u0000b"));                 // 控制符
        assertEquals("minecraft_overworld", StorageNames.sanitize("minecraft:overworld")); // 维度 ID 的真实形态
    }

    @Test
    void trimsAndStripsTrailingDotsAndSpaces() {
        assertEquals("name", StorageNames.sanitize("  name.  "));
        assertEquals("name", StorageNames.sanitize("name "));
        assertEquals("name", StorageNames.sanitize("name..."));
    }

    @Test
    void reservedWindowsNamesGetUnderscorePrefix() {
        assertEquals("_CON", StorageNames.sanitize("CON"));
        assertEquals("_con", StorageNames.sanitize("con"));       // 大小写不敏感
        assertEquals("_AUX", StorageNames.sanitize("AUX"));
        assertEquals("_NUL", StorageNames.sanitize("NUL"));
        assertEquals("_COM1", StorageNames.sanitize("COM1"));
        assertEquals("_LPT9", StorageNames.sanitize("LPT9"));
        assertEquals("_CON.txt", StorageNames.sanitize("CON.txt")); // 只看扩展名前的基名
        assertEquals("COM0", StorageNames.sanitize("COM0"));        // 只有 1-9
        assertEquals("console", StorageNames.sanitize("console"));  // 精确匹配，非前缀
    }

    @Test
    void ordinaryNamesPassThrough() {
        assertEquals("My Server", StorageNames.sanitize("My Server"));
        assertEquals("1.0.0", StorageNames.sanitize("1.0.0")); // 中间的点不受影响
    }
}
