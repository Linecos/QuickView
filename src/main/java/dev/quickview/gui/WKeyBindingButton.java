package dev.quickview.gui;

import io.github.cottonmc.cotton.gui.widget.WButton;
import io.github.cottonmc.cotton.gui.widget.data.InputResult;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class WKeyBindingButton extends WButton {
    private final KeyBinding keyBinding;
    private boolean listening = false;
    private Runnable onChange;

    public WKeyBindingButton(KeyBinding keyBinding) {
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
        keyBinding.setBoundKey(InputUtil.Type.KEYSYM.createFromCode(defaultKey));
        KeyBinding.updateKeysByCode();
        MinecraftClient.getInstance().options.write();
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
        keyBinding.setBoundKey(InputUtil.Type.KEYSYM.createFromCode(glfwKey));
        KeyBinding.updateKeysByCode();
        MinecraftClient.getInstance().options.write();
        stopListening();
        refreshLabel();
        if (onChange != null) onChange.run();
    }

    @Override
    public InputResult onKeyPressed(KeyInput input) {
        if (!listening) return super.onKeyPressed(input);

        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            applyKey(GLFW.GLFW_KEY_UNKNOWN);
        } else {
            applyKey(input.key());
        }
        return InputResult.PROCESSED;
    }

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
            setLabel(Text.translatable("quickview.gui.settings.key.prompt"));
        } else {
            setLabel(keyBinding.getBoundKeyLocalizedText());
        }
    }
}