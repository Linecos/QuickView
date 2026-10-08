package dev.quickview.gui;

import dev.quickview.QuickViewManager;
import dev.quickview.Viewpoint;
import io.github.cottonmc.cotton.gui.client.BackgroundPainter;
import io.github.cottonmc.cotton.gui.client.LightweightGuiDescription;
import io.github.cottonmc.cotton.gui.widget.WButton;
import io.github.cottonmc.cotton.gui.widget.WGridPanel;
import io.github.cottonmc.cotton.gui.widget.WLabel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.List;
import java.util.function.Predicate;

public class ViewpointEditGUI extends LightweightGuiDescription {
    private static final int COORD_BOX_W = 16;
    private static final int LEFT_LABEL_W = 3;
    private static final int RIGHT_LABEL_W = 7;

    /** 下拉列表的宽度（像素）：与「分组输入框 + 下拉按钮」总宽一致（x 130 → 245）。 */
    private static final int LIST_W = 115;
    private static final int LIST_ROW_H = 20;
    /** 编辑面板高度有限，最多同时显示几项，再多就直接在手输框里打。 */
    private static final int LIST_MAX_ROWS = 4;
    private static final int LIST_X = 26;   // 格
    private static final int LIST_Y = 5;    // 格

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

    private final WGridPanel root = new WGridPanel(5) {
        @Override
        public void paint(DrawContext context, int x, int y, int mouseX, int mouseY) {
            // 分组下拉展开期间冻结全界面 hover（原理见 ViewpointGUI.root 注释）
            if (groupList != null) {
                super.paint(context, x, y, ViewpointGUI.HOVER_OFF_XY, ViewpointGUI.HOVER_OFF_XY);
                return;
            }
            super.paint(context, x, y, mouseX, mouseY);
        }
    };
    private final Viewpoint viewpoint;
    private final WTextFieldExtra nameField = new WTextFieldExtra()
            .setSuggestion(Text.translatable("quickview.gui.edit.name"));
    private final WTextFieldExtra groupField = new WTextFieldExtra()
            .setSuggestion(Text.translatable("quickview.gui.edit.group"));
    private final WTextFieldExtra xField;
    private final WTextFieldExtra yField;
    private final WTextFieldExtra zField;
    private final WTextFieldExtra yawField;
    private final WTextFieldExtra pitchField;
    private final WButton setCurrentBtn = new WButton(Text.translatable("quickview.gui.edit.set_current"))
            .setOnClick(this::setToCurrent);
    /** 下拉按钮上的箭头：按钮和箭头必须同一个实例，展开时才能翻成 ∧。 */
    private final ChevronIcon chevron = new ChevronIcon();
    private final WButton groupPickBtn = new WButton(chevron)
            .setOnClick(this::toggleGroupList);
    /** 删除按钮：红色垃圾桶图标（无独立文案，配合主界面的二次确认使用）。 */
    private final WButton deleteBtn = new WButton(new TrashIcon())
            .setOnClick(this::requestDelete);
    private final QuickViewManager manager = QuickViewManager.getInstance();

    /** 展开中的分组候选列表；null 表示收起。 */
    private WGridPanel groupList;
    /** 展开列表时铺满面板的透明挡板，点列表外先把列表收起。 */
    private ClickCatcher clickCatcher;
    /** 下拉处于「输入新分组名」状态时记住输入框，确认后要把内容写回。 */
    private WTextFieldExtra newGroupField;

    /** 由主界面注入：点「删除该书签」时回调（主界面负责先关编辑页、再弹确认）。 */
    private Runnable onDeleteRequested;

    public ViewpointEditGUI(Viewpoint viewpoint) {
        this.viewpoint = viewpoint;
        this.nameField.setText(viewpoint.getName());
        this.nameField.setFocusLostCallback(s -> {
            // 空名不覆盖（与 saveData 同一规则），并把保留的旧名回填进输入框，避免框里是空、模型里是旧名
            mutateAndMaybeSave(() -> {
                if (!s.isEmpty()) {
                    viewpoint.setName(s);
                }
            });
            if (s.isEmpty()) {
                nameField.setText(viewpoint.getName());
            }
            syncNameLength();
        });
        this.groupField.setText(viewpoint.getGroup());
        this.groupField.setFocusLostCallback(s -> mutateAndMaybeSave(() -> viewpoint.setGroup(s)));
        this.xField = coordField(String.format("%.1f", viewpoint.getX()));
        this.yField = coordField(String.format("%.1f", viewpoint.getY()));
        this.zField = coordField(String.format("%.1f", viewpoint.getZ()));
        this.yawField = coordField(String.format("%.1f", viewpoint.getYaw()));
        this.pitchField = coordField(String.format("%.1f", viewpoint.getPitch()));
        this.setupRoot();
        this.setRootPanel(root);
    }

    /** 注入删除回调（返回 this 便于链式调用）。 */
    public ViewpointEditGUI setOnDeleteRequested(Runnable onDeleteRequested) {
        this.onDeleteRequested = onDeleteRequested;
        return this;
    }

    private void requestDelete() {
        if (onDeleteRequested != null) {
            onDeleteRequested.run();
        }
    }

    /**
     * 「∨」下拉按钮：展开 / 收起分组候选列表。
     *
     * <p>列表内容：「＋ 新建分组…」（进入行内输入）、「（无分组）」、各已有分组。
     * 原来是点「已有」在已有分组里逐个循环，分组一多就得连点好几次才能翻到目标；
     * 改成一次展开、直接挑。
     */
    private void toggleGroupList() {
        if (groupList != null) {
            closeGroupList();
            return;
        }
        openGroupList(false);
    }

    /** @param inputMode true = 行内输入新分组名；false = 常规候选列表 */
    private void openGroupList(boolean inputMode) {
        List<String> groups = manager.getGroups();
        DropdownListPanel list = new DropdownListPanel();
        list.setBackgroundPainter(DropdownStyle.LIST_BG);

        // 行内容四周内缩，露出面板描边（见 DropdownStyle）
        int rowX = DropdownStyle.ROW_INSET;
        int rowW = LIST_W - DropdownStyle.ROW_INSET * 2;

        int rows;
        if (inputMode) {
            rows = 1;
            newGroupField = new WTextFieldExtra()
                    .setSuggestion(Text.translatable("quickview.gui.edit.group.new.hint"));
            WButton okBtn = new WButton(Text.translatable("quickview.gui.confirm.ok"))
                    .setOnClick(this::confirmNewGroup);
            list.add(newGroupField, rowX, DropdownStyle.ROW_TOP, rowW - 43, LIST_ROW_H);
            list.add(okBtn, rowX + rowW - 40, DropdownStyle.ROW_TOP, 40, LIST_ROW_H);
        } else {
            rows = Math.min(groups.size() + 2, LIST_MAX_ROWS);
            // 「新建分组」永远可用 —— 没有任何已有分组时它是唯一入口
            list.add(createGroupEntry(Text.translatable("quickview.gui.edit.group.new"), null),
                    rowX, DropdownStyle.ROW_TOP, rowW, LIST_ROW_H);
            list.add(createGroupEntry(Text.translatable("quickview.gui.edit.group.none"), ""),
                    rowX, DropdownStyle.ROW_TOP + LIST_ROW_H, rowW, LIST_ROW_H);
            for (int i = 0; i < rows - 2; i++) {
                String name = groups.get(i);
                list.add(createGroupEntry(Text.literal(name), name),
                        rowX, DropdownStyle.ROW_TOP + (i + 2) * LIST_ROW_H, rowW, LIST_ROW_H);
            }
        }
        // setHost 会递归设给已加入的子控件；必须在 requestFocus 之前调，否则焦点请求会被静默忽略
        list.setHost(this);
        if (inputMode && newGroupField != null) {
            newGroupField.requestFocus();
        }

        // 挡板先加（在列表下层），点列表以外的地方能被它先吃掉；列表后加，画在最上面
        this.clickCatcher = ClickCatcher.closeOnOutsideClick(this::closeGroupList);
        this.clickCatcher.setHost(this);
        this.groupList = list;
        this.root.add(this.clickCatcher, 0, 0, 50, 23);
        // +1 格（5px）是面板的垂直留白（上 2 + 下 3），见 DropdownStyle.VERTICAL_PADDING
        this.root.add(list, LIST_X, LIST_Y, LIST_W / 5, rows * LIST_ROW_H / 5 + 1);

        this.chevron.setFlipped(true);
    }

    /** 确认新建分组：名字非空才生效；与已有分组重名时等同于选中该分组。 */
    private void confirmNewGroup() {
        String name = newGroupField != null ? newGroupField.getText().trim() : "";
        if (!name.isEmpty()) {
            groupField.setText(name);
            viewpoint.setGroup(name);
        }
        closeGroupList();
    }

    private void closeGroupList() {
        if (this.groupList != null) {
            this.root.remove(this.groupList);
            this.groupList = null;
        }
        if (this.clickCatcher != null) {
            this.root.remove(this.clickCatcher);
            this.clickCatcher = null;
        }
        this.newGroupField = null;
        this.chevron.setFlipped(false);
    }

    /** value 为 null 表示「新建分组」入口，点击后切换到行内输入。 */
    private WButton createGroupEntry(Text label, String value) {
        return new WButton(label).setOnClick(() -> {
            if (value == null) {
                closeGroupList();
                openGroupList(true);
                return;
            }
            groupField.setText(value);
            viewpoint.setGroup(value);
            closeGroupList();
        });
    }

    private void syncNameLength() {
        int len = nameField.getText().length() == 0 ? 1 : nameField.getText().length();
        nameField.setMaxLength(Math.max(nameField.getMaxLength(), len));
    }

    private WTextFieldExtra coordField(String initial) {
        WTextFieldExtra field = new WTextFieldExtra();
        field.setTextPredicate(NUMBER_PREDICATE);
        field.setText(initial);
        field.setFocusLostCallback(s -> {
            mutateAndMaybeSave(this::applyCoordinates);
            // 回填：输入为空或只有 "-" / "." 这类半截内容时 parse 会失败，若不回填，
            // 输入框显示的内容会和模型里的真实值不一致。
            refreshFields();
        });
        return field;
    }

    private WLabel coordLabel(String text) {
        return new WLabel(Text.literal(text), 0xFFFFFFFF);
    }

    /** 用模型中的真实值刷新全部坐标/朝向输入框。 */
    private void refreshFields() {
        xField.setText(String.format("%.1f", viewpoint.getX()));
        yField.setText(String.format("%.1f", viewpoint.getY()));
        zField.setText(String.format("%.1f", viewpoint.getZ()));
        yawField.setText(String.format("%.1f", viewpoint.getYaw()));
        pitchField.setText(String.format("%.1f", viewpoint.getPitch()));
    }

    /**
     * 把输入框内容写回模型（不落盘；解析失败的字段保持原值）。
     * <p>落盘由 {@link #mutateAndMaybeSave} 在调用处负责判断。
     */
    private void applyCoordinates() {
        try { viewpoint.setX(Double.parseDouble(xField.getText())); } catch (NumberFormatException ignored) {}
        try { viewpoint.setY(Double.parseDouble(yField.getText())); } catch (NumberFormatException ignored) {}
        try { viewpoint.setZ(Double.parseDouble(zField.getText())); } catch (NumberFormatException ignored) {}
        try { viewpoint.setYaw(Float.parseFloat(yawField.getText())); } catch (NumberFormatException ignored) {}
        try { viewpoint.setPitch(Float.parseFloat(pitchField.getText())); } catch (NumberFormatException ignored) {}
    }

    /** 模型当前值的快照，用于判断一次修改是否真的改了东西。 */
    private String snapshot() {
        return viewpoint.getName() + '\u0000' + viewpoint.getGroup()
                + '\u0000' + viewpoint.getX() + '\u0000' + viewpoint.getY() + '\u0000' + viewpoint.getZ()
                + '\u0000' + viewpoint.getYaw() + '\u0000' + viewpoint.getPitch();
    }

    /**
     * 执行一次修改，<b>只有真的改了值才落盘</b>。
     *
     * <p>为什么不一律在关闭时统一落盘：那样崩溃 / 强杀（不走 {@code Screen#removed()}）会丢掉编辑内容。
     * 为什么不每次失焦都无条件落盘：5 个坐标框逐个失焦会白写 5 次盘。
     * 折中成「变了才写」——既没有丢改动的窗口，也没有无意义的写盘。
     */
    private void mutateAndMaybeSave(Runnable mutation) {
        String before = snapshot();
        mutation.run();
        if (!snapshot().equals(before)) {
            manager.saveViewpoints();
        }
    }

    private void setToCurrent() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        Viewpoint snapshot = manager.captureViewSnapshot("");
        if (snapshot == null) return;

        mutateAndMaybeSave(() -> {
            viewpoint.setX(snapshot.getX());
            viewpoint.setY(snapshot.getY());
            viewpoint.setZ(snapshot.getZ());
            viewpoint.setYaw(snapshot.getYaw());
            viewpoint.setPitch(snapshot.getPitch());
        });

        refreshFields();
    }

    private void setupRoot() {
        // 高度收窄到 115：内容到 y=21 格（105px）为止，下拉 4 行（25 + 4×20 + 3 = 108px）也放得下
        this.root.setSize(250, 115);
        this.root.add(this.nameField, 1, 1, 23, 4);
        this.root.add(this.groupField, 26, 1, 18, 4);
        this.root.add(this.groupPickBtn, 45, 1, 4, 4);

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

        // 「设为当前」向左扩到与 Yaw/Pitch 列对齐（x=25），加宽后与垃圾桶按钮一起填满右栏
        this.root.add(this.setCurrentBtn, 25, 17, 19, 4);
        this.root.add(this.deleteBtn, 45, 17, 4, 4);

        this.root.validate(this);
    }

    /** 关闭编辑页时调用：把全部字段写回模型并统一落盘一次。 */
    public void saveData() {
        // 名字为空时不覆盖旧名：用户可能只是随手清了输入框，不该把书签变成无名
        String name = nameField.getText();
        if (!name.isEmpty()) {
            viewpoint.setName(name);
        }
        applyCoordinates();
        manager.saveViewpoints();
    }

    @Override
    public void addPainters() {
        super.addPainters();
        this.rootPanel.setBackgroundPainter(BackgroundPainter.createColorful(0x4D000000));
    }
}