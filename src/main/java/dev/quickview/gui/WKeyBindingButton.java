package dev.quickview.gui;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.cottonmc.cotton.gui.widget.WButton;
import io.github.cottonmc.cotton.gui.widget.data.InputResult;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
//? if >=1.21.9 {
import net.minecraft.client.input.KeyEvent;
//?}
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class WKeyBindingButton extends WButton {
    private final KeyMapping keyBinding;
    private boolean listening = false;
    private Runnable onChange;

    public WKeyBindingButton(KeyMapping keyBinding) {
        this.keyBinding = keyBinding;
        setOnClick(this::startListening);
        refreshLabel();
    }

    public WKeyBindingButton setOnChange(Runnable onChange) {
        this.onChange = onChange;
        return this;
    }

    /** 当前绑定是否就是原版默认键（直接用 KeyBinding.isDefault()，无需比对 InputUtil.Key）。 */
    public boolean isAtDefault() {
        return keyBinding.isDefault();
    }

    public void resetToDefault(int defaultKey) {
        keyBinding.setKey(InputConstants.Type.KEYSYM.getOrCreate(defaultKey));
        KeyMapping.resetMapping();
        Minecraft.getInstance().options.save();
        if (listening) {
            stopListening();
        }
        refreshLabel();
        if (onChange != null) onChange.run();
    }

    private void startListening() {
        listening = true;
        requestFocus();
        refreshLabel();
    }

    /** 结束监听：清标志 + 释放焦点。改键 / 重置后共用（{@code releaseFocus} 内部有身份检查，可安全调用）。 */
    private void stopListening() {
        listening = false;
        releaseFocus();
    }

    private void applyKey(int glfwKey) {
        keyBinding.setKey(InputConstants.Type.KEYSYM.getOrCreate(glfwKey));
        KeyMapping.resetMapping();
        Minecraft.getInstance().options.save();
        stopListening();
        refreshLabel();
        if (onChange != null) onChange.run();
    }

    //? if >=1.21.9 {
    @Override
    public InputResult onKeyPressed(KeyEvent input) {
        if (!listening) return super.onKeyPressed(input);

        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            applyKey(GLFW.GLFW_KEY_UNKNOWN);
        } else {
            applyKey(input.key());
        }
        return InputResult.PROCESSED;
    }
    //?} else {
    @Override
    public InputResult onKeyPressed(int ch, int key, int modifiers) {
        if (!listening) return super.onKeyPressed(ch, key, modifiers);

        if (ch == GLFW.GLFW_KEY_ESCAPE) {
            applyKey(GLFW.GLFW_KEY_UNKNOWN);
        } else {
            applyKey(ch);
        }
        return InputResult.PROCESSED;
    }
    //?}

    @Override
    public void onFocusLost() {
        super.onFocusLost();
        if (listening) {
            listening = false;
            refreshLabel();
        }
    }

    private void refreshLabel() {
        if (listening) {
            setLabel(Component.translatable("quickview.gui.settings.key.prompt"));
        } else {
            setLabel(keyBinding.getTranslatedKeyMessage());
        }
    }
}