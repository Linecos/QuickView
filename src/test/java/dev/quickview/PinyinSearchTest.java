package dev.quickview;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 拼音匹配键的回归测试。
 * <p>
 * 这些是「多音字按词组上下文取音」等行为的事实基线（见 PROJECT_MEMORY §4.4），
 * 换库或改动 {@link PinyinSearch} 时若结果变了，会在这里立刻暴露。
 * 期望值来自文档与实测，顺序即 keysOf 的生成顺序（原文 / 词级全拼 / 去标点全拼 / 逐字全拼 / 词级声母 / 逐字声母，去重后）。
 */
class PinyinSearchTest {

    @Test
    void emptyOrNullHasNoKeys() {
        assertEquals(List.of(), PinyinSearch.keysOf(null));
        assertEquals(List.of(), PinyinSearch.keysOf(""));
    }

    @Test
    void chongqingUsesCorrectPolyphone() {
        // 重庆：词级取 chong，逐字取 zhong（兼容读错音），声母 cq / zq
        assertEquals(List.of("重庆", "chongqing", "zhongqing", "cq", "zq"), PinyinSearch.keysOf("重庆"));
    }

    @Test
    void changshaUsesCorrectPolyphone() {
        assertEquals(List.of("长沙", "changsha", "zhangsha", "cs", "zs"), PinyinSearch.keysOf("长沙"));
    }

    @Test
    void bankAndMusicPolyphones() {
        assertEquals(List.of("银行", "yinhang", "yinxing", "yh", "yx"), PinyinSearch.keysOf("银行"));
        assertEquals(List.of("音乐", "yinyue", "yinle", "yy", "yl"), PinyinSearch.keysOf("音乐"));
    }

    @Test
    void wordLevelInitialsForPlainChinese() {
        // 家里蹲：三个字，词级声母与逐字声母一致（jld）
        assertEquals(List.of("家里蹲", "jialidun", "jld"), PinyinSearch.keysOf("家里蹲"));
        assertEquals(List.of("矿洞", "kuangdong", "kd"), PinyinSearch.keysOf("矿洞"));
    }

    @Test
    void mixedChineseAndAscii() {
        // 矿洞Boss房：词级声母 kdbossf（Boss 视为一段），逐字声母 kdbf（连续字母段只取首字母）
        assertEquals(List.of("矿洞boss房", "kuangdongbossfang", "kdbossf", "kdbf"),
                PinyinSearch.keysOf("矿洞Boss房"));
    }

    @Test
    void asciiNamesKeepDashedAndAlnumVariants() {
        assertEquals(List.of("home-1", "home1", "h1"), PinyinSearch.keysOf("Home-1"));
        assertEquals(List.of("abc", "a"), PinyinSearch.keysOf("ABC"));
    }

    @Test
    void allKeysAreLowercaseAndDeduped() {
        for (String name : List.of("重庆", "矿洞Boss房", "Home-1", "ABC")) {
            List<String> keys = PinyinSearch.keysOf(name);
            for (String key : keys) {
                assertEquals(key.toLowerCase(java.util.Locale.ROOT), key, "key not lowercase: " + key);
            }
            assertEquals(keys.stream().distinct().count(), keys.size(), "duplicate keys: " + keys);
        }
    }

    @Test
    void resultsAreCachedAndImmutable() {
        List<String> first = PinyinSearch.keysOf("重庆");
        List<String> second = PinyinSearch.keysOf("重庆");
        assertEquals(first, second);
        assertFalse(first.isEmpty());
        // List.copyOf 的结果不可变：外部误改会抛异常，而不是悄悄污染缓存
        assertThrows(UnsupportedOperationException.class, () -> first.add("污染"));
        assertTrue(first.contains("chongqing"));
    }
}
