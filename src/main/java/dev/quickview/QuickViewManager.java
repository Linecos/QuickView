package dev.quickview;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.Util;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

public class QuickViewManager {
    private static final QuickViewManager INSTANCE = new QuickViewManager();
    private static final double MOVE_RAMP = 0.15;
    private static final double MOVE_DECELERATION = 0.4;
    private static final double MOVE_SPEED = 0.7;
    private static final double SPRINT_MULTIPLIER = 3.0;

    private List<Viewpoint> viewpoints = new ArrayList<>();
    private Viewpoint activeViewpoint;
    private String currentContext = "";
    private String currentDimension = "";
    private boolean viewActive = false;

    /** 全局设置（config/quickview.json），改动即落盘。 */
    private QuickViewConfig config = new QuickViewConfig();

    private double freeX;
    private double freeY;
    private double freeZ;
    private float freeYaw;
    private float freePitch;

    private double prevX;
    private double prevY;
    private double prevZ;

    private Perspective savedPerspective;

    private double velForward;
    private double velStrafe;
    private double velVertical;

    /** 平滑过渡状态：从 from* 插值到 target*，由 {@link #updateTransition()} 每帧推进。 */
    private boolean transitionActive;
    private long transitionStartMs;
    private int transitionMillis;
    private double fromX;
    private double fromY;
    private double fromZ;
    private float fromYaw;
    private float fromPitch;
    private double targetX;
    private double targetY;
    private double targetZ;
    private float targetYaw;
    private float targetPitch;

    private QuickViewManager() {
    }

    public static QuickViewManager getInstance() {
        return INSTANCE;
    }

    /** 从 config/quickview.json 读取设置，应在客户端初始化时调用一次。 */
    public void loadConfig() {
        this.config = QuickViewConfig.load();
    }

    public boolean isViewActive() {
        return viewActive;
    }

    public Viewpoint getActiveViewpoint() {
        return activeViewpoint;
    }

    public boolean isQuickAddEnabled() {
        return config.isQuickAddEnabled();
    }

    public void toggleQuickAdd() {
        config.setQuickAddEnabled(!config.isQuickAddEnabled());
        config.save();
    }

    public boolean isFreeMoveEnabled() {
        return config.isFreeMoveEnabled();
    }

    public void toggleFreeMove() {
        config.setFreeMoveEnabled(!config.isFreeMoveEnabled());
        config.save();
    }

    public boolean isPreferFreecam() {
        return config.isPreferFreecam();
    }

    public void togglePreferFreecam() {
        config.setPreferFreecam(!config.isPreferFreecam());
        config.save();
    }

    public boolean isSmoothTransitionEnabled() {
        return config.isSmoothTransitionEnabled();
    }

    public void toggleSmoothTransition() {
        config.setSmoothTransitionEnabled(!config.isSmoothTransitionEnabled());
        config.save();
    }

    public double getFreeX() {
        return freeX;
    }

    public double getFreeY() {
        return freeY;
    }

    public double getFreeZ() {
        return freeZ;
    }

    public float getFreeYaw() {
        return freeYaw;
    }

    public float getFreePitch() {
        return freePitch;
    }

    public double getPrevFreeX() {
        return prevX;
    }

    public double getPrevFreeY() {
        return prevY;
    }

    public double getPrevFreeZ() {
        return prevZ;
    }

    public void onTickStart() {
        if (!viewActive) return;

        prevX = freeX;
        prevY = freeY;
        prevZ = freeZ;
    }

    public void applyFreecamLook(double cursorDeltaX, double cursorDeltaY) {
        if (!viewActive || !config.isFreeMoveEnabled() || transitionActive) return;

        freeYaw += (float) (cursorDeltaX * 0.15);
        freePitch += (float) (cursorDeltaY * 0.15);
        freePitch = Math.max(-90.0f, Math.min(90.0f, freePitch));
    }

    public void applyFreecamMovement(PlayerInput input) {
        if (!viewActive) return;

        int forward = 0;
        int strafe = 0;
        int vertical = 0;

        if (config.isFreeMoveEnabled()) {
            if (input.forward()) forward += 1;
            if (input.backward()) forward -= 1;
            if (input.right()) strafe += 1;
            if (input.left()) strafe -= 1;
            if (input.jump()) vertical += 1;
            if (input.sneak()) vertical -= 1;
        }

        boolean sprint = config.isFreeMoveEnabled() && input.sprint();

        double diagonal = (forward != 0 && strafe != 0) ? 1.2 : 1.0;
        velForward = rampVelocity(velForward, forward) / diagonal;
        velStrafe = rampVelocity(velStrafe, strafe) / diagonal;
        velVertical = rampVelocity(velVertical, vertical);

        double yaw = Math.toRadians(freeYaw);
        double cosYaw = Math.cos(yaw);
        double sinYaw = Math.sin(yaw);

        double fx = -sinYaw;
        double fz = cosYaw;
        double rx = -cosYaw;
        double rz = -sinYaw;

        double forwardFactor = sprint ? velForward * SPRINT_MULTIPLIER : velForward;

        freeX += (fx * forwardFactor + rx * velStrafe) * MOVE_SPEED;
        freeY += velVertical * MOVE_SPEED;
        freeZ += (fz * forwardFactor + rz * velStrafe) * MOVE_SPEED;
    }

    private double rampVelocity(double current, int input) {
        if (input != 0) {
            double ramp = MOVE_RAMP;
            if (input < 0) {
                ramp = -MOVE_RAMP;
            }
            if ((input < 0) != (current < 0.0)) {
                current = 0.0;
            }
            current = Math.max(-1.0, Math.min(1.0, current + ramp));
        } else {
            current *= MOVE_DECELERATION;
        }
        return current;
    }

    public List<Viewpoint> getViewpoints() {
        return viewpoints;
    }

    public String getCurrentDimension() {
        return currentDimension;
    }

    public String getCurrentContext() {
        return currentContext;
    }

    public void loadViewpoints() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld world = client.world;
        if (world == null) return;

        RegistryKey<World> dimKey = world.getRegistryKey();
        Identifier dimId = dimKey.getValue();
        currentDimension = dimId.toString();
        currentContext = resolveContext(client);
        viewpoints = ViewpointStorage.load(currentContext, currentDimension);
    }

    public void saveViewpoints() {
        ViewpointStorage.save(currentContext, currentDimension, viewpoints);
    }

    private static String resolveContext(MinecraftClient client) {
        ServerInfo entry = client.getCurrentServerEntry();
        if (entry != null) {
            String address = entry.address;
            if (address != null && !address.isEmpty()) {
                return address;
            }
            return entry.name;
        }
        if (client.getServer() != null) {
            return client.getServer().getSaveProperties().getLevelName();
        }
        return "unknown";
    }

    public void addViewpoint(String name) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        Viewpoint vp = captureViewSnapshot(name);
        if (vp == null) return;
        viewpoints.add(vp);
        saveViewpoints();
    }

    public Viewpoint createViewpoint(String name) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return null;

        Viewpoint vp = captureViewSnapshot(name);
        if (vp == null) return null;
        viewpoints.add(vp);
        saveViewpoints();
        return vp;
    }

    public Viewpoint captureViewSnapshot(String name) {
        MinecraftClient client = MinecraftClient.getInstance();

        // 处于 QuickView 自由视角时，快照必须基于“当前正在看的相机”，而非玩家本体
        if (viewActive) {
            return new Viewpoint(name, currentDimension, freeX, freeY, freeZ, freeYaw, freePitch);
        }

        Entity entity = resolveViewEntity(client);
        if (entity == null) return null;

        Vec3d eye = entity.getEyePos();
        return new Viewpoint(name, currentDimension, eye.x, eye.y, eye.z, entity.getYaw(), entity.getPitch());
    }

    private Entity resolveViewEntity(MinecraftClient client) {
        Entity cameraEntity = client.getCameraEntity();
        if (config.isPreferFreecam() && cameraEntity != null && cameraEntity != client.player) {
            return cameraEntity;
        }
        return client.player;
    }

    public void removeViewpoint(int index) {
        if (index >= 0 && index < viewpoints.size()) {
            viewpoints.remove(index);
            saveViewpoints();
        }
    }

    public void renameViewpoint(int index, String newName) {
        if (index >= 0 && index < viewpoints.size()) {
            viewpoints.get(index).setName(newName);
            saveViewpoints();
        }
    }

    public void switchToViewpoint(Viewpoint vp) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        // 必须在 viewActive 置位前取值：若已经是自由视角（例如在菜单里又点了另一个书签），
        // options 里的视角此时已被强制成 FIRST_PERSON，再存一次会把玩家真正的原始视角
        // 覆盖掉，恢复时就回不去了。
        boolean firstEntry = !viewActive;

        // 过渡起点：自由视角下是当前自由相机；否则是玩家本体（或相机实体）的眼睛位置
        double startX;
        double startY;
        double startZ;
        float startYaw;
        float startPitch;
        if (viewActive) {
            startX = freeX;
            startY = freeY;
            startZ = freeZ;
            startYaw = freeYaw;
            startPitch = freePitch;
        } else {
            Entity entity = resolveViewEntity(client);
            if (entity == null) return;
            Vec3d eye = entity.getEyePos();
            startX = eye.x;
            startY = eye.y;
            startZ = eye.z;
            startYaw = entity.getYaw();
            startPitch = entity.getPitch();
        }

        activeViewpoint = vp;
        viewActive = true;
        targetX = vp.getX();
        targetY = vp.getY();
        targetZ = vp.getZ();
        targetYaw = vp.getYaw();
        targetPitch = vp.getPitch();

        velForward = 0.0;
        velStrafe = 0.0;
        velVertical = 0.0;

        transitionMillis = config.getSmoothTransitionMillis();
        if (config.isSmoothTransitionEnabled() && transitionMillis > 0) {
            fromX = startX;
            fromY = startY;
            fromZ = startZ;
            fromYaw = startYaw;
            fromPitch = startPitch;
            transitionStartMs = Util.getMeasuringTimeMs();
            transitionActive = true;
            freeX = startX;
            freeY = startY;
            freeZ = startZ;
            freeYaw = startYaw;
            freePitch = startPitch;
        } else {
            transitionActive = false;
            freeX = targetX;
            freeY = targetY;
            freeZ = targetZ;
            freeYaw = targetYaw;
            freePitch = targetPitch;
        }

        prevX = freeX;
        prevY = freeY;
        prevZ = freeZ;

        if (firstEntry) {
            savedPerspective = client.options.getPerspective();
        }
        client.options.setPerspective(Perspective.FIRST_PERSON);
    }

    /**
     * 推进平滑过渡，由 {@code CameraMixin} 每帧调用一次（必须在读取自由相机坐标之前）。
     * <p>
     * 过渡期间把 {@code prev*} 同步成当前值，避免 {@code CameraMixin} 再按 tickProgress 插值一次造成二次平滑。
     */
    public void updateTransition() {
        if (!transitionActive) return;

        long elapsed = Util.getMeasuringTimeMs() - transitionStartMs;
        double t = transitionMillis <= 0 ? 1.0 : Math.min(1.0, elapsed / (double) transitionMillis);
        double eased = t * t * (3.0 - 2.0 * t);

        freeX = fromX + (targetX - fromX) * eased;
        freeY = fromY + (targetY - fromY) * eased;
        freeZ = fromZ + (targetZ - fromZ) * eased;
        freeYaw = fromYaw + shortestAngleDelta(fromYaw, targetYaw) * (float) eased;
        freePitch = fromPitch + (targetPitch - fromPitch) * (float) eased;

        prevX = freeX;
        prevY = freeY;
        prevZ = freeZ;

        if (t >= 1.0) {
            transitionActive = false;
            freeYaw = targetYaw;
            freePitch = targetPitch;
        }
    }

    /** 两个偏航角之间的最短角度差，返回 [-180, 180)，避免转身时绕远路。 */
    private static float shortestAngleDelta(float from, float to) {
        float delta = (to - from) % 360.0f;
        if (delta >= 180.0f) {
            delta -= 360.0f;
        } else if (delta < -180.0f) {
            delta += 360.0f;
        }
        return delta;
    }

    public void restore() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (savedPerspective != null) {
            client.options.setPerspective(savedPerspective);
            savedPerspective = null;
        }
        viewActive = false;
        activeViewpoint = null;
        transitionActive = false;
        velForward = 0.0;
        velStrafe = 0.0;
        velVertical = 0.0;
    }

    /**
     * 把「可见书签的新顺序」写回完整列表并落盘。
     * <p>
     * 分组筛选生效时可见的只是子集，所以做法是：取出这些书签在完整列表里原本占用的槽位，
     * 再把它们按新顺序填回去 —— 未显示的书签保持原位不动。
     */
    public void reorderVisible(List<Viewpoint> visibleOrder) {
        if (visibleOrder == null || visibleOrder.isEmpty()) {
            return;
        }
        // 按「对象身份」判断哪些槽位属于本次被移动的元素：Viewpoint 没有覆写 equals，
        // 这里刻意保持身份语义，避免两个字段恰好相同的书签互相误判。
        Set<Viewpoint> moved = Collections.newSetFromMap(new IdentityHashMap<>());
        moved.addAll(visibleOrder);

        List<Integer> slots = new ArrayList<>(visibleOrder.size());
        for (int i = 0; i < viewpoints.size(); i++) {
            if (moved.contains(viewpoints.get(i))) {
                slots.add(i);
            }
        }
        if (slots.size() != visibleOrder.size()) {
            // 有元素已经不在列表里（例如期间被删除），放弃这次重排
            return;
        }
        for (int i = 0; i < slots.size(); i++) {
            viewpoints.set(slots.get(i), visibleOrder.get(i));
        }
        saveViewpoints();
    }

    /**
     * 断线 / 退出世界时调用：还原视角并彻底清空自由视角状态。
     * <p>
     * 若不做这一步，单例状态会跨世界残留：进入下一个世界时相机被 {@code CameraMixin} 锁在旧坐标
     * （可能还是另一个维度），同时移动输入被清零，角色动不了。
     */
    public void clearViewState() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (savedPerspective != null) {
            client.options.setPerspective(savedPerspective);
            savedPerspective = null;
        }
        viewActive = false;
        activeViewpoint = null;
        transitionActive = false;

        velForward = 0.0;
        velStrafe = 0.0;
        velVertical = 0.0;

        prevX = 0.0;
        prevY = 0.0;
        prevZ = 0.0;
        freeX = 0.0;
        freeY = 0.0;
        freeZ = 0.0;
        freeYaw = 0.0f;
        freePitch = 0.0f;
    }
}
