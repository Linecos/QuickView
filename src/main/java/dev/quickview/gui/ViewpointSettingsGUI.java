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

        root.setSize(350, 240);
        root.add(tabGeneral, 1, 1, 14, 4);
        root.add(tabKeybinds, 17, 1, 14, 4);
        root.add(content, 1, 7, 68, 36);

        root.validate(this);
        setRootPanel(root);
    }

    private WGridPanel createFeaturePanel() {
        WGridPanel panel = new WGridPanel(1);

        panel.add(createFeatureRow("quickview.gui.settings.option.quick_add",
                manager.isQuickAddEnabled(), on -> manager.toggleQuickAdd()), 5, 10, 330, 24);
        panel.add(createFeatureRow("quickview.gui.settings.option.free_move",
                manager.isFreeMoveEnabled(), on -> manager.toggleFreeMove()), 5, 46, 330, 24);
        panel.add(createFeatureRow("quickview.gui.settings.option.freecam",
                manager.isPreferFreecam(), on -> manager.togglePreferFreecam()), 5, 82, 330, 24);
        panel.add(createFeatureRow("quickview.gui.settings.option.smooth_transition",
                manager.isSmoothTransitionEnabled(), on -> manager.toggleSmoothTransition()), 5, 118, 330, 24);

        return panel;
    }

    private WGridPanel createFeatureRow(String key, boolean initialState, Consumer<Boolean> handler) {
        WGridPanel row = new WGridPanel(1);
        row.setBackgroundPainter(ROW_BG);

        WLabel nameLabel = new WLabel(Text.translatable(key), 0xFFFFFFFF)
                .setVerticalAlignment(VerticalAlignment.CENTER);
        row.add(nameLabel, 15, 0, 270, 24);

        WToggleButton toggle = new WToggleButton()
                .setColor(0xFFFFFFFF, 0xFFFFFFFF)
                .setOnToggle(handler);
        toggle.setToggle(initialState);
        row.add(toggle, 295, 3, 18, 18);

        return row;
    }

    private WGridPanel createShortcutPanel() {
        WGridPanel panel = new WGridPanel(1);

        List<KeyBinding> bindings = QuickViewKeybindings.getAll();
        for (int i = 0; i < bindings.size(); i++) {
            panel.add(createKeybindingRow(bindings.get(i)), 5, 10 + i * 36, 330, 24);
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