package dev.quickview.gui;

import dev.quickview.QuickViewKeybindings;
import dev.quickview.QuickViewManager;
import io.github.cottonmc.cotton.gui.client.BackgroundPainter;
import io.github.cottonmc.cotton.gui.client.LightweightGuiDescription;
import io.github.cottonmc.cotton.gui.widget.WButton;
import io.github.cottonmc.cotton.gui.widget.WCardPanel;
import io.github.cottonmc.cotton.gui.widget.WGridPanel;
import io.github.cottonmc.cotton.gui.widget.WLabel;
import io.github.cottonmc.cotton.gui.widget.WToggleButton;
import io.github.cottonmc.cotton.gui.widget.data.HorizontalAlignment;
import io.github.cottonmc.cotton.gui.widget.data.VerticalAlignment;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ViewpointSettingsGUI extends LightweightGuiDescription {
    private static final BackgroundPainter ROW_BG = BackgroundPainter.createColorful(0x80000000);

    private final WGridPanel root = new WGridPanel(5);
    private final QuickViewManager manager = QuickViewManager.getInstance();
    private WButton tabGeneral;
    private WButton tabKeybinds;
    /**
     * 功能说明里涉及快捷键的标签刷新器（改键后要立即重设文字）。
     * ⚠️ {@code Text.translatable(key.desc, getBoundKeyLocalizedText())} 的按键参数在<b>创建标签时</b>
     * 按当时的 boundKey 求值并捕获；改键只换 {@code KeyBinding.boundKey}，已捕获的旧 Text 不会跟着变 ——
     * 所以必须保留刷新器，改键后用新绑定重建说明文字，否则显示旧键名直到重进设置页。
     */
    private final List<Runnable> keyDescRefreshers = new ArrayList<>();

    public ViewpointSettingsGUI() {
        WGridPanel featurePanel = createFeaturePanel();
        WGridPanel shortcutPanel = createShortcutPanel();

        WCardPanel content = new WCardPanel();
        content.add(featurePanel);
        content.add(shortcutPanel);
        content.setSelectedIndex(0);

        tabGeneral = new WButton(Text.translatable("quickview.gui.settings.tab.general"))
                .setOnClick(() -> {
                    content.setSelectedIndex(0);
                    tabGeneral.setEnabled(false);
                    tabKeybinds.setEnabled(true);
                });
        tabKeybinds = new WButton(Text.translatable("quickview.gui.settings.tab.keybinds"))
                .setOnClick(() -> {
                    content.setSelectedIndex(1);
                    tabKeybinds.setEnabled(false);
                    tabGeneral.setEnabled(true);
                });
        tabGeneral.setEnabled(false);

        // 内容卡 41 格（205px）：5 个功能行从 y=4 起、行距 40、最低到 200px；root 250 让底部留白 10px。
        // 行距从 44 收到 40 是为了在第 5 行之后仍不超过 250px（GUI Scale 5 下更高的面板会顶出屏幕）
        root.setSize(350, 250);
        root.add(tabGeneral, 1, 1, 14, 4);
        root.add(tabKeybinds, 17, 1, 14, 4);
        root.add(content, 1, 7, 68, 41);

        root.validate(this);
        setRootPanel(root);
    }

    private WGridPanel createFeaturePanel() {
        WGridPanel panel = new WGridPanel(1);

        panel.add(createFeatureRow("quickview.gui.settings.option.quick_add",
                manager.isQuickAddEnabled(), on -> manager.toggleQuickAdd(),
                QuickViewKeybindings.getSaveKey()), 5, 4, 330, 36);
        panel.add(createFeatureRow("quickview.gui.settings.option.free_move",
                manager.isFreeMoveEnabled(), on -> manager.toggleFreeMove(),
                QuickViewKeybindings.getToggleMoveKey()), 5, 44, 330, 36);
        panel.add(createFeatureRow("quickview.gui.settings.option.freecam",
                manager.isPreferFreecam(), on -> manager.togglePreferFreecam(), null), 5, 84, 330, 36);
        panel.add(createFeatureRow("quickview.gui.settings.option.smooth_transition",
                manager.isSmoothTransitionEnabled(), on -> manager.toggleSmoothTransition(), null), 5, 124, 330, 36);
        panel.add(createFeatureRow("quickview.gui.settings.option.restore_on_damage",
                manager.isRestoreOnDamage(), on -> manager.toggleRestoreOnDamage(), null), 5, 164, 330, 36);

        return panel;
    }

    private WGridPanel createFeatureRow(String key, boolean initialState, Consumer<Boolean> handler,
                                        KeyBinding descKeyBinding) {
        WGridPanel row = new WGridPanel(1);
        row.setBackgroundPainter(ROW_BG);

        WLabel nameLabel = new WLabel(Text.translatable(key), 0xFFFFFFFF)
                .setVerticalAlignment(VerticalAlignment.CENTER);
        row.add(nameLabel, 15, 2, 270, 16);

        // 说明里涉及快捷键的部分动态取当前绑定（改键后描述跟着变），不再写死 N/G
        Text desc = descKeyBinding != null
                ? Text.translatable(key + ".desc", descKeyBinding.getBoundKeyLocalizedText())
                : Text.translatable(key + ".desc");
        WLabel descLabel = new WLabel(desc, 0xFFAAAAAA)
                .setVerticalAlignment(VerticalAlignment.CENTER);
        row.add(descLabel, 15, 19, 270, 14);
        if (descKeyBinding != null) {
            // 按键参数创建时捕获（见 keyDescRefreshers 注释）：登记刷新器，改键后用新绑定重建文字
            keyDescRefreshers.add(() -> descLabel.setText(
                    Text.translatable(key + ".desc", descKeyBinding.getBoundKeyLocalizedText())));
        }

        WToggleButton toggle = new WToggleButton()
                .setColor(0xFFFFFFFF, 0xFFFFFFFF)
                .setOnToggle(handler);
        toggle.setToggle(initialState);
        row.add(toggle, 295, 9, 18, 18);

        return row;
    }

    private WGridPanel createShortcutPanel() {
        WGridPanel panel = new WGridPanel(1);

        List<KeyBinding> bindings = QuickViewKeybindings.getAll();
        for (int i = 0; i < bindings.size(); i++) {
            panel.add(createKeybindingRow(bindings.get(i)), 5, 10 + i * 32, 330, 24);
        }

        return panel;
    }

    private WGridPanel createKeybindingRow(KeyBinding kb) {
        WGridPanel row = new WGridPanel(1);
        row.setBackgroundPainter(ROW_BG);

        int defaultKey = QuickViewKeybindings.getDefaultKey(kb);

        WLabel nameLabel = new WLabel(Text.translatable(kb.getId()), 0xFFFFFFFF)
                .setHorizontalAlignment(HorizontalAlignment.LEFT)
                .setVerticalAlignment(VerticalAlignment.CENTER);
        row.add(nameLabel, 15, 0, 180, 24);

        WKeyBindingButton keyBtn = new WKeyBindingButton(kb);
        row.add(keyBtn, 220, 2, 72, 20);

        WButton resetBtn = new WButton(Text.translatable("quickview.gui.settings.key.reset"))
                .setAlignment(HorizontalAlignment.CENTER)
                .setOnClick(() -> keyBtn.resetToDefault(defaultKey));
        resetBtn.setEnabled(!keyBtn.isAtDefault());
        row.add(resetBtn, 292, 2, 28, 20);

        keyBtn.setOnChange(() -> {
            resetBtn.setEnabled(!keyBtn.isAtDefault());
            // 功能 tab 说明里的快捷键也跟着立即刷新（改键 / 重置 / ESC 解绑都会走 onChange）
            refreshKeyDescriptions();
        });

        return row;
    }

    /** 快捷键 tab 改键后立即刷新功能 tab 里涉及快捷键的说明文字（不用重进设置页）。 */
    private void refreshKeyDescriptions() {
        for (Runnable refresher : keyDescRefreshers) {
            refresher.run();
        }
    }

    @Override
    public void addPainters() {
        super.addPainters();
        this.rootPanel.setBackgroundPainter(BackgroundPainter.createColorful(0x4D000000));
    }
}