package dev.quickview;

import com.github.houbb.pinyin.constant.enums.PinyinStyleEnum;
import com.github.houbb.pinyin.util.PinyinHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 书签搜索的匹配键生成：原文、全拼、声母。
 *
 * <p>拼音转换用 <a href="https://github.com/houbb/pinyin">houbb/pinyin</a>（Apache-2.0），
 * 它做的是<b>词级分词</b>转换，多音字按词组上下文取音：
 * {@code 重庆 → chongqing}、{@code 长沙 → changsha}、{@code 银行 → yinhang}、{@code 音乐 → yinyue}，
 * 声母 {@code 重庆 → cq}、{@code 音乐 → yy}。通过 Loom 的 {@code include} 打成 jar-in-jar，用户无需单独安装。
 *
 * <p>六个匹配键（小写、去重后返回）：
 * <ul>
 *   <li><b>原文</b>：整名小写（含标点），用于直接输入中文或英文名</li>
 *   <li><b>词级全拼</b>：houbb 按词组上下文给出的全拼，多音字取的是<b>正确</b>读音
 *       （{@code 重庆 → chongqing}）；标点原样保留</li>
 *   <li><b>去标点全拼</b>：在上面基础上删除所有标点（{@code Home-1} → {@code home1}）。
 *       单独留一个键是因为 houbb 对非中文字符是原样透传的，不会像预期那样丢掉 {@code -} / {@code _} / 空格</li>
 *   <li><b>逐字全拼</b>：每个汉字用<b>常用读音</b>直接拼接（{@code 重庆 → zhongqing}）。
 *       它专门用来兼容「按习惯读错音」的输入 —— 配合词级全拼，{@code chongqing} 与 {@code zhongqing} 都能命中</li>
 *   <li><b>词级声母</b>：由 houbb 按词组给出的首字母（{@code 重庆 → cq}、{@code 矿洞Boss房 → kdbossf}）</li>
 *   <li><b>逐字声母</b>：每段连续字母/数字只取首字符（{@code 矿洞Boss房 → kdbf}、{@code Home-1 → h1}），
 *       汉字部分同样是常用读音，于是「重庆」得到 {@code zq}</li>
 * </ul>
 *
 * <p>匹配方式是子串包含，因此不支持「中文与拼音混输」（如 {@code 矿dong}）。
 */
public final class PinyinSearch {
    private static final int KEY_CAPACITY = 6;

    private PinyinSearch() {
    }

    /**
     * 预热 houbb 的拼音字典。首次转换要加载词典、实测约 250ms，
     * 应在 mod 初始化时放到后台线程调用，避免第一次敲搜索框时出现卡顿。
     */
    public static void warmUp() {
        try {
            PinyinHelper.toPinyin("预热", PinyinStyleEnum.NORMAL, "");
        } catch (RuntimeException ignored) {
            // 预热失败不影响功能，真正用到时会再初始化一次
        }
    }

    /** 生成一个书签名的全部匹配键，全部小写并已去重。 */
    public static List<String> keysOf(String name) {
        List<String> keys = new ArrayList<>(KEY_CAPACITY);
        if (name == null || name.isEmpty()) {
            return keys;
        }

        StringBuilder charWiseFull = new StringBuilder(name.length());
        StringBuilder charWiseInitials = new StringBuilder(name.length());
        buildCharWise(name, charWiseFull, charWiseInitials);

        String wordFull = toPinyin(name, PinyinStyleEnum.NORMAL);

        addKey(keys, name.toLowerCase(Locale.ROOT));
        addKey(keys, wordFull);
        addKey(keys, alphanumericOnly(wordFull));
        addKey(keys, charWiseFull.toString());
        addKey(keys, toPinyin(name, PinyinStyleEnum.FIRST_LETTER));
        addKey(keys, charWiseInitials.toString());
        return keys;
    }

    private static String toPinyin(String name, PinyinStyleEnum style) {
        String result = PinyinHelper.toPinyin(name, style, "");
        return result == null ? "" : result.toLowerCase(Locale.ROOT);
    }

    /** 只保留字母与数字（转换后的全拼里已没有中文，所以只影响标点）。 */
    private static String alphanumericOnly(String s) {
        StringBuilder builder = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                builder.append(c);
            }
        }
        return builder.toString();
    }

    /**
     * 逐字转换：汉字用常用读音（不做词级消歧），标点丢弃，连续字母/数字段只取首字符进声母。
     * <p>
     * 「某字符是否被转换过」用来判断它是不是汉字 —— houbb 对非中文字符是原样透传的，
     * 转换结果与输入不同即说明发生了拼音转换，这样就不必写死 CJK 码点范围。
     */
    private static void buildCharWise(String name, StringBuilder full, StringBuilder initials) {
        boolean inAsciiRun = false;

        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            String converted = PinyinHelper.toPinyin(String.valueOf(c), PinyinStyleEnum.NORMAL, "");

            if (converted != null && !converted.isEmpty() && !converted.equals(String.valueOf(c))) {
                String lower = converted.toLowerCase(Locale.ROOT);
                full.append(lower);
                initials.append(lower.charAt(0));
                inAsciiRun = false;
            } else if (Character.isLetterOrDigit(c)) {
                char lower = Character.toLowerCase(c);
                full.append(lower);
                if (!inAsciiRun) {
                    initials.append(lower);
                }
                inAsciiRun = true;
            } else {
                inAsciiRun = false;
            }
        }
    }

    private static void addKey(List<String> keys, String key) {
        if (key != null && !key.isEmpty() && !keys.contains(key)) {
            keys.add(key);
        }
    }
}
