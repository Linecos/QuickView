package dev.quickview.gui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.quickview.QuickViewKeybindings;
import io.github.cottonmc.cotton.gui.GuiDescription;
import io.github.cottonmc.cotton.gui.client.CottonClientScreen;
import io.github.cottonmc.cotton.gui.widget.WTextField;
import io.github.cottonmc.cotton.gui.widget.WWidget;
import io.github.cottonmc.cotton.gui.widget.data.InputResult;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
//? if >=1.21.9 {
import net.minecraft.client.input.KeyEvent;
//?}
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

    //? if >=1.21.9 {
    @Override
    public boolean keyPressed(KeyEvent keyInput) {
        GuiDescription description = getDescription();
        WWidget focus = description != null ? description.getFocus() : null;
        if (focus != null && focus.onKeyPressed(keyInput) == InputResult.PROCESSED) {
            // 键位捕获按钮等焦点控件先吃按键
            return true;
        }

        if (focus instanceof WTextField) {
            // 焦点在文本框上时不能关界面：字母键（如 V）不进 onKeyPressed、走 charTyped
            // 通道上屏，所以上面的焦点派发拦不住，必须在这里放行，否则在输入框里打 V 会误关。
            // ESC 仍走 super 的原版关闭路径。
            return super.keyPressed(keyInput);
        }

        // MC 的 keybinding 计数在有界面打开时不工作（按键全给了屏幕），
        // 所以「再按一次打开菜单键关闭界面」只能在这里做
        InputConstants.Key openMenuBound = KeyBindingHelper.getBoundKeyOf(QuickViewKeybindings.getOpenMenuKey());
        if (openMenuBound.getType() == InputConstants.Type.KEYSYM
                && keyInput.key() == openMenuBound.getValue()) {
            Minecraft.getInstance().setScreen(null);
            return true;
        }

        boolean isEscape = keyInput.key() == GLFW.GLFW_KEY_ESCAPE;
        if (isEscape && this.parent != null) {
            Minecraft.getInstance().setScreen(this.parent);
            return true;
        }
        return super.keyPressed(keyInput);
    }
    //?} else {
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        GuiDescription description = getDescription();
        WWidget focus = description != null ? description.getFocus() : null;
        if (focus != null && focus.onKeyPressed(keyCode, scanCode, modifiers) == InputResult.PROCESSED) {
            // 键位捕获按钮等焦点控件先吃按键
            return true;
        }

        if (focus instanceof WTextField) {
            // 焦点在文本框上时不能关界面：字母键（如 V）不进 onKeyPressed、走 charTyped
            // 通道上屏，所以上面的焦点派发拦不住，必须在这里放行，否则在输入框里打 V 会误关。
            // ESC 仍走 super 的原版关闭路径。
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        // MC 的 keybinding 计数在有界面打开时不工作（按键全给了屏幕），
        // 所以「再按一次打开菜单键关闭界面」只能在这里做
        InputConstants.Key openMenuBound = KeyBindingHelper.getBoundKeyOf(QuickViewKeybindings.getOpenMenuKey());
        if (openMenuBound.getType() == InputConstants.Type.KEYSYM
                && keyCode == openMenuBound.getValue()) {
            Minecraft.getInstance().setScreen(null);
            return true;
        }

        boolean isEscape = keyCode == GLFW.GLFW_KEY_ESCAPE;
        if (isEscape && this.parent != null) {
            Minecraft.getInstance().setScreen(this.parent);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    //?}
}
