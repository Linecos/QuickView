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

        // 内容卡 41 格（205px）：6 个功能行从 y=4 起、行距 33、行高 32，最低到 201px；
        // root 250 让底部留白 10px。行距从 40 收到 33 是为了第 6 行之后仍不超过 250px
        // （GUI Scale 5 下更高的面板会顶出屏幕）
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
                QuickViewKeybindings.getSaveKey()), 5, 4, 330, 32);
        panel.add(createFeatureRow("quickview.gui.settings.option.free_move",
                manager.isFreeMoveEnabled(), on -> manager.toggleFreeMove(),
                QuickViewKeybindings.getToggleMoveKey()), 5, 37, 330, 32);
        panel.add(createFeatureRow("quickview.gui.settings.option.freecam",
                manager.isPreferFreecam(), on -> manager.togglePreferFreecam(), null), 5, 70, 330, 32);
        panel.add(createFeatureRow("quickview.gui.settings.option.smooth_transition",
                manager.isSmoothTransitionEnabled(), on -> manager.toggleSmoothTransition(), null), 5, 103, 330, 32);
        panel.add(createFeatureRow("quickview.gui.settings.option.restore_on_damage",
                manager.isRestoreOnDamage(), on -> manager.toggleRestoreOnDamage(), null), 5, 136, 330, 32);
        panel.add(createFeatureRow("quickview.gui.settings.option.tweakeroo_freecam",
                        manager.isTweakerooFreecamSelected(), on -> manager.toggleUseTweakerooFreecam(), null,
                        manager.isTweakerooFreecamAvailable()), 5, 169, 330, 32);

        return panel;
    }

    private WGridPanel createFeatureRow(String key, boolean initialState, Consumer<Boolean> handler,
                                        KeyBinding descKeyBinding) {
        return createFeatureRow(key, initialState, handler, descKeyBinding, true);
    }

    /**
     * @param usable false = 该项当前不可用（例如没装 Tweakeroo）：开关置灰、名称与说明变暗，
     *               但仍占位显示，让用户知道有这个选项以及为什么不能用
     */
    private WGridPanel createFeatureRow(String key, boolean initialState, Consumer<Boolean> handler,
                                        KeyBinding descKeyBinding, boolean usable) {
        WGridPanel row = new WGridPanel(1);
        row.setBackgroundPainter(ROW_BG);

        WLabel nameLabel = new WLabel(Text.translatable(key), 0xFFFFFFFF)
                .setVerticalAlignment(VerticalAlignment.CENTER);
        row.add(nameLabel, 15, 2, 270, 15);

        // 说明里涉及快捷键的部分动态取当前绑定（改键后描述跟着变），不再写死 N/G
        Text desc = descKeyBinding != null
                ? Text.translatable(key + ".desc", descKeyBinding.getBoundKeyLocalizedText())
                : Text.translatable(key + ".desc");
        WLabel descLabel = new WLabel(desc, 0xFFAAAAAA)
                .setVerticalAlignment(VerticalAlignment.CENTER);
        row.add(descLabel, 15, 17, 270, 13);
        if (descKeyBinding != null) {
            // 按键参数创建时捕获（见 keyDescRefreshers 注释）：登记刷新器，改键后用新绑定重建文字
            keyDescRefreshers.add(() -> descLabel.setText(
                    Text.translatable(key + ".desc", descKeyBinding.getBoundKeyLocalizedText())));
        }

        // WToggleButton 没有 setEnabled：不可用时自己拦截点击并回滚视觉状态
        WToggleButton toggle = new WToggleButton()
                .setColor(usable ? 0xFFFFFFFF : 0xFF888888, usable ? 0xFFFFFFFF : 0xFF555555);
        toggle.setToggle(initialState);
        toggle.setOnToggle(value -> {
            if (usable) {
                handler.accept(value);
            } else {
                toggle.setToggle(initialState);
            }
        });
        if (!usable) {
            // 不可用的项：名称与说明一起变暗，避免看起来能点
            nameLabel.setColor(0xFF777777);
            descLabel.setColor(0xFF666666);
        }
        row.add(toggle, 295, 7, 18, 18);

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