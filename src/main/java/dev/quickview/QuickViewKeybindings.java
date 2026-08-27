package dev.quickview;

import dev.quickview.gui.ViewpointScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class QuickViewKeybindings {
    private static KeyBinding openMenuKey;
    private static KeyBinding restoreKey;
    private static KeyBinding saveKey;

    public static void register() {
        KeyBinding.Category category = KeyBinding.Category.MISC;

        openMenuKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.quickview.openMenu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V, category)
        );
        restoreKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.quickview.restore", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, category)
        );
        saveKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.quickview.save", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_N, category)
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) return;

            QuickViewManager manager = QuickViewManager.getInstance();

            if (openMenuKey.wasPressed()) {
                manager.loadViewpoints();
                MinecraftClient.getInstance().setScreen(new ViewpointScreen());
            }

            if (restoreKey.wasPressed() && manager.isViewActive()) {
                manager.restore();
            }

            if (saveKey.wasPressed() && !manager.isViewActive()) {
                manager.loadViewpoints();
                int idx = manager.getViewpoints().size() + 1;
                manager.addViewpoint("Bookmark " + idx);
            }
        });
    }
}
