package dev.quickview;

import dev.quickview.gui.ViewpointGUI;
import dev.quickview.gui.WrapperViewpointScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuickViewKeybindings {
    private static KeyBinding openMenuKey;
    private static KeyBinding restoreKey;
    private static KeyBinding saveKey;
    private static KeyBinding toggleMoveKey;
    private static KeyBinding toggleFreecamPriorityKey;

    private static final Map<KeyBinding, Integer> DEFAULT_KEYS = new HashMap<>();

    public static List<KeyBinding> getAll() {
        return List.of(openMenuKey, restoreKey, saveKey, toggleMoveKey, toggleFreecamPriorityKey);
    }

    public static int getDefaultKey(KeyBinding kb) {
        return DEFAULT_KEYS.getOrDefault(kb, GLFW.GLFW_KEY_UNKNOWN);
    }

    public static void register() {
        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of("quickview", "quickview"));

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

        DEFAULT_KEYS.put(openMenuKey, GLFW.GLFW_KEY_V);
        DEFAULT_KEYS.put(restoreKey, GLFW.GLFW_KEY_B);
        DEFAULT_KEYS.put(saveKey, GLFW.GLFW_KEY_N);
        DEFAULT_KEYS.put(toggleMoveKey, GLFW.GLFW_KEY_G);
        DEFAULT_KEYS.put(toggleFreecamPriorityKey, GLFW.GLFW_KEY_H);

        ClientTickEvents.START_CLIENT_TICK.register(client -> QuickViewManager.getInstance().onTickStart());

        // 断线 / 退出世界时清空自由视角状态，避免 viewActive 与 savedPerspective 跨世界残留，
        // 导致进入下一个世界时相机被锁在旧坐标、移动输入被清零。
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                QuickViewManager.getInstance().clearViewState());
        // 兜底：万一 DISCONNECT 没触发（异常退出等），进新世界时再清一次。
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
                QuickViewManager.getInstance().clearViewState());

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

            if (saveKey.wasPressed() && manager.isQuickAddEnabled()) {
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
