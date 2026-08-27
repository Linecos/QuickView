package dev.quickview.gui;

import dev.quickview.mixin.IWListPanel;
import dev.quickview.mixin.IWWidget;
import io.github.cottonmc.cotton.gui.client.ScreenDrawing;
import io.github.cottonmc.cotton.gui.widget.WListPanel;
import io.github.cottonmc.cotton.gui.widget.WTextField;
import io.github.cottonmc.cotton.gui.widget.WWidget;
import io.github.cottonmc.cotton.gui.widget.data.InputResult;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ViewpointListPanel<D, W extends WWidget> extends WListPanel<D, W> {
    private final WTextField search;
    private final List<D> allData;
    private int lastScrollValue = -1;
    private static final int ROW_HEIGHT = 22;
    private static final int COLS = 4;
    private static final int COL_GAP = 5;

    public ViewpointListPanel(List<D> data, Supplier<W> supplier, BiConsumer<D, W> configurator, WTextField search) {
        super(new ArrayList<>(data), supplier, configurator);
        this.allData = data;
        this.search = search;
    }

    public void setData(List<D> newData) {
        this.allData.clear();
        this.allData.addAll(newData);
        applyFilter();
    }

    public void applyFilter() {
        if (this.search == null || this.search.getText().isEmpty()) {
            this.data.clear();
            this.data.addAll(allData);
        } else {
            String query = this.search.getText().trim().toLowerCase();
            this.data.clear();
            this.data.addAll(allData.stream()
                    .filter(d -> d.toString().toLowerCase().contains(query))
                    .collect(Collectors.toList()));
        }
        this.configured.clear();
        this.scrollBar.setValue(0);
        this.lastScrollValue = -1;
        this.layout();
    }

    @Override
    public void tick() {
        super.tick();
        if (scrollBar.getValue() != lastScrollValue) {
            lastScrollValue = scrollBar.getValue();
            relayoutItems();
        }
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

        int layoutHeight = this.getHeight() - 4;
        int cellsHigh = Math.max((layoutHeight + 2) / (cellHeight + 2), 1);

        scrollBar.setWindow(cellsHigh);
        scrollBar.setMaxValue(data.size() > 32 ? data.size() - 8 : 8);

        relayoutItems();
    }

    @SuppressWarnings("unchecked")
    private void relayoutItems() {
        this.children.clear();
        this.children.add(scrollBar);

        int panelHeight = this.height;
        int scrollOffset = scrollBar.getValue();
        int btnWidth = (this.width - scrollBar.getWidth() - COL_GAP * (COLS - 1)) / COLS;

        int presentCells = Math.min(data.size() - scrollOffset / 4 + 1, 32);

        int offsetX = 0;
        int offsetY = 0;

        if (presentCells > 0) {
            for (int i = 0; i < presentCells; i++) {
                int index = i + scrollOffset;
                if (index >= data.size()) break;
                if (index < 0) continue;
                D d = data.get(index);
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

                w.setSize(btnWidth, 20);
                ((IWWidget) w).setX((w.getWidth() + COL_GAP) * offsetX);
                ((IWWidget) w).setY(offsetY * ROW_HEIGHT);
                offsetX++;

                if (offsetX >= COLS) {
                    offsetX = 0;
                    offsetY++;
                }

                this.children.add(w);
            }
        }
    }
}
