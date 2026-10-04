package dev.quickview.gui;

import dev.quickview.mixin.IWListPanel;
import dev.quickview.mixin.IWWidget;
import io.github.cottonmc.cotton.gui.widget.WListPanel;
import io.github.cottonmc.cotton.gui.widget.WTextField;
import io.github.cottonmc.cotton.gui.widget.WWidget;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class ViewpointListPanel<D, W extends WWidget> extends WListPanel<D, W> {
    private final WTextField search;
    private final List<D> allData;
    /**
     * 从元素取出「参与搜索的全部文本」。显式传入，避免依赖 toString() 造成的隐式耦合；
     * 返回多个键是为了支持拼音（原文 / 全拼 / 声母任一命中即算匹配）。
     */
    private final Function<D, List<String>> searchKeys;
    private static final int ROW_HEIGHT = 22;
    private static final int COLS = 4;
    private static final int COL_GAP = 5;

    public ViewpointListPanel(List<D> data, Supplier<W> supplier, BiConsumer<D, W> configurator,
                              WTextField search, Function<D, List<String>> searchKeys) {
        super(new ArrayList<>(data), supplier, configurator);
        this.allData = data;
        this.search = search;
        this.searchKeys = searchKeys;
        scrollBar.setScrollingSpeed(8);
    }

    public void setData(List<D> newData) {
        this.allData.clear();
        this.allData.addAll(newData);
        applyFilter();
    }

    public void applyFilter() {
        this.data.clear();

        String query = (search == null || search.getText() == null)
                ? ""
                : search.getText().trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            this.data.addAll(allData);
        } else {
            for (D d : allData) {
                if (matches(d, query)) {
                    this.data.add(d);
                }
            }
        }

        this.configured.clear();
        this.scrollBar.setValue(0);
        this.layout();
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

    @SuppressWarnings("unchecked")
    @Override
    public void layout() {
        this.children.clear();
        this.children.add(scrollBar);
        scrollBar.setLocation(this.width - scrollBar.getWidth(), 0);
        scrollBar.setSize(8, this.height);

        if (!fixedHeight) {
            if (unconfigured.isEmpty()) {
                if (configured.isEmpty()) {
                    W exemplar = ((IWListPanel<W>) this).invokeCreateChild();
                    unconfigured.add(exemplar);
                    if (!exemplar.canResize()) cellHeight = exemplar.getHeight();
                } else {
                    W exemplar = configured.values().iterator().next();
                    if (!exemplar.canResize()) cellHeight = exemplar.getHeight();
                }
            } else {
                W exemplar = unconfigured.get(0);
                if (!exemplar.canResize()) cellHeight = exemplar.getHeight();
            }
        }
        if (cellHeight < 4) cellHeight = 4;

        int totalRows = (int) Math.ceil((double) data.size() / COLS);
        int contentHeight = totalRows * ROW_HEIGHT;

        scrollBar.setWindow(this.height);
        scrollBar.setMaxValue(Math.max(contentHeight, this.height));

        int scrollPixels = scrollBar.getValue();
        int btnWidth = (this.width - scrollBar.getWidth() - COL_GAP * (COLS - 1)) / COLS;

        for (int i = 0; i < data.size(); i++) {
            int row = i / COLS;
            int col = i % COLS;

            int y = row * ROW_HEIGHT - scrollPixels;
            if (y + ROW_HEIGHT < 0 || y > this.height) continue;

            D d = data.get(i);
            W w = configured.get(d);
            if (w == null) {
                if (unconfigured.isEmpty()) {
                    w = ((IWListPanel<W>) this).invokeCreateChild();
                } else {
                    w = unconfigured.remove(0);
                }
                configurator.accept(d, w);
                configured.put(d, w);
            }

            int x = col * (btnWidth + COL_GAP);
            w.setSize(btnWidth, 20);
            ((IWWidget) w).setX(x);
            ((IWWidget) w).setY(y + 1);

            this.children.add(w);
        }
    }
}