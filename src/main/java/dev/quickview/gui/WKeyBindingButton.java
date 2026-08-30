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

    public WKeyBindingButton(KeyBinding keyBinding) {
        this.keyBinding = keyBinding;
        setOnClick(this::startListening);
        refreshLabel();
    }

    private void startListening() {
        listening = true;
        requestFocus();
        refreshLabel();
    }

    private void cancelListening() {
        listening = false;
        releaseFocus();
        refreshLabel();
    }

    private void applyKey(int glfwKey) {
        keyBinding.setBoundKey(InputUtil.Type.KEYSYM.createFromCode(glfwKey));
        KeyBinding.updateKeysByCode();
        MinecraftClient.getInstance().options.write();
        listening = false;
        releaseFocus();
        refreshLabel();
    }

    @Override
    public InputResult onKeyPressed(KeyInput input) {
        if (!listening) return super.onKeyPressed(input);

        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            cancelListening();
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
            setLabel(Text.translatable("quickview.gui.settings.key.label",
                    Text.translatable(keyBinding.getId()), keyBinding.getBoundKeyLocalizedText()));
        }
    }
}