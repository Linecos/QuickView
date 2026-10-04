package dev.quickview.gui;

import io.github.cottonmc.cotton.gui.client.BackgroundPainter;
import io.github.cottonmc.cotton.gui.client.LightweightGuiDescription;
import io.github.cottonmc.cotton.gui.widget.WButton;
import io.github.cottonmc.cotton.gui.widget.WGridPanel;
import io.github.cottonmc.cotton.gui.widget.WLabel;
import io.github.cottonmc.cotton.gui.widget.data.VerticalAlignment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * 通用二次确认弹窗：点「确认」执行 {@code onConfirm} 后返回父界面，点「取消」直接返回。
 * 两个按钮都通过 {@code setScreen(parent)} 返回，因此父界面的 {@code removed()} 回调照常触发。
 */
public class ConfirmGUI extends LightweightGuiDescription {
    private final WGridPanel root = new WGridPanel(5);
    private final Screen parent;
    private final Runnable onConfirm;
    private final WButton confirmBtn;

    public ConfirmGUI(Text message, Screen parent, Runnable onConfirm) {
        this.parent = parent;
        this.onConfirm = onConfirm;
        root.setSize(220, 80);

        WLabel label = new WLabel(message, 0xFFFFFFFF)
                .setVerticalAlignment(VerticalAlignment.CENTER);
        root.add(label, 2, 2, 40, 6);

        WButton cancelBtn = new WButton(Text.translatable("quickview.gui.confirm.cancel"))
                .setOnClick(() -> MinecraftClient.getInstance().setScreen(parent));
        root.add(cancelBtn, 6, 11, 12, 4);

        confirmBtn = new WButton(Text.translatable("quickview.gui.confirm.ok"))
                .setOnClick(() -> {
                    if (onConfirm != null) {
                        onConfirm.run();
                    }
                    MinecraftClient.getInstance().setScreen(parent);
                });
        root.add(confirmBtn, 26, 11, 12, 4);

        root.validate(this);
        setRootPanel(root);
    }

    /** 自定义确认按钮文案（默认「确定」），例如删除场景传「删除」。 */
    public ConfirmGUI setConfirmLabel(Text label) {
        confirmBtn.setLabel(label);
        return this;
    }

    @Override
    public void addPainters() {
        super.addPainters();
        this.rootPanel.setBackgroundPainter(BackgroundPainter.createColorful(0x4D000000));
    }
}
