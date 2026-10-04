package dev.quickview.gui;

import io.github.cottonmc.cotton.gui.widget.WButton;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

/**
 * 搜索框右侧的「清空」按钮（20×20 正方形）。
 *
 * <p>直接给 {@link WButton} 传 {@code ×} 标签时，叉号看起来不在几何中心（用户截图反馈），原因有两个：
 * <ul>
 *   <li>{@code WButton} 用 {@code ScreenDrawing.drawStringWithShadow} 画标签，恒定带 1px 阴影，
 *       GUI Scale 放大后整体偏右下</li>
 *   <li>按钮原来是 15px 宽的长条，横向留白左右不等</li>
 * </ul>
 * 所以这里用空标签（{@code super.paint} 只画底纹），再自己按字体度量把字形画在正中间、不带阴影。
 */
public class WClearButton extends WButton {
    /** 叉号字形；用字体自带字形而不是像素方块，边缘更干净。 */
    private static final Text GLYPH = Text.translatable("quickview.gui.main.clear");

    private static final int ENABLED_COLOR = 0xFFFFFFFF;
    /** 与 {@code WButton} 的标签禁用色保持一致。 */
    private static final int DISABLED_COLOR = 0xFFA0A0A0;
    /** 字体行高（与 {@code WButton} 的标签定位同一常量）。 */
    private static final int FONT_HEIGHT = 8;

    public WClearButton() {
        super(Text.literal(""));
    }

    @Override
    public void paint(DrawContext context, int x, int y, int mouseX, int mouseY) {
        super.paint(context, x, y, mouseX, mouseY);

        var textRenderer = MinecraftClient.getInstance().textRenderer;
        OrderedText glyph = GLYPH.asOrderedText();
        int width = textRenderer.getWidth(glyph);
        int color = isEnabled() ? ENABLED_COLOR : DISABLED_COLOR;

        context.drawText(textRenderer, glyph,
                x + (getWidth() - width) / 2,
                y + (getHeight() - FONT_HEIGHT) / 2,
                color, false);
    }
}
