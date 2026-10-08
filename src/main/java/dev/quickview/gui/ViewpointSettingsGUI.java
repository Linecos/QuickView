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

import java.util.List;
import java.util.function.Consumer;

public class ViewpointSettingsGUI extends LightweightGuiDescription {
    private static final BackgroundPainter ROW_BG = BackgroundPainter.createColorful(0x80000000);

    private final WGridPanel root = new WGridPanel(5);
    private final QuickViewManager manager = QuickViewManager.getInstance();
    private WButton tabGeneral;
    private WButton tabKeybinds;

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

        keyBtn.setOnChange(() -> resetBtn.setEnabled(!keyBtn.isAtDefault()));

        return row;
    }

    @Override
    public void addPainters() {
        super.addPainters();
        this.rootPanel.setBackgroundPainter(BackgroundPainter.createColorful(0x4D000000));
    }
}