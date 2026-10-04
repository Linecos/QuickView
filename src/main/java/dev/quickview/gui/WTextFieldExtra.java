package dev.quickview.gui;

import io.github.cottonmc.cotton.gui.widget.WTextField;
import net.minecraft.text.Text;

import java.util.function.Consumer;

public class WTextFieldExtra extends WTextField {
    private Consumer<String> focusLostCallback;

    public WTextFieldExtra setFocusLostCallback(Consumer<String> callback) {
        this.focusLostCallback = callback;
        return this;
    }

    @Override
    public WTextFieldExtra setSuggestion(Text suggestion) {
        super.setSuggestion(suggestion);
        return this;
    }

    @Override
    public WTextFieldExtra setChangedListener(Consumer<String> onChanged) {
        super.setChangedListener(onChanged);
        return this;
    }

    @Override
    public WTextFieldExtra setMaxLength(int max) {
        super.setMaxLength(max);
        return this;
    }

    @Override
    public void onFocusLost() {
        super.onFocusLost();
        if (focusLostCallback != null) {
            focusLostCallback.accept(getText());
        }
    }
}