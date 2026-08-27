package dev.quickview.gui;

import dev.quickview.mixin.IWListPanel;
import dev.quickview.mixin.IWWidget;
import io.github.cottonmc.cotton.gui.widget.WListPanel;
import io.github.cottonmc.cotton.gui.widget.WTextField;
import io.github.cottonmc.cotton.gui.widget.WWidget;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ViewpointListPanel<D, W extends WWidget> extends WListPanel<D, W> {
    private final WTextField search;
    private static final int ROW_HEIGHT = 22;
    private static final int COLS = 4;
    private static final int COL_GAP = 5;

    public ViewpointListPanel(List<D> data, Supplier<W> supplier, BiConsumer<D, W> configurator, WTextField search) {
        super(data, supplier, configurator);
        this.search = search;
        this.fixedHeight = true;
    }

    public void setData(List<D> newData) {
        this.data.clear();
        this.data.addAll(newData);
        this.configured.clear();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void layout() {
        this.children.clear();
        this.children.add(scrollBar);

        int panelWidth = this.width;
        int panelHeight = this.height;

        scrollBar.setLocation(panelWidth - 8, 0);
        scrollBar.setSize(8, panelHeight);

        List<D> filteredData = new ArrayList<>(this.data);
        if (this.search != null && !this.search.getText().isEmpty()) {
            String query = this.search.getText().trim().toLowerCase();
            filteredData = filteredData.stream()
                    .filter(d -> d.toString().toLowerCase().contains(query))
                    .collect(Collectors.toList());
        }

        int totalRows = (int) Math.ceil((double) filteredData.size() / COLS);
        int visibleRows = panelHeight / ROW_HEIGHT;
        int maxScroll = Math.max(0, (totalRows - visibleRows) * ROW_HEIGHT);

        scrollBar.setWindow(panelHeight);
        scrollBar.setMaxValue(maxScroll);

        int scrollPixels = scrollBar.getValue();

        int btnWidth = (panelWidth - 8 - COL_GAP * (COLS - 1)) / COLS;

        for (int i = 0; i < filteredData.size(); i++) {
            int row = i / COLS;
            int col = i % COLS;

            int y = row * ROW_HEIGHT - scrollPixels;
            if (y + ROW_HEIGHT < 0 || y > panelHeight) continue;

            D d = filteredData.get(i);
            W w = configured.get(d);
            if (w == null) {
                if (unconfigured.isEmpty()) {
                    w = ((IWListPanel<W>) this).invokeCreateChild();
                } else {
                    w = unconfigured.remove(0);
                }
                configured.put(d, w);
            }
            configurator.accept(d, w);

            int x = col * (btnWidth + COL_GAP);
            w.setSize(btnWidth, 20);
            ((IWWidget) w).setX(x);
            ((IWWidget) w).setY(y + 1);

            this.children.add(w);
        }
    }
}
