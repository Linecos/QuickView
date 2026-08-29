package dev.quickview.gui;

import dev.quickview.QuickViewManager;
import dev.quickview.Viewpoint;
import io.github.cottonmc.cotton.gui.client.BackgroundPainter;
import io.github.cottonmc.cotton.gui.client.LightweightGuiDescription;
import io.github.cottonmc.cotton.gui.widget.WButton;
import io.github.cottonmc.cotton.gui.widget.WGridPanel;
import io.github.cottonmc.cotton.gui.widget.WLabel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.function.Predicate;

public class ViewpointEditGUI extends LightweightGuiDescription {
    private static final int COORD_BOX_W = 16;
    private static final int LEFT_LABEL_W = 3;
    private static final int RIGHT_LABEL_W = 7;

    private static final Predicate<String> NUMBER_PREDICATE = s -> {
        if (s.isEmpty()) return true;
        int dots = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '-') {
                if (i != 0) return false;
            } else if (c == '.') {
                if (++dots > 1) return false;
            } else if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    };

    private final WGridPanel root = new WGridPanel(5);
    private final Viewpoint viewpoint;
    private final int viewpointIndex;
    private final WTextFieldExtra nameField = new WTextFieldExtra()
            .setSuggestion(Text.translatable("quickview.gui.edit.name"));
    private final WTextFieldExtra xField;
    private final WTextFieldExtra yField;
    private final WTextFieldExtra zField;
    private final WTextFieldExtra yawField;
    private final WTextFieldExtra pitchField;
    private final WButton setCurrentBtn = new WButton(Text.translatable("quickview.gui.edit.set_current"))
            .setOnClick(this::setToCurrent);
    private final QuickViewManager manager = QuickViewManager.getInstance();

    public ViewpointEditGUI(Viewpoint viewpoint, int viewpointIndex) {
        this.viewpoint = viewpoint;
        this.viewpointIndex = viewpointIndex;
        this.nameField.setText(viewpoint.getName());
        this.nameField.setFocusLostCallback(s -> {
            manager.renameViewpoint(viewpointIndex, s);
            syncNameLength();
        });
        this.xField = coordField(String.format("%.1f", viewpoint.getX()));
        this.yField = coordField(String.format("%.1f", viewpoint.getY()));
        this.zField = coordField(String.format("%.1f", viewpoint.getZ()));
        this.yawField = coordField(String.format("%.1f", viewpoint.getYaw()));
        this.pitchField = coordField(String.format("%.1f", viewpoint.getPitch()));
        this.setupRoot();
        this.setRootPanel(root);
    }

    private void syncNameLength() {
        int len = nameField.getText().length() == 0 ? 1 : nameField.getText().length();
        nameField.setMaxLength(Math.max(nameField.getMaxLength(), len));
    }

    private WTextFieldExtra coordField(String initial) {
        WTextFieldExtra field = new WTextFieldExtra();
        field.setTextPredicate(NUMBER_PREDICATE);
        field.setText(initial);
        field.setFocusLostCallback(s -> applyCoordinates());
        return field;
    }

    private WLabel coordLabel(String text) {
        return new WLabel(Text.literal(text), 0xFFFFFFFF);
    }

    private void applyCoordinates() {
        try { viewpoint.setX(Double.parseDouble(xField.getText())); } catch (NumberFormatException ignored) {}
        try { viewpoint.setY(Double.parseDouble(yField.getText())); } catch (NumberFormatException ignored) {}
        try { viewpoint.setZ(Double.parseDouble(zField.getText())); } catch (NumberFormatException ignored) {}
        try { viewpoint.setYaw(Float.parseFloat(yawField.getText())); } catch (NumberFormatException ignored) {}
        try { viewpoint.setPitch(Float.parseFloat(pitchField.getText())); } catch (NumberFormatException ignored) {}
        manager.saveViewpoints();
    }

    private void setToCurrent() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        Viewpoint snapshot = manager.captureViewSnapshot("");
        if (snapshot == null) return;

        viewpoint.setX(snapshot.getX());
        viewpoint.setY(snapshot.getY());
        viewpoint.setZ(snapshot.getZ());
        viewpoint.setYaw(snapshot.getYaw());
        viewpoint.setPitch(snapshot.getPitch());

        xField.setText(String.format("%.1f", viewpoint.getX()));
        yField.setText(String.format("%.1f", viewpoint.getY()));
        zField.setText(String.format("%.1f", viewpoint.getZ()));
        yawField.setText(String.format("%.1f", viewpoint.getYaw()));
        pitchField.setText(String.format("%.1f", viewpoint.getPitch()));

        manager.saveViewpoints();
    }

    private void setupRoot() {
        this.root.setSize(250, 115);
        this.root.add(this.nameField, 1, 1, 48, 4);

        this.root.add(coordLabel("X:"), 1, 8, LEFT_LABEL_W, 2);
        this.root.add(this.xField, 5, 7, COORD_BOX_W, 2);
        this.root.add(coordLabel("Y:"), 1, 13, LEFT_LABEL_W, 2);
        this.root.add(this.yField, 5, 12, COORD_BOX_W, 2);
        this.root.add(coordLabel("Z:"), 1, 18, LEFT_LABEL_W, 2);
        this.root.add(this.zField, 5, 17, COORD_BOX_W, 2);

        this.root.add(coordLabel("Yaw:"), 25, 8, RIGHT_LABEL_W, 2);
        this.root.add(this.yawField, 33, 7, COORD_BOX_W, 2);
        this.root.add(coordLabel("Pitch:"), 25, 13, RIGHT_LABEL_W, 2);
        this.root.add(this.pitchField, 33, 12, COORD_BOX_W, 2);

        this.root.add(this.setCurrentBtn, 33, 17, COORD_BOX_W, 4);

        this.root.validate(this);
    }

    public void saveData() {
        manager.renameViewpoint(viewpointIndex, nameField.getText());
        applyCoordinates();
    }

    @Override
    public void addPainters() {
        super.addPainters();
        this.rootPanel.setBackgroundPainter(BackgroundPainter.createColorful(0x4D000000));
    }
}