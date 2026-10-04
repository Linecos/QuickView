package dev.quickview.gui;

import io.github.cottonmc.cotton.gui.client.ScreenDrawing;
import io.github.cottonmc.cotton.gui.widget.icon.Icon;
import net.minecraft.client.gui.DrawContext;

/**
 * 下拉箭头图标（∨ / ∧）：纯矩形像素画，不依赖任何字体字形。
 *
 * <p>配合 {@code new WButton(chevron)} 使用：{@code WButton} 把图标画在 {@code x + 2}、
 * 垂直居中，在 20×20 的正方形按钮里刚好四周各留 2px。
 */
public class ChevronIcon implements Icon {
    /** 箭头外接尺寸（像素）。与搜索框右侧叉号（约 7px 字形）视觉相当。 */
    private static final int SHAPE_W = 7;
    private static final int SHAPE_H = 4;
    /** 1px 描边：2px 时箭头显得笨重（用户反馈「像素太粗」），与齿轮的细密风格不搭。 */
    private static final int STROKE = 1;
    private static final int COLOR = 0xFFE8E6DF;

    /** 展开时翻转成 ∧，让按钮本身能表达「已展开」。 */
    private boolean flipped;

    public ChevronIcon setFlipped(boolean flipped) {
        this.flipped = flipped;
        return this;
    }

    @Override
    public void paint(DrawContext context, int x, int y, int size) {
        int ox = x + (size - SHAPE_W) / 2;
        int oy = y + (size - SHAPE_H) / 2;
        int steps = SHAPE_H / STROKE;

        for (int i = 0; i < steps; i++) {
            int rowY = flipped ? oy + (steps - 1 - i) * STROKE : oy + i * STROKE;
            ScreenDrawing.coloredRect(context, ox + i * STROKE, rowY, STROKE, STROKE, COLOR);
            ScreenDrawing.coloredRect(context, ox + SHAPE_W - STROKE - i * STROKE, rowY, STROKE, STROKE, COLOR);
        }
    }
}
