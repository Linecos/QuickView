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

    public ViewpointListPanel(List<D> data, Supplier<W> supplier, BiConsumer<D, W> configurator, WTextField search) {
        super(data, supplier, configurator);
        this.search = search;
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

        List<D> filteredData = new ArrayList<>(this.data);
        if (this.search != null && !this.search.getText().isEmpty()) {
            String query = this.search.getText().trim().toLowerCase();
            filteredData = filteredData.stream()
                    .filter(d -> d.toString().toLowerCase().contains(query))
                    .collect(Collectors.toList());
        }

        scrollBar.setWindow(cellsHigh);
        scrollBar.setMaxValue(filteredData.size() > 32 ? filteredData.size() - 8 : 8);
        int scrollOffset = scrollBar.getValue();

        int presentCells = Math.min(filteredData.size() - scrollOffset / 4 + 1, 32);

        int offsetX = 0;
        int offsetY = 0;

        if (presentCells > 0) {
            for (int i = 0; i < presentCells; i++) {
                int index = i + scrollOffset;
                if (index >= filteredData.size()) break;
                if (index < 0) continue;
                D d = filteredData.get(index);
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

                ((IWWidget) w).setX((w.getWidth() + 5) * offsetX);
                ((IWWidget) w).setY(offsetY * 22);
                offsetX++;

                if (offsetX >= 4) {
                    offsetX = 0;
                    offsetY++;
                }

                this.children.add(w);
            }
        }
    }
}
