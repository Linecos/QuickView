package dev.quickview.gui;

import dev.quickview.PinyinSearch;
import dev.quickview.QuickViewManager;
import dev.quickview.Viewpoint;
import io.github.cottonmc.cotton.gui.client.BackgroundPainter;
import io.github.cottonmc.cotton.gui.client.LightweightGuiDescription;
import io.github.cottonmc.cotton.gui.widget.WButton;
import io.github.cottonmc.cotton.gui.widget.WGridPanel;
import io.github.cottonmc.cotton.gui.widget.WToggleButton;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
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
    private final WButton settingsBtn = new WButton(Text.translatable("quickview.gui.main.settings"))
            .setOnClick(this::settingsCallback);

    private final ViewpointListPanel<Viewpoint, WButton> panel;
    private final WGridPanel root = new WGridPanel(5);
    private final QuickViewManager manager = QuickViewManager.getInstance();

    public ViewpointGUI() {
        manager.loadViewpoints();
        List<Viewpoint> data = new ArrayList<>(manager.getViewpoints());
        this.panel = new ViewpointListPanel<>(data, this::createEntry, this::configureEntry,
                this.search, vp -> PinyinSearch.keysOf(vp.getName()));
        this.setupRoot();
        this.setRootPanel(root);
        this.search.setChangedListener(s -> this.panel.applyFilter());
    }

    private WButton createEntry() {
        return new WButton(Text.literal(""));
    }

    private void configureEntry(Viewpoint vp, WButton btn) {
        btn.setLabel(Text.literal(vp.getName()));
        btn.setOnClick(() -> {
            if (editBtn.getToggle()) {
                openEditScreen(vp);
            } else if (deleteBtn.getToggle()) {
                openDeleteConfirm(vp);
            } else {
                manager.switchToViewpoint(vp);
                MinecraftClient.getInstance().setScreen(null);
            }
        });
    }

    private void setupRoot() {
        this.root.setSize(350, 240);
        this.root.add(this.search, 1, 1, 68, 2);
        this.root.add(this.panel, 1, 6, 68, 34);
        this.root.add(this.addBtn, 1, 41, 4, 4);
        this.root.add(this.editBtn, 8, 41, 8, 4);
        this.root.add(this.deleteBtn, 17, 41, 8, 4);
        this.root.add(this.restoreBtn, 26, 41, 12, 4);
        this.root.add(this.settingsBtn, 50, 41, 12, 4);
        this.root.validate(this);
    }

    private void addCallback() {
        Viewpoint vp = manager.createViewpoint("");
        if (vp == null) return;
        openEditScreen(vp);
    }

    /** 从磁盘重新读取列表并刷新面板（编辑/删除后统一走这里）。 */
    private void refreshList() {
        manager.loadViewpoints();
        panel.setData(new ArrayList<>(manager.getViewpoints()));
    }

    /** 打开书签编辑面板；关闭时保存改动并刷新列表。 */
    private void openEditScreen(Viewpoint vp) {
        int idx = manager.getViewpoints().indexOf(vp);
        ViewpointEditGUI editGui = new ViewpointEditGUI(vp, idx);
        WrapperViewpointScreen screen = new WrapperViewpointScreen(editGui);
        screen.setCloseCallback(() -> {
            editGui.saveData();
            refreshList();
        });
        screen.setParent(MinecraftClient.getInstance().currentScreen);
        MinecraftClient.getInstance().setScreen(screen);
    }

    /** 删除前先弹一次确认，避免「删除」开关打开时误点条目直接永久删除。 */
    private void openDeleteConfirm(Viewpoint vp) {
        Screen parent = MinecraftClient.getInstance().currentScreen;
        int idx = manager.getViewpoints().indexOf(vp);
        ConfirmGUI confirm = new ConfirmGUI(
                Text.translatable("quickview.gui.confirm.delete", shorten(vp.getName())),
                parent,
                () -> manager.removeViewpoint(idx));
        WrapperViewpointScreen screen = new WrapperViewpointScreen(confirm);
        screen.setParent(parent);
        screen.setCloseCallback(this::refreshList);
        MinecraftClient.getInstance().setScreen(screen);
    }

    /** 确认框一行放不下过长的书签名，超出部分用省略号截断。 */
    private static String shorten(String name) {
        if (name == null) return "";
        return name.length() <= 20 ? name : name.substring(0, 20) + "…";
    }

    private void restoreCallback() {
        manager.restore();
    }

    private void settingsCallback() {
        ViewpointSettingsGUI settingsGui = new ViewpointSettingsGUI();
        WrapperViewpointScreen screen = new WrapperViewpointScreen(settingsGui);
        screen.setParent(MinecraftClient.getInstance().currentScreen);
        MinecraftClient.getInstance().setScreen(screen);
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
