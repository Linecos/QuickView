package dev.quickview.gui;

import dev.quickview.QuickViewManager;
import dev.quickview.Viewpoint;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class ViewpointScreen extends Screen {
    private final QuickViewManager manager = QuickViewManager.getInstance();
    private boolean editMode = false;
    private int scrollOffset = 0;
    private static final int ENTRY_HEIGHT = 40;
    private static final int VISIBLE_ENTRIES = 8;
    private int renamingIndex = -1;
    private TextFieldWidget renameField;

    public ViewpointScreen() {
        super(Text.literal("QuickView"));
    }

    @Override
    protected void init() {
        manager.loadViewpoints();
        renameField = new TextFieldWidget(this.textRenderer, 0, 0, 120, 18, Text.literal(""));
        renameField.setMaxLength(50);
        renameField.setVisible(false);
        renameField.setFocused(false);
        addDrawableChild(renameField);
        rebuildUI();
    }

    private void rebuildUI() {
        List<Viewpoint> viewpoints = manager.getViewpoints();
        int centerX = this.width / 2;
        int startY = 40;

        clearChildren();
        addDrawableChild(renameField);
        renameField.setVisible(renamingIndex >= 0);
        renameField.setFocused(renamingIndex >= 0);

        ButtonWidget addButton = ButtonWidget.builder(
                Text.literal("+ Add Viewpoint"),
                button -> {
                    int idx = manager.getViewpoints().size() + 1;
                    manager.addViewpoint("Bookmark " + idx);
                    manager.loadViewpoints();
                    renamingIndex = -1;
                    rebuildUI();
                }
        ).dimensions(centerX - 100, startY, 200, 20).build();
        addDrawableChild(addButton);

        ButtonWidget editToggle = ButtonWidget.builder(
                Text.literal(editMode ? "Done" : "Edit"),
                button -> {
                    editMode = !editMode;
                    renamingIndex = -1;
                    rebuildUI();
                }
        ).dimensions(centerX + 105, startY, 60, 20).build();
        addDrawableChild(editToggle);

        if (manager.isViewActive()) {
            ButtonWidget restoreBtn = ButtonWidget.builder(
                    Text.literal("Restore View"),
                    button -> {
                        manager.restore();
                        renamingIndex = -1;
                        rebuildUI();
                    }
            ).dimensions(centerX - 100, startY + 25, 200, 20).build();
            addDrawableChild(restoreBtn);
        }

        int listStartY = startY + 55;
        int maxVisible = Math.min(VISIBLE_ENTRIES, viewpoints.size());

        for (int i = 0; i < maxVisible; i++) {
            int vpIndex = scrollOffset + i;
            if (vpIndex >= viewpoints.size()) break;

            Viewpoint vp = viewpoints.get(vpIndex);
            int entryY = listStartY + i * ENTRY_HEIGHT;

            int vpIdx = vpIndex;

            if (renamingIndex == vpIdx) {
                renameField.setPosition(centerX - 95, entryY);
                renameField.setVisible(true);
                renameField.setFocused(true);
                renameField.setText(vp.getName());
                renameField.setChangedListener(newName -> {
                    if (!newName.isEmpty()) {
                        manager.renameViewpoint(vpIdx, newName);
                    }
                });
            } else {
                ButtonWidget switchBtn = ButtonWidget.builder(
                        Text.literal(vp.getName()),
                        button -> {
                            manager.switchToViewpoint(vp);
                            renamingIndex = -1;
                            rebuildUI();
                        }
                ).dimensions(centerX - 100, entryY, 140, 20).build();
                addDrawableChild(switchBtn);
            }

            if (editMode) {
                ButtonWidget renameBtn = ButtonWidget.builder(
                        Text.literal("Name"),
                        button -> {
                            renamingIndex = vpIdx;
                            rebuildUI();
                        }
                ).dimensions(centerX + 45, entryY, 40, 20).build();
                addDrawableChild(renameBtn);

                ButtonWidget deleteBtn = ButtonWidget.builder(
                        Text.literal("Del"),
                        button -> {
                            manager.removeViewpoint(vpIdx);
                            if (renamingIndex == vpIdx) renamingIndex = -1;
                            if (scrollOffset > 0 && scrollOffset >= manager.getViewpoints().size()) {
                                scrollOffset = Math.max(0, scrollOffset - 1);
                            }
                            rebuildUI();
                        }
                ).dimensions(centerX + 90, entryY, 40, 20).build();
                addDrawableChild(deleteBtn);

                ButtonWidget upBtn = ButtonWidget.builder(
                        Text.literal("^"),
                        button -> {
                            if (vpIdx > 0) {
                                List<Viewpoint> list = manager.getViewpoints();
                                Viewpoint temp = list.get(vpIdx);
                                list.set(vpIdx, list.get(vpIdx - 1));
                                list.set(vpIdx - 1, temp);
                                manager.saveViewpoints();
                                if (renamingIndex == vpIdx) renamingIndex = vpIdx - 1;
                                else if (renamingIndex == vpIdx - 1) renamingIndex = vpIdx;
                                rebuildUI();
                            }
                        }
                ).dimensions(centerX + 135, entryY, 25, 20).build();
                addDrawableChild(upBtn);

                ButtonWidget downBtn = ButtonWidget.builder(
                        Text.literal("v"),
                        button -> {
                            List<Viewpoint> list = manager.getViewpoints();
                            if (vpIdx < list.size() - 1) {
                                Viewpoint temp = list.get(vpIdx);
                                list.set(vpIdx, list.get(vpIdx + 1));
                                list.set(vpIdx + 1, temp);
                                manager.saveViewpoints();
                                if (renamingIndex == vpIdx) renamingIndex = vpIdx + 1;
                                else if (renamingIndex == vpIdx + 1) renamingIndex = vpIdx;
                                rebuildUI();
                            }
                        }
                ).dimensions(centerX + 165, entryY, 25, 20).build();
                addDrawableChild(downBtn);
            }
        }

        if (viewpoints.size() > VISIBLE_ENTRIES) {
            if (scrollOffset > 0) {
                ButtonWidget scrollUpBtn = ButtonWidget.builder(
                        Text.literal("<<"),
                        button -> {
                            scrollOffset = Math.max(0, scrollOffset - VISIBLE_ENTRIES);
                            renamingIndex = -1;
                            rebuildUI();
                        }
                ).dimensions(centerX - 100, listStartY + maxVisible * ENTRY_HEIGHT + 5, 90, 20).build();
                addDrawableChild(scrollUpBtn);
            }
            if (scrollOffset + VISIBLE_ENTRIES < viewpoints.size()) {
                ButtonWidget scrollDownBtn = ButtonWidget.builder(
                        Text.literal(">>"),
                        button -> {
                            scrollOffset += VISIBLE_ENTRIES;
                            renamingIndex = -1;
                            rebuildUI();
                        }
                ).dimensions(centerX + 10, listStartY + maxVisible * ENTRY_HEIGHT + 5, 90, 20).build();
                addDrawableChild(scrollDownBtn);
            }
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int centerX = this.width / 2;
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, centerX, 15, 0xFFFFFF);

        String dimText = "Dimension: " + manager.getCurrentDimension();
        context.drawCenteredTextWithShadow(this.textRenderer, dimText, centerX, 28, 0xAAAAAA);

        List<Viewpoint> viewpoints = manager.getViewpoints();
        int listStartY = 95;
        int maxVisible = Math.min(VISIBLE_ENTRIES, viewpoints.size());

        for (int i = 0; i < maxVisible; i++) {
            int vpIndex = scrollOffset + i;
            if (vpIndex >= viewpoints.size()) break;

            Viewpoint vp = viewpoints.get(vpIndex);
            int entryY = listStartY + i * ENTRY_HEIGHT;

            if (renamingIndex != vpIndex) {
                context.drawTextWithShadow(this.textRenderer,
                        vp.getFormattedCoords(),
                        centerX - 95, entryY + 22, 0x888888);
                context.drawTextWithShadow(this.textRenderer,
                        vp.getFormattedRotation(),
                        centerX - 95, entryY + 32, 0x666666);
            }
        }

        if (manager.isViewActive()) {
            Viewpoint active = manager.getActiveViewpoint();
            if (active != null) {
                context.drawCenteredTextWithShadow(this.textRenderer,
                        ">> Viewing: " + active.getName() + " <<",
                        centerX, this.height - 30, 0x55FF55);
            }
        }
    }

    @Override
    public boolean keyPressed(KeyInput keyInput) {
        if (renamingIndex >= 0 && renameField != null && renameField.isFocused()) {
            int keyCode = keyInput.key();
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                renamingIndex = -1;
                renameField.setFocused(false);
                renameField.setVisible(false);
                manager.saveViewpoints();
                rebuildUI();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                renamingIndex = -1;
                renameField.setFocused(false);
                renameField.setVisible(false);
                manager.loadViewpoints();
                rebuildUI();
                return true;
            }
        }
        return super.keyPressed(keyInput);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    public static void open() {
        MinecraftClient.getInstance().setScreen(new ViewpointScreen());
    }
}
