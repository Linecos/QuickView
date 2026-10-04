package dev.quickview.gui;

import io.github.cottonmc.cotton.gui.GuiDescription;
import io.github.cottonmc.cotton.gui.client.CottonClientScreen;
import io.github.cottonmc.cotton.gui.widget.WWidget;
import io.github.cottonmc.cotton.gui.widget.data.InputResult;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public class WrapperViewpointScreen extends CottonClientScreen {
    @Nullable
    private Runnable closeCallback;
    @Nullable
    private Screen parent;

    public WrapperViewpointScreen(GuiDescription description) {
        super(description);
    }

    public void setCloseCallback(@Nullable Runnable closeCallback) {
        this.closeCallback = closeCallback;
    }

    public void setParent(@Nullable Screen parent) {
        this.parent = parent;
    }

    @Nullable
    public Screen getParent() {
        return this.parent;
    }

    @Override
    public void removed() {
        if (this.closeCallback != null) {
            closeCallback.run();
        }
        super.removed();
    }

    @Override
    public boolean keyPressed(KeyInput keyInput) {
        GuiDescription description = getDescription();
        WWidget focus = description != null ? description.getFocus() : null;
        if (focus != null && focus.onKeyPressed(keyInput) == InputResult.PROCESSED) {
            return true;
        }

        boolean isEscape = keyInput.key() == GLFW.GLFW_KEY_ESCAPE;
        if (isEscape && this.parent != null) {
            MinecraftClient.getInstance().setScreen(this.parent);
            return true;
        }
        return super.keyPressed(keyInput);
    }
}
