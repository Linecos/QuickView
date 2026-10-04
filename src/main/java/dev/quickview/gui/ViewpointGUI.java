package dev.quickview.gui;

import dev.quickview.PinyinSearch;
import dev.quickview.QuickViewManager;
import dev.quickview.Viewpoint;
import io.github.cottonmc.cotton.gui.client.BackgroundPainter;
import io.github.cottonmc.cotton.gui.client.LightweightGuiDescription;
import io.github.cottonmc.cotton.gui.widget.WButton;
import io.github.cottonmc.cotton.gui.widget.WGridPanel;
import io.github.cottonmc.cotton.gui.widget.WLabel;
import io.github.cottonmc.cotton.gui.widget.WToggleButton;
import io.github.cottonmc.cotton.gui.widget.data.VerticalAlignment;
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
    private final WButton clearBtn = new WClearButton()
            .setOnClick(this::clearSearch);
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
    private final WButton settingsBtn = new WGearButton()
            .setOnClick(this::settingsCallback);
    /** 说明当前「模式」选中后点条目会发生什么；都没选中时不给文案，避免多一行无用的提示。 */
    private final WLabel modeHint = new WLabel(Text.literal(""), 0xFFAAAAAA)
            .setVerticalAlignment(VerticalAlignment.CENTER);

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
        this.clearBtn.setEnabled(false);
        this.refreshList();
        // 不在自由视角时「恢复视角」点了不会有任何反应，直接置灰
        this.restoreBtn.setEnabled(manager.isViewActive());
        this.updateModeHint();
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
        this.root.setSize(350, 250);

        // 第一行：搜索框 + 清空 + 分组筛选。分组按钮放这里而非底部行，是因为它的文字会随
        // 分组名变长，挤在按钮行里迟早溢出。
        // 清空按钮做成 4 格（20px）正方形，与右侧分组按钮各留 5px 间隙。
        this.root.add(this.search, 1, 1, 36, 4);
        this.root.add(this.clearBtn, 38, 1, 4, 4);
        this.root.add(this.groupBtn, 43, 1, 25, 4);

        this.root.add(this.panel, 1, 6, 68, 33);

        // 第二行：三个「模式开关」，彼此互斥，改变「点条目」的含义；右侧是随模式变化的说明文字
        this.root.add(this.editBtn, 1, 40, 11, 4);
        this.root.add(this.deleteBtn, 14, 40, 11, 4);
        this.root.add(this.sortBtn, 27, 40, 11, 4);
        this.root.add(this.modeHint, 39, 40, 30, 4);

        // 第三行：动作按钮，点了立即生效
        this.root.add(this.addBtn, 1, 45, 8, 4);
        this.root.add(this.restoreBtn, 10, 45, 54, 4);
        this.root.add(this.settingsBtn, 65, 45, 4, 4);

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
        Screen main = MinecraftClient.getInstance().currentScreen;
        screen.setCloseCallback(() -> {
            editGui.saveData();
            refreshList();
        });
        screen.setParent(main);
        editGui.setOnDeleteRequested(() -> {
            editGui.saveData();
            MinecraftClient.getInstance().setScreen(main);
            openDeleteConfirm(vp, main);
        });
        MinecraftClient.getInstance().setScreen(screen);
    }

    /** 删除前先弹一次确认，避免「删除」开关打开时误点条目直接永久删除。 */
    private void openDeleteConfirm(Viewpoint vp) {
        openDeleteConfirm(vp, MinecraftClient.getInstance().currentScreen);
    }

    private void openDeleteConfirm(Viewpoint vp, Screen parent) {
        int idx = manager.getViewpoints().indexOf(vp);
        ConfirmGUI confirm = new ConfirmGUI(
                Text.translatable("quickview.gui.confirm.delete", shorten(vp.getName())),
                parent,
                () -> manager.removeViewpoint(idx));
        confirm.setConfirmLabel(Text.translatable("quickview.gui.main.delete"));
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

    private void clearSearch() {
        search.setText("");
        panel.applyFilter();
        clearBtn.setEnabled(false);
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
        updateModeHint();
    }

    private void deleteBtnCallback(Boolean toggled) {
        if (toggled) {
            this.editBtn.setToggle(false);
            setSortMode(false);
        }
        updateModeHint();
    }

    private void sortBtnCallback(Boolean toggled) {
        if (toggled) {
            this.editBtn.setToggle(false);
            this.deleteBtn.setToggle(false);
        }
        setSortMode(toggled);
        updateModeHint();
    }

    /** 根据当前选中的模式，更新右侧的说明文字。 */
    private void updateModeHint() {
        Text hint;
        if (editBtn.getToggle()) {
            hint = Text.translatable("quickview.gui.main.modeHint.edit");
        } else if (deleteBtn.getToggle()) {
            hint = Text.translatable("quickview.gui.main.modeHint.delete");
        } else if (sortBtn.getToggle()) {
            hint = Text.translatable("quickview.gui.main.modeHint.sort");
        } else {
            // 没选模式时不提示（用户反馈这句话没必要）
            hint = Text.literal("");
        }
        modeHint.setText(hint);
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
