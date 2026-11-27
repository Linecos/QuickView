package dev.quickview.gui;

import com.mojang.blaze3d.platform.Window;
import io.github.cottonmc.cotton.gui.widget.WGridPanel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * 下拉列表本体（1px 网格 = 子控件直接用像素坐标）。
 *
 * <p>宿主 root 在下拉展开期间会把传给子控件的鼠标坐标冻结成屏幕外值
 * （见 {@code ViewpointGUI} / {@code ViewpointEditGUI} 的 root paint 覆写），
 * 让下层控件的 hover 高亮全部熄灭；但下拉行自己也在 root 之下，会被一起冻掉。
 * 所以这里在绘制时从系统光标位置还原真实鼠标坐标，行悬停高亮才能正常工作。
 */
final class DropdownListPanel extends WGridPanel {
    DropdownListPanel() {
        super(1);
    }

    @Override
    public void paint(GuiGraphics context, int x, int y, int mouseX, int mouseY) {
        Minecraft client = Minecraft.getInstance();
        Window window = client.getWindow();
        double scale = window.getGuiScale();
        // Mouse.getX/getY 是窗口像素坐标，除以 GUI 缩放比得到 GUI 坐标；
        // 再减本面板绝对位置换算成本地坐标系（WPanel 派发约定）
        int realX = (int) (client.mouseHandler.xpos() / scale) - x;
        int realY = (int) (client.mouseHandler.ypos() / scale) - y;
        super.paint(context, x, y, realX, realY);
    }
}
