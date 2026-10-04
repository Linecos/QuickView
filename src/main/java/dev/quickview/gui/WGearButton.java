package dev.quickview.gui;

import io.github.cottonmc.cotton.gui.client.ScreenDrawing;
import io.github.cottonmc.cotton.gui.widget.WButton;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/**
 * 设置入口的齿轮图标按钮：正方形，用若干实心矩形拼出一个像素风齿轮。
 *
 * <p>不用字符（{@code ⚙} U+2699 依赖 unicode 字体回退，未必有字形），也不用纹理
 * （得额外打包一张图），改成 {@link ScreenDrawing#coloredRect} 画 10 个矩形块 +
 * 中心一个深色孔，任何 GUI Scale 下都不会糊。
 */
public class WGearButton extends WButton {
    /** 齿盘主色：暖白（与 GUI 文本同色系，不是纯白，避免在暗色按钮上发灰）。 */
    private static final int GEAR_COLOR = 0xFFE8E6DF;
    /** 中心孔颜色：比按钮底纹更暗，看得出是个「孔」。 */
    private static final int HOLE_COLOR = 0xFF3A3A38;
    /**
     * 齿轮外接正方形的边长（像素，必须是奇数）。
     *
     * <p>按钮本身 20×20，边长 13 时几乎顶满底纹、看着比旁边的文字按钮重（用户截图反馈），
     * 因此收到 11。要再调大小只改这一个数，下面的偏移全部由它推导。
     */
    private static final int GEAR_SIZE = 11;

    public WGearButton() {
        super(Text.literal(""));
    }

    @Override
    public void paint(DrawContext context, int x, int y, int mouseX, int mouseY) {
        super.paint(context, x, y, mouseX, mouseY);

        int cx = x + getWidth() / 2;
        int cy = y + getHeight() / 2;
        int r = GEAR_SIZE / 2;      // 11 -> 5，齿轮的「半径」
        int disc = r - 2;           // 圆盘半径，比外接框小 2px，剩下 2px 留给齿

        // 十字形圆盘（两条对称的矩形相交）
        fill(context, cx - disc, cy - disc + 1, disc * 2 + 1, disc * 2 - 1);
        fill(context, cx - disc + 1, cy - disc, disc * 2 - 1, disc * 2 + 1);

        // 四个正向齿（3×3）
        fill(context, cx - 1, cy - r, 3, 3);
        fill(context, cx - 1, cy + r - 2, 3, 3);
        fill(context, cx - r, cy - 1, 3, 3);
        fill(context, cx + r - 2, cy - 1, 3, 3);

        // 四个对角齿（2×2，填掉圆盘和正向齿之间的缺口）
        fill(context, cx - r + 1, cy - r + 1, 2, 2);
        fill(context, cx + r - 2, cy - r + 1, 2, 2);
        fill(context, cx - r + 1, cy + r - 2, 2, 2);
        fill(context, cx + r - 2, cy + r - 2, 2, 2);

        // 中心孔
        ScreenDrawing.coloredRect(context, cx - 1, cy - 1, 3, 3, HOLE_COLOR);
    }

    private static void fill(DrawContext context, int x, int y, int w, int h) {
        ScreenDrawing.coloredRect(context, x, y, w, h, GEAR_COLOR);
    }
}
