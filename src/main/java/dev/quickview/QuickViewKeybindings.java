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

    /** 「打开菜单」键本体：界面打开时 keybinding 不计数，屏幕层要靠它自己判断关闭（见 WrapperViewpointScreen）。 */
    public static KeyBinding getOpenMenuKey() {
        return openMenuKey;
    }

    public static KeyBinding getSaveKey() {
        return saveKey;
    }

    public static KeyBinding getToggleMoveKey() {
        return toggleMoveKey;
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

            // 先做受伤监测（命中且开关打开时它会直接恢复本体视角），再处理按键，
            // 免得同 tick 的按键逻辑基于已经失效的自由视角状态做判断
            if (manager.tickDamageWatch()) {
                // 受伤恢复时把 QuickView 界面一并关掉：界面开着时移动输入被拦截，
                // 否则玩家会陷入「已经回到本体、却动不了」的状态。确认弹窗也一并关（等于取消，不会误执行）
                closeQuickViewScreens();
            }

            if (openMenuKey.wasPressed()) {
                // 再按一次 V 关闭：任何 QuickView 界面（主菜单/编辑页/设置页/确认弹窗）
                // 都直接关闭，而不是再叠一层新菜单
                if (closeQuickViewScreens()) {
                    return;
                }
                // ViewpointGUI 构造函数里已经 loadViewpoints()，这里不重复读盘
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

    /**
     * 关闭当前打开的 QuickView 界面（主菜单 / 编辑页 / 设置页 / 确认弹窗都是 {@link WrapperViewpointScreen}）。
     *
     * @return 是否真的关掉了一个；false 表示当前没有 QuickView 界面打开
     */
    private static boolean closeQuickViewScreens() {
        if (MinecraftClient.getInstance().currentScreen instanceof WrapperViewpointScreen) {
            MinecraftClient.getInstance().setScreen(null);
            return true;
        }
        return false;
    }
}
