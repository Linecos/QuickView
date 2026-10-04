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
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class ViewpointGUI extends LightweightGuiDescription {
    /** 分组下拉列表的几何：贴左上角（分组按钮正下方），宽度与按钮一致。 */
    private static final int GROUP_LIST_X = 1;    // 格
    private static final int GROUP_LIST_W = 60;   // px，= 12 格
    private static final int GROUP_ROW_H = 20;    // px
    /** 主面板高 250px，列表从 y≈30px 起，最多再放下 8 行。 */
    private static final int GROUP_MAX_ROWS = 8;

    private final WTextFieldExtra search = new WTextFieldExtra()
            .setSuggestion(Text.translatable("quickview.gui.main.search"))
            .setMaxLength(64);
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
            .setOnClick(this::toggleGroupList);
    private final WButton settingsBtn = new WGearButton()
            .setOnClick(this::settingsCallback);
    /** 说明当前「模式」选中后点条目会发生什么；都没选中时不给文案，避免多一行无用的提示。 */
    private final WLabel modeHint = new WLabel(Text.literal(""), 0xFFAAAAAA)
            .setVerticalAlignment(VerticalAlignment.CENTER);

    private final ViewpointListPanel<Viewpoint> panel;
    /**
     * 下拉展开期间冻结全界面 hover：LibGui 遮挡不阻止 paint，下层控件会照常按鼠标位置
     * 自绘 hover 高亮，看起来像能点（实际被挡板拦截）。这里把传给子控件的鼠标坐标换成
     * 屏幕外的固定值，所有 hover 判定自然为 false。不用 Integer.MIN_VALUE 是为了避免
     * paint 派发里 mouseX - childX 的减法溢出回绕成正数。
     */
    static final int HOVER_OFF_XY = -1_000_000;

    private final WGridPanel root = new WGridPanel(5) {
        @Override
        public void paint(DrawContext context, int x, int y, int mouseX, int mouseY) {
            if (groupListPanel != null) {
                super.paint(context, x, y, HOVER_OFF_XY, HOVER_OFF_XY);
                return;
            }
            super.paint(context, x, y, mouseX, mouseY);
        }
    };
    private final QuickViewManager manager = QuickViewManager.getInstance();

    /** 当前分组筛选，空串表示「全部」。 */
    private String groupFilter = "";
    /** 展开中的分组筛选列表；null 表示收起。 */
    private WGridPanel groupListPanel;
    /** 展开列表时铺满面板的透明挡板，点列表外先把列表收起。 */
    private ClickCatcher groupCatcher;

    public ViewpointGUI() {
        manager.loadViewpoints();
        this.panel = new ViewpointListPanel<>(new ArrayList<>(), this::createEntry, this::configureEntry,
                this.search, vp -> PinyinSearch.keysOf(vp.getName()));
        this.panel.setOnReorder(manager::reorderVisible);
        this.setupRoot();
        this.setRootPanel(root);
        this.search.setChangedListener(s -> {
            this.panel.applyFilter();
            // 清空按钮只在有输入时可用（漏了这步它就永远是暗的、点不动）
            this.clearBtn.setEnabled(!s.isEmpty());
        });
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

        // 第一行：左上角是分组切换（按钮本身点开下拉，不需要单独的 ∨ 按钮），
        // 右侧是搜索框 + 清空。分组块收窄到 12 格（60px）—— 一般分组名不会太长，
        // 宽按钮反而抢视觉；省出的空间全给搜索框（拼音输入更长更好打）。
        this.root.add(this.groupBtn, 1, 1, 12, 4);
        this.root.add(this.search, 14, 1, 50, 4);
        this.root.add(this.clearBtn, 65, 1, 4, 4);

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

    /**
     * 分组筛选的下拉选择（编辑/删除/排序之外的第二处下拉，行为与编辑页一致）：
     * 「全部」+ 各已有分组，选中即筛选。循环切换在分组一多时要连点很多下，弃用。
     */
    private void toggleGroupList() {
        if (groupListPanel != null) {
            closeGroupList();
            return;
        }
        List<String> groups = groupsOf(manager.getViewpoints());
        int rows = Math.min(groups.size() + 1, GROUP_MAX_ROWS);

        DropdownListPanel list = new DropdownListPanel();
        list.setBackgroundPainter(DropdownStyle.LIST_BG);

        // 面板背景与按钮外框同为 60px（一致）；行内缩 1px 让左右 1px 描边露出来，
        // 否则行铺满会盖住描边，只剩上下框，看起来像背景宽度没跟上行。
        int rowX = 1;
        int rowW = GROUP_LIST_W - 2;
        list.add(createGroupFilterEntry(Text.translatable("quickview.gui.main.groupAll"), "",
                        groupFilter.isEmpty()),
                rowX, DropdownStyle.ROW_TOP, rowW, GROUP_ROW_H);
        for (int i = 0; i < rows - 1; i++) {
            String name = groups.get(i);
            list.add(createGroupFilterEntry(Text.literal(name), name, groupFilter.equals(name)),
                    rowX, DropdownStyle.ROW_TOP + (i + 1) * GROUP_ROW_H, rowW, GROUP_ROW_H);
        }
        list.setHost(this);

        // 挡板先加（在列表下层）；列表后加，画在最上面
        this.groupCatcher = ClickCatcher.closeOnOutsideClick(this::closeGroupList);
        this.groupCatcher.setHost(this);
        this.groupListPanel = list;
        this.root.add(this.groupCatcher, 0, 0, 70, 50);
        // +1 格（5px）是面板的垂直留白（上 2 + 下 3），见 DropdownStyle.VERTICAL_PADDING
        this.root.add(list, GROUP_LIST_X, 6, GROUP_LIST_W / 5, rows * GROUP_ROW_H / 5 + 1);
    }

    private void closeGroupList() {
        if (this.groupListPanel != null) {
            this.root.remove(this.groupListPanel);
            this.groupListPanel = null;
        }
        if (this.groupCatcher != null) {
            this.root.remove(this.groupCatcher);
            this.groupCatcher = null;
        }
    }

    /** value 为空串表示「全部」。当前已选中的行置灰（暗态），点击其余行切换筛选。 */
    private WButton createGroupFilterEntry(Text label, String value, boolean selected) {
        WButton btn = new WButton(label).setOnClick(() -> {
            groupFilter = value;
            applyGroupFilter();
            closeGroupList();
        });
        btn.setEnabled(!selected);
        return btn;
    }

    private void updateGroupButton(List<String> groups) {
        // 按钮文字精简：无筛选显示「全部」，有筛选只显示分组名（不带「分组：」前缀）
        groupBtn.setLabel(groupFilter.isEmpty()
                ? Text.translatable("quickview.gui.main.groupAll")
                : Text.literal(groupFilter));
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
        // 空名书签显示「未命名」，避免出现「确定删除「」吗？」
        Text nameArg = vp.getName().isEmpty()
                ? Text.translatable("quickview.gui.edit.unnamed")
                : Text.literal(shorten(vp.getName()));
        ConfirmGUI confirm = new ConfirmGUI(
                Text.translatable("quickview.gui.confirm.delete", nameArg),
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
        // 与点书签的行为对称：完成切换就退出菜单 —— 恢复后玩家的下一步是回到游戏，
        // 菜单挡在前面没有意义；也顺带避免「恢复视角」按钮的亮暗状态滞留
        MinecraftClient.getInstance().setScreen(null);
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
