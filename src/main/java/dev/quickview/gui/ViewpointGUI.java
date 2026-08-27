package dev.quickview.gui;

import dev.quickview.QuickViewManager;
import dev.quickview.Viewpoint;
import io.github.cottonmc.cotton.gui.client.BackgroundPainter;
import io.github.cottonmc.cotton.gui.client.LightweightGuiDescription;
import io.github.cottonmc.cotton.gui.widget.WButton;
import io.github.cottonmc.cotton.gui.widget.WGridPanel;
import io.github.cottonmc.cotton.gui.widget.WToggleButton;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class ViewpointGUI extends LightweightGuiDescription {
    private final WTextFieldExtra search = new WTextFieldExtra()
            .setSuggestion(Text.translatable("quickview.gui.main.search"));
    private final WButton addBtn = new WButton(Text.translatable("quickview.gui.main.add"))
            .setOnClick(this::addCallback);
    private final WToggleButton editBtn = new WToggleButton(Text.translatable("quickview.gui.main.edit"))
            .setColor(0xFFFFFFFF, 0xFFFFFFFF)
            .setOnToggle(this::editBtnCallback);
    private final WToggleButton deleteBtn = new WToggleButton(Text.translatable("quickview.gui.main.delete"))
            .setColor(0xFFFFFFFF, 0xFFFFFFFF)
            .setOnToggle(this::deleteBtnCallback);
    private final WButton restoreBtn = new WButton(Text.translatable("quickview.gui.main.restore"))
            .setOnClick(this::restoreCallback);

    private final ViewpointListPanel<Viewpoint, WButton> panel;
    private final WGridPanel root = new WGridPanel(5);
    private final QuickViewManager manager = QuickViewManager.getInstance();

    public ViewpointGUI() {
        manager.loadViewpoints();
        List<Viewpoint> data = new ArrayList<>(manager.getViewpoints());
        this.panel = new ViewpointListPanel<>(data, this::createEntry, this::configureEntry, this.search);
        this.setupRoot();
        this.setRootPanel(root);
        this.search.setChangedListener(s -> this.panel.layout());
    }

    private WButton createEntry() {
        WButton btn = new WButton(Text.literal(""));
        btn.setSize(78, 20);
        return btn;
    }

    private void configureEntry(Viewpoint vp, WButton btn) {
        btn.setLabel(Text.literal(vp.getName()));
        btn.setOnClick(() -> {
            if (editBtn.getToggle()) {
                ViewpointEditGUI editGui = new ViewpointEditGUI(vp, manager.getViewpoints().indexOf(vp));
                WrapperViewpointScreen screen = new WrapperViewpointScreen(editGui);
                screen.setCloseCallback(editGui::saveData);
                screen.setReturnAction(this.panel::layout);
                screen.setParent(MinecraftClient.getInstance().currentScreen);
                MinecraftClient.getInstance().setScreen(screen);
            } else if (deleteBtn.getToggle()) {
                int idx = manager.getViewpoints().indexOf(vp);
                manager.removeViewpoint(idx);
                manager.loadViewpoints();
                panel.setData(new ArrayList<>(manager.getViewpoints()));
                panel.layout();
            } else {
                manager.switchToViewpoint(vp);
            }
        });
    }

    private void setupRoot() {
        this.root.setSize(350, 240);
        this.root.add(this.search, 1, 1, 68, 2);
        this.root.add(this.panel, 1, 6, 68, 34);
        this.root.add(this.addBtn, 1, 41, 4, 4);
        this.root.add(this.editBtn, 6, 41, 4, 4);
        this.root.add(this.deleteBtn, 11, 41, 4, 4);
        this.root.add(this.restoreBtn, 30, 41, 10, 4);
        this.root.validate(this);
    }

    private void addCallback() {
        int idx = manager.getViewpoints().size() + 1;
        manager.addViewpoint("Bookmark " + idx);
        manager.loadViewpoints();
        panel.setData(new ArrayList<>(manager.getViewpoints()));
        panel.layout();
    }

    private void restoreCallback() {
        manager.restore();
    }

    private void editBtnCallback(Boolean toggled) {
        if (toggled) {
            this.deleteBtn.setToggle(false);
        }
    }

    private void deleteBtnCallback(Boolean toggled) {
        if (toggled) {
            this.editBtn.setToggle(false);
        }
    }

    @Override
    public void addPainters() {
        super.addPainters();
        this.rootPanel.setBackgroundPainter(BackgroundPainter.createColorful(0x4D000000));
    }
}
