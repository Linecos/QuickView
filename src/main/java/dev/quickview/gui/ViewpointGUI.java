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
    private final WToggleButton sortBtn = new WToggleButton(Text.translatable("quickview.gui.main.sort"))
            .setColor(0xFFFFFFFF, 0xFFFFFFFF)
            .setOnToggle(this::sortBtnCallback);
    private final WButton groupBtn = new WButton(Text.translatable("quickview.gui.main.groupAll"))
            .setOnClick(this::cycleGroupFilter);
    private final WButton settingsBtn = new WButton(Text.translatable("quickview.gui.main.settings"))
            .setOnClick(this::settingsCallback);

    private final ViewpointListPanel<Viewpoint> panel;
    private final WGridPanel root = new WGridPanel(5);
    private final QuickViewManager manager = QuickViewManager.getInstance();

    /** 当前分组筛选，空串表示「全部」。 */
    private String groupFilter = "";

    public ViewpointGUI() {
        manager.loadViewpoints();
        this.panel = new ViewpointListPanel<>(new ArrayList<>(), this::createEntry, this::configureEntry,
                this.search, vp -> PinyinSearch.keysOf(vp.getName()));
        this.panel.setOnReorder(manager::reorderVisible);
        this.setupRoot();
        this.setRootPanel(root);
        this.search.setChangedListener(s -> this.panel.applyFilter());
        this.refreshList();
    }

    private WViewpointEntry createEntry() {
        return new WViewpointEntry();
    }

    private void configureEntry(Viewpoint vp, WViewpointEntry btn) {
        String group = vp.getGroup();
        btn.setLabel(group.isEmpty()
                ? Text.literal(vp.getName())
                : Text.literal("[" + group + "] " + vp.getName()));
        btn.setOnClick(() -> {
            if (sortBtn.getToggle()) {
                // 排序模式下条目只用于拖拽，点一下不做任何事（避免误切换视角）
                return;
            }
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
        this.root.add(this.editBtn, 9, 41, 8, 4);
        this.root.add(this.deleteBtn, 18, 41, 8, 4);
        this.root.add(this.restoreBtn, 27, 41, 11, 4);
        this.root.add(this.sortBtn, 39, 41, 7, 4);
        this.root.add(this.groupBtn, 47, 41, 12, 4);
        this.root.add(this.settingsBtn, 60, 41, 9, 4);
        this.root.validate(this);
    }

    private void addCallback() {
        Viewpoint vp = manager.createViewpoint("");
        if (vp == null) return;
        // 正在按分组筛选时，新建的书签直接归入当前分组，否则它会被筛掉、看起来像没建成功
        if (!groupFilter.isEmpty()) {
            vp.setGroup(groupFilter);
            manager.saveViewpoints();
        }
        openEditScreen(vp);
    }

    /** 从磁盘重新读取列表，并按当前分组筛选刷新面板（编辑/删除后统一走这里）。 */
    private void refreshList() {
        manager.loadViewpoints();
        applyGroupFilter();
    }

    private void applyGroupFilter() {
        List<Viewpoint> all = manager.getViewpoints();
        List<String> groups = groupsOf(all);
        if (!groupFilter.isEmpty() && !groups.contains(groupFilter)) {
            // 该分组已被改名或删空，退回「全部」
            groupFilter = "";
        }

        List<Viewpoint> visible = new ArrayList<>();
        for (Viewpoint vp : all) {
            if (groupFilter.isEmpty() || groupFilter.equals(vp.getGroup())) {
                visible.add(vp);
            }
        }
        panel.setData(visible);
        updateGroupButton(groups);
    }

    private void cycleGroupFilter() {
        List<String> groups = groupsOf(manager.getViewpoints());
        if (groups.isEmpty()) {
            groupFilter = "";
        } else if (groupFilter.isEmpty()) {
            groupFilter = groups.get(0);
        } else {
            int next = groups.indexOf(groupFilter) + 1;
            groupFilter = next >= groups.size() ? "" : groups.get(next);
        }
        applyGroupFilter();
    }

    private void updateGroupButton(List<String> groups) {
        groupBtn.setLabel(groupFilter.isEmpty()
                ? Text.translatable("quickview.gui.main.groupAll")
                : Text.translatable("quickview.gui.main.groupName", groupFilter));
        groupBtn.setEnabled(!groups.isEmpty());
    }

    /** 按出现顺序收集所有非空分组名。 */
    private static List<String> groupsOf(List<Viewpoint> list) {
        List<String> groups = new ArrayList<>();
        for (Viewpoint vp : list) {
            String group = vp.getGroup();
            if (!group.isEmpty() && !groups.contains(group)) {
                groups.add(group);
            }
        }
        return groups;
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
            setSortMode(false);
        }
    }

    private void deleteBtnCallback(Boolean toggled) {
        if (toggled) {
            this.editBtn.setToggle(false);
            setSortMode(false);
        }
    }

    private void sortBtnCallback(Boolean toggled) {
        if (toggled) {
            this.editBtn.setToggle(false);
            this.deleteBtn.setToggle(false);
        }
        setSortMode(toggled);
    }

    /** 排序模式与编辑/删除开关互斥：同一时刻只有一种「点条目的含义」。 */
    private void setSortMode(boolean enabled) {
        if (sortBtn.getToggle() != enabled) {
            sortBtn.setToggle(enabled);
        }
        panel.setSortMode(enabled);
    }

    @Override
    public void addPainters() {
        super.addPainters();
        this.rootPanel.setBackgroundPainter(BackgroundPainter.createColorful(0x4D000000));
    }
}
