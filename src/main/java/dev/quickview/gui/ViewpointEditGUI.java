package dev.quickview.gui;

import dev.quickview.QuickViewManager;
import dev.quickview.Viewpoint;
import io.github.cottonmc.cotton.gui.client.BackgroundPainter;
import io.github.cottonmc.cotton.gui.client.LightweightGuiDescription;
import io.github.cottonmc.cotton.gui.widget.WGridPanel;
import io.github.cottonmc.cotton.gui.widget.WLabel;
import net.minecraft.text.Text;

public class ViewpointEditGUI extends LightweightGuiDescription {
    private final WGridPanel root = new WGridPanel(5);
    private final Viewpoint viewpoint;
    private final int viewpointIndex;
    private final WTextFieldExtra nameField = new WTextFieldExtra()
            .setSuggestion(Text.translatable("quickview.gui.edit.name"));
    private final WLabel coordsLabel;
    private final QuickViewManager manager = QuickViewManager.getInstance();

    public ViewpointEditGUI(Viewpoint viewpoint, int viewpointIndex) {
        this.viewpoint = viewpoint;
        this.viewpointIndex = viewpointIndex;
        this.coordsLabel = new WLabel(Text.literal(viewpoint.getFormattedCoords() + "  " + viewpoint.getFormattedRotation()));
        this.nameField.setText(viewpoint.getName());
        this.nameField.setFocusLostCallback(newName -> {
            if (!newName.trim().isEmpty()) {
                manager.renameViewpoint(viewpointIndex, newName.trim());
            }
        });
        this.setupRoot();
        this.setRootPanel(root);
    }

    private void setupRoot() {
        this.root.setSize(250, 120);
        this.root.add(this.nameField, 1, 1, 48, 4);
        this.root.add(this.coordsLabel, 1, 6, 48, 4);
        this.root.validate(this);
    }

    public void saveData() {
        manager.saveViewpoints();
    }

    @Override
    public void addPainters() {
        super.addPainters();
        this.rootPanel.setBackgroundPainter(BackgroundPainter.createColorful(0x4D000000));
    }
}
