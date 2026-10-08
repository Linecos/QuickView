package dev.quickview.gui;

import dev.quickview.mixin.IWListPanel;
import dev.quickview.mixin.IWWidget;
import io.github.cottonmc.cotton.gui.client.ScreenDrawing;
import io.github.cottonmc.cotton.gui.widget.WListPanel;
import io.github.cottonmc.cotton.gui.widget.WTextField;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 书签列表：网格布局 + 搜索过滤 + 拖拽排序。
 *
 * <p>拖拽只在「排序模式」开启时生效（由主界面的排序按钮控制）。拖拽期间：
 * 源条目变暗、一个半透明「幽灵」跟着光标走、落点格子显示预览框；<b>列表顺序在松手时才真正改变</b>。
 *
 * <p>它重写了 {@link WListPanel#layout()}（依赖 4 个 Accessor mixin 暴露的内部字段），
 * 升级 LibGui 前务必先看 PROJECT_MEMORY §13 的说明。
 */
public class ViewpointListPanel<D> extends WListPanel<D, WViewpointEntry> implements WViewpointEntry.DragHost {
    private static final int ROW_HEIGHT = 22;
    private static final int ENTRY_HEIGHT = 20;
    private static final int COLS = 4;
    private static final int COL_GAP = 5;

    /** 落点预览框。 */
    private static final int DROP_FILL = 0x40FFD166;
    private static final int DROP_BORDER = 0xFFFFD166;
    /** 跟随光标的幽灵条目。 */
    private static final int GHOST_FILL = 0xE0101010;
    private static final int GHOST_BORDER = 0xFFFFD166;
    private static final int GHOST_TEXT = 0xFFFFFFFF;
    /** 批量删除勾选态：浅红包裹 + 红边框（与排序落点预览同构，颜色换成危险红）。 */
    private static final int SELECT_FILL = 0x40FF5555;
    private static final int SELECT_BORDER = 0xFFFF5555;

    private final WTextField search;
    private final List<D> allData;
    /** {@code data[i]} 在 {@link #allData} 中的下标，与 {@code data} 同步重建。 */
    private final List<Integer> visibleSlots = new ArrayList<>();
    /**
     * 从元素取出「参与搜索的全部文本」。显式传入，避免依赖 toString() 造成的隐式耦合；
     * 返回多个键是为了支持拼音（原文 / 全拼 / 声母任一命中即算匹配）。
     */
    private final Function<D, List<String>> searchKeys;

    /** 拖拽结束后回调，参数是面板当前（已按新顺序排好）的完整数据。 */
    private Consumer<List<D>> onReorder;

    private boolean sortMode;
    private int cellWidth;

    /** 批量删除的选择模式：开启时点条目 = 勾选/取消勾选，而非触发点击动作。 */
    private boolean selectMode;
    /**
     * 已勾选的数据。用<b>身份</b>集合而不是 HashSet：勾选/拖拽/排序三处都依赖「同一批对象实例」的语义，
     * 显式身份集合把这条约束写死在类型里 —— 将来给 Viewpoint 覆写 equals 也不会悄悄改变行为。
     */
    private final java.util.Set<D> selected =
            java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
    /** 勾选集合变化时回调（主界面据此更新「删除已选 (N)」按钮）。 */
    private Runnable onSelectionChanged;

    private D draggingData;
    private String draggingLabel = "";
    /** 落点格子下标（0..data.size()-1），-1 表示无有效落点。 */
    private int dropIndex = -1;
    private int dragPanelX;
    private int dragPanelY;

    public ViewpointListPanel(List<D> data, Supplier<WViewpointEntry> supplier,
                              BiConsumer<D, WViewpointEntry> configurator,
                              WTextField search, Function<D, List<String>> searchKeys) {
        super(new ArrayList<>(data), supplier, configurator);
        this.allData = data;
        this.search = search;
        this.searchKeys = searchKeys;
        scrollBar.setScrollingSpeed(8);
    }

    public void setOnReorder(Consumer<List<D>> onReorder) {
        this.onReorder = onReorder;
    }

    public void setOnSelectionChanged(Runnable onSelectionChanged) {
        this.onSelectionChanged = onSelectionChanged;
    }

    /** 开关选择（批量删除）模式；关闭时清空勾选。 */
    public void setSelectMode(boolean selectMode) {
        this.selectMode = selectMode;
        if (!selectMode) {
            clearSelection();
        }
    }

    /** 勾选集合（身份集合的拷贝）。 */
    public List<D> getSelected() {
        return new ArrayList<>(selected);
    }

    /** 勾选数量：省掉 {@link #getSelected()} 的整表拷贝（主界面刷新按钮文案时只关心数量）。 */
    public int getSelectedCount() {
        return selected.size();
    }

    /**
     * 批量删除执行后清空勾选（主界面在删除成功后调用）。
     * <p>空集合时直接返回：省掉一次无意义的 layout 与回调（{@code setData} 每次都走这里）。
     */
    public void clearSelection() {
        if (selected.isEmpty()) {
            return;
        }
        selected.clear();
        if (onSelectionChanged != null) {
            onSelectionChanged.run();
        }
        layout();
    }

    /** 点条目时切换勾选（由主界面在删除模式下调用）。 */
    public void toggleSelected(D d) {
        if (selected.contains(d)) {
            selected.remove(d);
        } else {
            selected.add(d);
        }
        if (onSelectionChanged != null) {
            onSelectionChanged.run();
        }
        layout();
    }

    /** 开关排序模式；关闭时立刻结束进行中的拖拽状态。 */
    public void setSortMode(boolean sortMode) {
        this.sortMode = sortMode;
        if (!sortMode) {
            clearDragState();
        }
    }

    public void setData(List<D> newData) {
        this.allData.clear();
        this.allData.addAll(newData);
        clearDragState();
        clearSelection();
        applyFilter();
    }

    /** 搜索框内容变化时调用：重建可见列表并重置滚动。 */
    public void applyFilter() {
        rebuildData();
        this.configured.clear();
        this.scrollBar.setValue(0);
        this.layout();
    }

    /**
     * 只按当前搜索词重建可见列表，<b>不清理 {@code configured}</b>。
     * <p>拖拽过程中必须走这条路：一旦清空 configured，正在被拖的那个 widget 会被丢弃重建，
     * 拖拽事件就再也送不到它身上了。
     */
    private void rebuildData() {
        this.data.clear();
        this.visibleSlots.clear();

        String query = (search == null || search.getText() == null)
                ? ""
                : search.getText().trim().toLowerCase(Locale.ROOT);
        for (int i = 0; i < allData.size(); i++) {
            D d = allData.get(i);
            if (query.isEmpty() || matches(d, query)) {
                this.data.add(d);
                this.visibleSlots.add(i);
            }
        }
    }

    /** 任一搜索键包含 query 即算命中。 */
    private boolean matches(D d, String query) {
        List<String> keys = searchKeys == null ? null : searchKeys.apply(d);
        if (keys == null) {
            return false;
        }
        for (String key : keys) {
            if (key != null && key.contains(query)) {
                return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------------- 拖拽排序

    @Override
    public boolean isDragEnabled() {
        return sortMode;
    }

    @Override
    public void entryDragged(WViewpointEntry entry, int panelX, int panelY) {
        if (!sortMode) {
            return;
        }
        if (draggingData == null) {
            draggingData = findDataOf(entry);
            if (draggingData == null) {
                return;
            }
            draggingLabel = entry.getLabel() == null ? "" : entry.getLabel().getString();
            entry.setDragSource(true);
        }
        dragPanelX = panelX;
        dragPanelY = panelY;
        dropIndex = dropIndexAt(panelX, panelY);
    }

    @Override
    public void entryDropped(WViewpointEntry entry) {
        entry.setDragSource(false);

        // 必须先把被拖的元素取到局部变量再清状态：clearDragState() 会把 draggingData 置空，
        // 若在那之后还拿它去构造新顺序，插进列表的就是 null —— 被拖的元素凭空消失、
        // 新顺序里少一个元素，回填校验便会失败并静默放弃排序（拖了没反应）。
        D moved = draggingData;
        if (moved == null) {
            clearDragState();
            return;
        }

        int from = data.indexOf(moved);
        int to = dropIndex;
        clearDragState();

        if (from < 0 || to < 0 || to == from) {
            layout();
            return;
        }

        // 「落进所指格子」：移除后在同样的下标插入，元素就会停在光标指的那一格
        List<D> visibleOrder = new ArrayList<>(data);
        visibleOrder.remove(from);
        visibleOrder.add(to, moved);
        applyVisibleOrder(visibleOrder);

        rebuildData();
        layout();

        if (onReorder != null) {
            onReorder.accept(new ArrayList<>(allData));
        }
    }

    private void clearDragState() {
        draggingData = null;
        draggingLabel = "";
        dropIndex = -1;
    }

    /**
     * 把可见条目按新顺序写回 {@code allData}：按 {@link #visibleSlots} 记录的槽位逐个填回，
     * 于是被搜索/分组过滤掉的条目位置保持不变。
     * <p>
     * 刻意<b>不</b>用「遍历 allData、看元素是否在 visibleOrder 里」那种写法：它依赖对象身份
     * （{@code Viewpoint} 没有覆写 {@code equals}），一旦两侧因任何原因不是同一批实例就会静默放弃排序。
     * 用下标映射则与对象身份无关，且和 {@code QuickViewManager.reorderVisible} 的语义一致。
     */
    private void applyVisibleOrder(List<D> visibleOrder) {
        if (visibleOrder.size() != visibleSlots.size()) {
            // 理论上不会发生（visibleOrder 就是 data 的重排）；真发生了就放弃，避免写坏列表
            return;
        }
        for (int i = 0; i < visibleOrder.size(); i++) {
            allData.set(visibleSlots.get(i), visibleOrder.get(i));
        }
    }

    /** 找出某个条目 widget 对应的数据（widget 是配置时按数据配上去的，用身份比较）。 */
    private D findDataOf(WViewpointEntry entry) {
        for (var e : configured.entrySet()) {
            if (e.getValue() == entry) {
                return e.getKey();
            }
        }
        return null;
    }

    /**
     * 把面板坐标换算成「落点格子下标」（0..size-1）。
     * <p>语义是「落进光标所指的那一格」，而不是「插到那一格之前」。后者看着合理，但往右挪一格时
     * 插入点换算回来恰好等于原位，会被判成"没移动"，导致往右拖永远无效、预览也停在自己那格。
     * 拖到最后一格右侧的空位时夹到最后一格。
     */
    private int dropIndexAt(int panelX, int panelY) {
        if (data.isEmpty()) {
            return -1;
        }
        int cell = indexAtCell(panelX, panelY);
        if (cell < 0) {
            return -1;
        }
        return Math.max(0, Math.min(cell, data.size() - 1));
    }

    /** 面板坐标 → 格子下标（可能超出 data.size()-1，由调用方决定怎么夹）。 */
    private int indexAtCell(int panelX, int panelY) {
        if (cellWidth <= 0) {
            return -1;
        }
        // layout() 里 y = row * ROW_HEIGHT - scrollPixels + 1
        int contentY = panelY + scrollBar.getValue() - 1;
        int row = contentY < 0 ? 0 : contentY / ROW_HEIGHT;
        int col = panelX / (cellWidth + COL_GAP);
        if (col < 0) {
            col = 0;
        } else if (col >= COLS) {
            col = COLS - 1;
        }
        return row * COLS + col;
    }

    // -------------------------------------------------------------------- 绘制

    @Override
    public void paint(DrawContext context, int x, int y, int mouseX, int mouseY) {
        // 指针在面板外时冻结鼠标坐标（HOVER_OFF_XY 同 ViewpointGUI，屏幕外值让 hover 判定为 false）：
        // 滚动列表的末行会超出面板下缘，仍留在 children 里，不冻结的话指针在面板下方
        // （模式开关行那一带）也会让它亮起来 —— 点不到但会亮，像 bug。
        boolean inside = mouseX >= 0 && mouseY >= 0 && mouseX < this.width && mouseY < this.height;
        int px = inside ? mouseX : ViewpointGUI.HOVER_OFF_XY;
        int py = inside ? mouseY : ViewpointGUI.HOVER_OFF_XY;

        // 裁剪到面板范围：条目、勾选框、落点预览、拖拽幽灵都可能超出面板（LibGui 不裁剪），
        // 不裁剪就会盖到下面的模式开关行上
        context.enableScissor(x, y, x + this.width, y + this.height);
        super.paint(context, x, y, px, py);

        // 批量删除勾选态：给已勾选条目画浅红包裹 + 红边框（与排序落点预览同构）
        if (selectMode && !selected.isEmpty() && cellWidth > 0) {
            for (int i = 0; i < data.size(); i++) {
                if (!selected.contains(data.get(i))) {
                    continue;
                }
                int row = i / COLS;
                int col = i % COLS;
                int cellX = x + col * (cellWidth + COL_GAP);
                int cellY = y + row * ROW_HEIGHT - scrollBar.getValue() + 1;
                if (cellY + ENTRY_HEIGHT < y || cellY > y + this.height) {
                    continue;
                }
                ScreenDrawing.coloredRect(context, cellX, cellY, cellWidth, ENTRY_HEIGHT, SELECT_FILL);
                ScreenDrawing.coloredRect(context, cellX, cellY, cellWidth, 1, SELECT_BORDER);
                ScreenDrawing.coloredRect(context, cellX, cellY + ENTRY_HEIGHT - 1, cellWidth, 1, SELECT_BORDER);
                ScreenDrawing.coloredRect(context, cellX, cellY, 1, ENTRY_HEIGHT, SELECT_BORDER);
                ScreenDrawing.coloredRect(context, cellX + cellWidth - 1, cellY, 1, ENTRY_HEIGHT, SELECT_BORDER);
            }
        }

        if (sortMode && draggingData != null && cellWidth > 0) {
            // 1) 落点预览：高亮光标所指的那一格（也就是松手后条目会停的位置）
            int preview = dropIndex;
            if (preview >= 0 && !data.isEmpty()) {
                preview = Math.max(0, Math.min(preview, data.size() - 1));
                int row = preview / COLS;
                int col = preview % COLS;
                int cellX = x + col * (cellWidth + COL_GAP);
                int cellY = y + row * ROW_HEIGHT - scrollBar.getValue() + 1;
                ScreenDrawing.coloredRect(context, cellX, cellY, cellWidth, ENTRY_HEIGHT, DROP_FILL);
                ScreenDrawing.coloredRect(context, cellX, cellY, cellWidth, 1, DROP_BORDER);
                ScreenDrawing.coloredRect(context, cellX, cellY + ENTRY_HEIGHT - 1, cellWidth, 1, DROP_BORDER);
                ScreenDrawing.coloredRect(context, cellX, cellY, 1, ENTRY_HEIGHT, DROP_BORDER);
                ScreenDrawing.coloredRect(context, cellX + cellWidth - 1, cellY, 1, ENTRY_HEIGHT, DROP_BORDER);
            }

            // 2) 幽灵条目：跟着光标走。位置夹在面板范围内，避免被屏幕边缘裁掉
            int ghostX = x + dragPanelX - cellWidth / 2;
            int ghostY = y + dragPanelY - ENTRY_HEIGHT / 2;
            ghostX = Math.max(x, Math.min(ghostX, x + this.width - cellWidth));
            ghostY = Math.max(y, Math.min(ghostY, y + this.height - ENTRY_HEIGHT));

            ScreenDrawing.coloredRect(context, ghostX - 1, ghostY - 1, cellWidth + 2, ENTRY_HEIGHT + 2, GHOST_BORDER);
            ScreenDrawing.coloredRect(context, ghostX, ghostY, cellWidth, ENTRY_HEIGHT, GHOST_FILL);
            ScreenDrawing.drawString(context, fit(draggingLabel, cellWidth - 8), ghostX + 4, ghostY + 6, GHOST_TEXT);
        }

        context.disableScissor();
    }

    /** 幽灵条目宽度有限，按实际字体宽度裁一下，免得文字溢出框外。 */
    private static String fit(String text, int maxWidth) {
        if (text == null || text.isEmpty() || maxWidth <= 0) {
            return "";
        }
        var textRenderer = MinecraftClient.getInstance().textRenderer;
        if (textRenderer.getWidth(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "…";
        return textRenderer.trimToWidth(text, Math.max(0, maxWidth - textRenderer.getWidth(ellipsis))) + ellipsis;
    }

    // -------------------------------------------------------------------- 布局

    @Override
    public void layout() {
        this.children.clear();
        this.children.add(scrollBar);
        scrollBar.setLocation(this.width - scrollBar.getWidth(), 0);
        scrollBar.setSize(8, this.height);

        if (!fixedHeight) {
            if (unconfigured.isEmpty()) {
                if (configured.isEmpty()) {
                    WViewpointEntry exemplar = createChild();
                    unconfigured.add(exemplar);
                    if (!exemplar.canResize()) cellHeight = exemplar.getHeight();
                } else {
                    WViewpointEntry exemplar = configured.values().iterator().next();
                    if (!exemplar.canResize()) cellHeight = exemplar.getHeight();
                }
            } else {
                WViewpointEntry exemplar = unconfigured.get(0);
                if (!exemplar.canResize()) cellHeight = exemplar.getHeight();
            }
        }
        if (cellHeight < 4) cellHeight = 4;

        int totalRows = (int) Math.ceil((double) data.size() / COLS);
        int contentHeight = totalRows * ROW_HEIGHT;

        scrollBar.setWindow(this.height);
        scrollBar.setMaxValue(Math.max(contentHeight, this.height));

        int scrollPixels = scrollBar.getValue();
        cellWidth = (this.width - scrollBar.getWidth() - COL_GAP * (COLS - 1)) / COLS;

        for (int i = 0; i < data.size(); i++) {
            int row = i / COLS;
            int col = i % COLS;

            int y = row * ROW_HEIGHT - scrollPixels;
            if (y + ROW_HEIGHT < 0 || y > this.height) continue;

            D d = data.get(i);
            WViewpointEntry w = configured.get(d);
            if (w == null) {
                if (unconfigured.isEmpty()) {
                    w = createChild();
                } else {
                    w = unconfigured.remove(0);
                }
                configurator.accept(d, w);
                configured.put(d, w);
            }
            w.setDragHost(this);

            int x = col * (cellWidth + COL_GAP);
            w.setSize(cellWidth, ENTRY_HEIGHT);
            ((IWWidget) w).setX(x);
            ((IWWidget) w).setY(y + 1);
            w.setSelected(selected.contains(d));

            this.children.add(w);
        }
    }

    @SuppressWarnings("unchecked")
    private WViewpointEntry createChild() {
        return ((IWListPanel<WViewpointEntry>) this).invokeCreateChild();
    }
}
