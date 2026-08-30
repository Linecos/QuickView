package dev.quickview.gui;

import dev.quickview.QuickViewKeybindings;
import dev.quickview.QuickViewManager;
import io.github.cottonmc.cotton.gui.client.BackgroundPainter;
import io.github.cottonmc.cotton.gui.client.LightweightGuiDescription;
import io.github.cottonmc.cotton.gui.widget.WGridPanel;
import io.github.cottonmc.cotton.gui.widget.WTabPanel;
import io.github.cottonmc.cotton.gui.widget.WToggleButton;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;

import java.util.List;
import java.util.function.Consumer;

public class ViewpointSettingsGUI extends LightweightGuiDescription {
    private static final BackgroundPainter CARD_BG = BackgroundPainter.createColorful(0xD0101010);

    private final WGridPanel root = new WGridPanel(5);
    private final QuickViewManager manager = QuickViewManager.getInstance();

    public ViewpointSettingsGUI() {
        WTabPanel tabs = new WTabPanel();

        tabs.add(createFeaturePanel(), tab -> tab.title(Text.translatable("quickview.gui.settings.tab.general")));
        tabs.add(createShortcutPanel(), tab -> tab.title(Text.translatable("quickview.gui.settings.tab.keybinds")));

        root.setSize(350, 200);
        root.add(tabs, 0, 0, 70, 40);
        root.validate(this);
        setRootPanel(root);
    }

    private WGridPanel createFeaturePanel() {
        WGridPanel panel = new WGridPanel(5);
        panel.setSize(350, 170);
        panel.setBackgroundPainter(CARD_BG);

        panel.add(toggle("quickview.gui.settings.option.quick_add", manager.isQuickAddEnabled(), on -> manager.toggleQuickAdd()), 2, 2, 60, 5);
        panel.add(toggle("quickview.gui.settings.option.free_move", manager.isFreeMoveEnabled(), on -> manager.toggleFreeMove()), 2, 13, 60, 5);
        panel.add(toggle("quickview.gui.settings.option.freecam", manager.isPreferFreecam(), on -> manager.togglePreferFreecam()), 2, 24, 60, 5);

        return panel;
    }

    private WGridPanel createShortcutPanel() {
        WGridPanel panel = new WGridPanel(5);
        panel.setSize(350, 170);
        panel.setBackgroundPainter(CARD_BG);

        List<KeyBinding> bindings = QuickViewKeybindings.getAll();
        for (int i = 0; i < bindings.size(); i++) {
            panel.add(new WKeyBindingButton(bindings.get(i)), 2, 1 + i * 6, 66, 5);
        }

        return panel;
    }

    private WToggleButton toggle(String translationKey, boolean initialState, Consumer<Boolean> handler) {
        WToggleButton button = new WToggleButton(Text.translatable(translationKey))
                .setColor(0xFFFFFFFF, 0xFFFFFFFF)
                .setOnToggle(handler);
        button.setToggle(initialState);
        return button;
    }

    @Override
    public void addPainters() {
        super.addPainters();
        this.rootPanel.setBackgroundPainter(BackgroundPainter.createColorful(0x4D000000));
    }
}