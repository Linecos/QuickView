package dev.quickview.gui;

import io.github.cottonmc.cotton.gui.client.BackgroundPainter;
import io.github.cottonmc.cotton.gui.client.ScreenDrawing;
import io.github.cottonmc.cotton.gui.widget.WWidget;

/**
 * 下拉列表的统一视觉：原版风格的深色悬浮面板（描边 + 上亮下暗斜面边）。
 *
 * <p>之前用一层近纯黑底色（{@code 0xE0101010}），但列表行几乎填满面板，底色只从行间缝隙
 * 露出几条死黑，看起来像渲染残渣。改用 {@link ScreenDrawing#drawGuiPanel} 画完整的
 * 斜面面板：行内容四周留出边距后，面板的描边和明暗边都能看见，才像一块「悬浮的菜单」。
 */
final class DropdownStyle {
    /** 面板底色（近不透明深灰）。 */
    private static final int PANEL = 0xF0181818;
    /** 内侧下/右暗边。 */
    private static final int SHADOW = 0xFF101010;
    /** 内侧上/左亮边。 */
    private static final int HILIGHT = 0xFF454545;
    /** 描边：与齿轮/箭头同色的暖白，统一图标语言。 */
    private static final int OUTLINE = 0xFFE8E6DF;

    private DropdownStyle() {
    }

    /** 下拉列表面板的背景 painter。 */
    static final BackgroundPainter LIST_BG = (context, left, top, panel) ->
            ScreenDrawing.drawGuiPanel(context, left, top, panel.getWidth(), panel.getHeight(),
                    SHADOW, PANEL, HILIGHT, OUTLINE);

    /** 行内容距面板左/右边框的留白（要露出 1px 描边 + 2px 明暗边）。 */
    static final int ROW_INSET = 3;
    /** 面板总高 = 行数 × 行高 + 垂直留白（上 2px、下 3px，凑成 5 的倍数以适配 5px 网格）。 */
    static final int VERTICAL_PADDING = 5;
    /** 首行距面板顶部的留白。 */
    static final int ROW_TOP = 2;
}
