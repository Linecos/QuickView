package dev.quickview;

import dev.quickview.gui.ViewpointGUI;
import dev.quickview.gui.WrapperViewpointScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class QuickViewKeybindings {
    private static KeyBinding openMenuKey;
    private static KeyBinding restoreKey;
    private static KeyBinding saveKey;
    private static KeyBinding toggleMoveKey;
    private static KeyBinding toggleFreecamPriorityKey;

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
        toggleMoveKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.quickview.toggleMove", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, category)
        );
        toggleFreecamPriorityKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.quickview.toggleFreecamPriority", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, category)
        );

        ClientTickEvents.START_CLIENT_TICK.register(client -> QuickViewManager.getInstance().onTickStart());

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) return;

            QuickViewManager manager = QuickViewManager.getInstance();

            if (openMenuKey.wasPressed()) {
                manager.loadViewpoints();
                ViewpointGUI gui = new ViewpointGUI();
                WrapperViewpointScreen screen = new WrapperViewpointScreen(gui);
                screen.setParent(MinecraftClient.getInstance().currentScreen);
                MinecraftClient.getInstance().setScreen(screen);
            }

            if (restoreKey.wasPressed() && manager.isViewActive()) {
                manager.restore();
            }

            if (saveKey.wasPressed() && !manager.isViewActive()) {
                manager.loadViewpoints();
                int idx = manager.getViewpoints().size() + 1;
                manager.addViewpoint("View " + idx);
            }

            if (toggleMoveKey.wasPressed()) {
                manager.toggleFreeMove();
                if (manager.isViewActive()) {
                    String key = manager.isFreeMoveEnabled()
                            ? "quickview.message.moveEnabled"
                            : "quickview.message.moveDisabled";
                    client.player.sendMessage(Text.translatable(key), true);
                }
            }

            if (toggleFreecamPriorityKey.wasPressed()) {
                manager.togglePreferFreecam();
                String key = manager.isPreferFreecam()
                        ? "quickview.message.freecamPriorityEnabled"
                        : "quickview.message.freecamPriorityDisabled";
                client.player.sendMessage(Text.translatable(key), true);
            }
        });
    }
}
