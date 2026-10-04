package dev.quickview.gui;

import io.github.cottonmc.cotton.gui.widget.WButton;
import io.github.cottonmc.cotton.gui.widget.data.InputResult;
import net.minecraft.client.gui.Click;
import net.minecraft.text.Text;

/**
 * 书签条目按钮，支持拖拽排序。
 *
 * <p>LibGui 的 {@code MouseInputHandler} 会把「命中的最深层 widget」记为 lastResponder
 * （{@code MouseInputHandler.onMouseDown} 里 {@code setLastResponder} 与返回值无关），
 * 之后的拖拽事件一律派发给它，并把坐标换算到它的坐标系 —— 所以即使指针拖出自身范围，
 * {@link #onMouseDrag} 也会持续收到事件。
 *
 * <p>只有 {@link DragHost#isDragEnabled()} 为真（用户打开了排序模式）时才识别拖拽，
 * 否则条目就是普通按钮。
 *
 * <p>拖拽结束那一次鼠标释放仍会走一次 {@code onClick}，所以用 {@code suppressClick} 把它吞掉，
 * 否则「拖一下条目」会顺带触发切换视角。
 */
public class WViewpointEntry extends WButton {
    /** 超过这个像素位移才算拖拽，避免手抖就让点击失效。 */
    private static final int DRAG_THRESHOLD = 4;
    /** 拖拽中的源条目：变暗表示「已被拿起」。 */
    private static final int DRAG_SOURCE_COLOR = 0xFF808080;

    private DragHost dragHost;
    private boolean pressed;
    private boolean dragging;
    private boolean suppressClick;
    private int pressX;
    private int pressY;
    private int normalColor;

    /** 由列表面板实现，负责把条目在列表里的移动落到实处。 */
    public interface DragHost {
        /** 是否允许拖拽（排序模式）。为 false 时条目退化成普通按钮。 */
        boolean isDragEnabled();

        /** entry 正在被拖拽，坐标是相对于列表面板左上角的（可能为负或超出面板）。 */
        void entryDragged(WViewpointEntry entry, int panelX, int panelY);

        /** 拖拽结束（鼠标释放），由宿主决定是否真的移动以及移动到哪。 */
        void entryDropped(WViewpointEntry entry);
    }

    public WViewpointEntry() {
        super(Text.literal(""));
        this.normalColor = this.color;
    }

    public void setDragHost(DragHost dragHost) {
        this.dragHost = dragHost;
    }

    public boolean isDragging() {
        return dragging;
    }

    /** 拖拽中的源条目视觉：变暗表示「已被拿起」。 */
    public void setDragSource(boolean dragSource) {
        this.color = dragSource ? DRAG_SOURCE_COLOR : normalColor;
    }

    @Override
    public InputResult onMouseDown(Click click, boolean doubled) {
        pressed = true;
        dragging = false;
        suppressClick = false;
        pressX = (int) click.x();
        pressY = (int) click.y();
        return InputResult.PROCESSED;
    }

    @Override
    public InputResult onMouseDrag(Click click, double offsetX, double offsetY) {
        if (!pressed || dragHost == null || !dragHost.isDragEnabled()) {
            return InputResult.IGNORED;
        }

        int x = (int) click.x();
        int y = (int) click.y();
        if (!dragging
                && (Math.abs(x - pressX) > DRAG_THRESHOLD || Math.abs(y - pressY) > DRAG_THRESHOLD)) {
            dragging = true;
        }

        if (dragging) {
            // click 的坐标是本 widget 坐标系的，加上自身位置即得到面板坐标系
            dragHost.entryDragged(this, x + getX(), y + getY());
        }
        return InputResult.PROCESSED;
    }

    @Override
    public InputResult onMouseUp(Click click) {
        pressed = false;
        boolean wasDragging = dragging;
        dragging = false;
        suppressClick = wasDragging;

        if (wasDragging) {
            if (dragHost != null) {
                dragHost.entryDropped(this);
            }
            return InputResult.PROCESSED;
        }
        return InputResult.IGNORED;
    }

    @Override
    public InputResult onClick(Click click, boolean doubled) {
        if (suppressClick) {
            suppressClick = false;
            return InputResult.PROCESSED;
        }
        return super.onClick(click, doubled);
    }
}
