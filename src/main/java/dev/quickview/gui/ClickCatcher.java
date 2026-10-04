package dev.quickview.gui;

import io.github.cottonmc.cotton.gui.widget.WPanel;
import io.github.cottonmc.cotton.gui.widget.data.InputResult;
import net.minecraft.client.gui.Click;

/**
 * 展开下拉列表时铺满宿主面板的透明挡板：点列表以外的地方先收起列表，
 * 并且不把这次点击透传给下面的控件（下拉菜单的常见行为）。
 *
 * <p>只拦鼠标、不画东西。下层控件的 hover 高亮由宿主 GUI 在下拉展开期间
 * 用「冻结鼠标坐标」的方式整体关闭（见 {@code ViewpointGUI} / {@code ViewpointEditGUI}
 * 的 root paint 覆写），不在这里压暗。
 *
 * <p>依赖 LibGui 的绘制顺序 —— 后 add 的 child 画在上层、也先命中鼠标，
 * 所以宿主面板要先 add 挡板、再 add 列表本体。
 */
final class ClickCatcher extends WPanel {
    private final Runnable onOutsideClick;

    private ClickCatcher(Runnable onOutsideClick) {
        this.onOutsideClick = onOutsideClick;
    }

    static ClickCatcher closeOnOutsideClick(Runnable onOutsideClick) {
        return new ClickCatcher(onOutsideClick);
    }

    @Override
    public InputResult onMouseDown(Click click, boolean doubled) {
        onOutsideClick.run();
        return InputResult.PROCESSED;
    }
}
