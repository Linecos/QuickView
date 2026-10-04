package dev.quickview.gui;

import io.github.cottonmc.cotton.gui.client.ScreenDrawing;
import io.github.cottonmc.cotton.gui.widget.icon.Icon;
import net.minecraft.client.gui.DrawContext;

/**
 * 编辑页删除按钮的垃圾桶图标：纯矩形像素画（与齿轮/箭头同一套画法，零外部资源依赖），
 * 红色以表达「危险操作」。
 *
 * <p>结构（12×12，画在 16×16 图标区内居中）：
 * <pre>
 *      ▄▄▄        桶盖提手（4×1）
 *   ▄▄▄▄▄▄▄▄▄▄    桶盖（10×2）
 *    ██ ██ ██     桶身（3 根 2px 竖条，1px 缝隙透出按钮底纹）
 * </pre>
 */
public class TrashIcon implements Icon {
    /** 危险红：在原版灰色按钮底纹上足够醒目。 */
    private static final int COLOR = 0xFFD9534F;
    private static final int SHAPE_W = 12;
    private static final int SHAPE_H = 12;

    @Override
    public void paint(DrawContext context, int x, int y, int size) {
        int ox = x + (size - SHAPE_W) / 2;
        int oy = y + (size - SHAPE_H) / 2;

        // 提手 + 桶盖
        rect(context, ox + 4, oy, 4, 1);
        rect(context, ox + 1, oy + 1, 10, 2);

        // 桶身：3 根竖条，1px 缝隙
        rect(context, ox + 2, oy + 4, 2, 8);
        rect(context, ox + 5, oy + 4, 2, 8);
        rect(context, ox + 8, oy + 4, 2, 8);
    }

    private static void rect(DrawContext context, int x, int y, int w, int h) {
        ScreenDrawing.coloredRect(context, x, y, w, h, COLOR);
    }
}
