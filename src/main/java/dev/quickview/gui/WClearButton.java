package dev.quickview.gui;

import io.github.cottonmc.cotton.gui.widget.WButton;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

/**
 * 搜索框**内部**右端的「清空」图标（不是独立按钮）。
 *
 * <p>原先是一个 20×20 的方块按钮放在搜索框右边，用户反馈直觉上会以为它关闭页面。
 * 现在改成叠在搜索框内的透明图标：不画任何按钮底纹，只把 {@code ×} 字形画在正中
 * （悬停时加一层极淡的白色底作为反馈），并且 {@link #canFocus()} 返回 false —— 点它不抢输入框焦点。
 */
public class WClearButton extends WButton {
    /** × 字形；用字体自带字形而不是像素方块，边缘更干净。 */
    private static final Text GLYPH = Text.translatable("quickview.gui.main.clear");

    /** 悬停时字形变纯白。 */
    private static final int HOVER_COLOR = 0xFFFFFFFF;
    /** 平时略暗的白：看着像输入框内的图标，不给底色（黑色输入框上任何底色都会显得像灰方块）。 */
    private static final int ENABLED_COLOR = 0xFFC0C0C0;
    /** 在黑色输入框底上，暗灰即表达「不可用」。 */
    private static final int DISABLED_COLOR = 0xFF707070;
    /** 字体行高（与 {@code WButton} 的标签定位同一常量）。 */
    private static final int FONT_HEIGHT = 8;

    public WClearButton() {
        super(Text.literal(""));
    }

    /** 不参与焦点：点 × 后焦点仍留在搜索框里，可以继续输入。 */
    @Override
    public boolean canFocus() {
        return false;
    }

    @Override
    public void paint(DrawContext context, int x, int y, int mouseX, int mouseY) {
        // 刻意不调 super.paint，也不画悬停底色 —— 只画字形，让它看起来就是输入框的一部分。
        // 悬停反馈用「字形变亮」表达（画底色在黑色输入框上会明显得像一块灰方块，用户反馈要不得）。
        var textRenderer = MinecraftClient.getInstance().textRenderer;
        OrderedText glyph = GLYPH.asOrderedText();
        int width = textRenderer.getWidth(glyph);
        int color;
        if (!isEnabled()) {
            color = DISABLED_COLOR;
        } else if (isWithinBounds(mouseX, mouseY)) {
            color = HOVER_COLOR;
        } else {
            color = ENABLED_COLOR;
        }

        context.drawText(textRenderer, glyph,
                x + (getWidth() - width) / 2,
                y + (getHeight() - FONT_HEIGHT) / 2,
                color, false);
    }
}
