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
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
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
    /**
     * 视角由 Tweakeroo 的自由相机接管中（{@link #viewActive} 保持 false —— 那套状态、混入和
     * 自带自由移动都不参与，QuickView 只负责把相机摆到书签位置）。
     */
    private boolean tweakerooViewActive = false;

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

    /**
     * 上一 tick 的生命值 / 吸收量，用于识别「受到伤害」。
     * {@code NaN} = 尚无基线（刚进世界或刚切世界），此时不判伤害，避免拿旧世界的血量误判。
     */
    private float lastHealth = Float.NaN;
    private float lastAbsorption = Float.NaN;

    private QuickViewManager() {
    }

    public static QuickViewManager getInstance() {
        return INSTANCE;
    }

    /** 从 config/quickview.json 读取设置，应在客户端初始化时调用一次。 */
    public void loadConfig() {
        this.config = QuickViewConfig.load();
        // 配置里开着「用 Tweakeroo 灵魂出窍」但当前 Tweakeroo 不可用（没装 / 版本对不上）：
        // 直接归零，避免设置页出现一个「灰着又是开」的开关 —— 那种状态在界面上关不掉，只能改文件。
        // 功能本身不受影响：所有调用点都还要求 TweakerooFreecam.isAvailable()，否则回落自带实现
        if (config.isUseTweakerooFreecam() && !TweakerooFreecam.isAvailable()) {
            QuickViewClient.LOGGER.warn("配置启用了 Tweakeroo 自由相机，但 Tweakeroo 不可用，已关闭该项");
            config.setUseTweakerooFreecam(false);
            config.save();
        }
    }

    public boolean isViewActive() {
        // 使用 Tweakeroo 实现时 QuickView 自己的 viewActive 始终为 false（不接管相机），
        // 但对外的「有没有在视角里」必须是 true —— B 键恢复、受伤恢复、设置页提示都靠它。
        // ⚠️ 混入层（CameraMixin 等）**必须用 isNativeViewActive()**：它们要的是「自带相机是否在接管」，
        // 用这个合并值会把 Tweakeroo 的相机覆盖到 freeX/Y/Z（此时全为 0）—— 表现就是切过去一片空白
        return viewActive || tweakerooViewActive;
    }

    /**
     * QuickView <b>自带的</b>自由视角是否在接管相机。
     * <p>只给混入层与自带实现的内部逻辑用；UI/按键请用 {@link #isViewActive()}。
     */
    public boolean isNativeViewActive() {
        return viewActive;
    }

    /** 当前这次视角是不是交给 Tweakeroo 的自由相机在管（用于按键提示措辞）。 */
    public boolean isUsingTweakerooFreecam() {
        return tweakerooViewActive;
    }

    /** 设置项是否选了 Tweakeroo 实现（不代表当前真的接通了）。 */
    public boolean isTweakerooFreecamSelected() {
        return config.isUseTweakerooFreecam();
    }

    /** Tweakeroo 是否可用（装了且反射签名对得上）；设置页据此禁用开关。 */
    public boolean isTweakerooFreecamAvailable() {
        return TweakerooFreecam.isAvailable();
    }

    public void toggleUseTweakerooFreecam() {
        config.setUseTweakerooFreecam(!config.isUseTweakerooFreecam());
        config.save();
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

    public boolean isRestoreOnDamage() {
        return config.isRestoreOnDamage();
    }

    public void toggleRestoreOnDamage() {
        config.setRestoreOnDamage(!config.isRestoreOnDamage());
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
        // Tweakeroo 会自己关掉自由相机（典型场景：换维度时它强制 setBooleanValue(false)），
        // 这里把标志同步回来，避免 QuickView 一直以为还在视角里（B 键 / 恢复按钮 / 受伤恢复都看 isViewActive()）。
        // isEnabled() 只在确实处于 Tweakeroo 接管时才调用，开销可忽略。
        if (tweakerooViewActive && !TweakerooFreecam.isEnabled()) {
            tweakerooViewActive = false;
            activeViewpoint = null;
        }
        if (!viewActive) return;

        prevX = freeX;
        prevY = freeY;
        prevZ = freeZ;
    }

    /**
     * 每客户端 tick 调用（<b>不受 {@code viewActive} 限制</b>）：维护生命/吸收量基线，
     * 一旦发现减少就认为「受到伤害」；若此时正在自由视角且开关打开，则立即 {@link #restore()}。
     *
     * <p>为什么不只在自由视角时记录：进入自由视角的那一刻没有基线，第一帧的伤害会被漏掉；
     * 全时段记录才能保证「进入自由视角前刚受过伤」不会被当成新伤害（血量已在那之前记过低值）。
     * <p>吸收量也要看：有吸收心时受击不掉血、只掉吸收量，只看 {@code getHealth()} 会漏判。
     * <p>恢复时机是客户端 tick（伤害经由网络包同步到血量），不引入新 mixin。
     *
     * @return 本次是否因受伤触发了恢复（调用方据此决定要不要把界面也关掉 ——
     *         菜单开着时移动输入被拦，玩家会「回到本体但动不了」）
     */
    public boolean tickDamageWatch() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            lastHealth = Float.NaN;
            lastAbsorption = Float.NaN;
            return false;
        }

        float health = player.getHealth();
        float absorption = player.getAbsorptionAmount();
        boolean hasBaseline = !Float.isNaN(lastHealth);
        boolean damaged = hasBaseline && (health < lastHealth || absorption < lastAbsorption);

        lastHealth = health;
        lastAbsorption = absorption;

        // 用 isViewActive()：Tweakeroo 接管时 viewActive 是 false，但受伤同样要能恢复
        if (damaged && isViewActive() && config.isRestoreOnDamage()) {
            restore();
            return true;
        }
        return false;
    }

    public void applyFreecamLook(double cursorDeltaX, double cursorDeltaY) {
        if (!viewActive || !config.isFreeMoveEnabled() || transitionActive) return;

        // 偏航角每次累加后归一化到 [-180,180)：不归一的话长时间自由视角会把 freeYaw 累加到很大，
        // 一是 Math.toRadians 的精度随幅度下降，二是快照存进 JSON 的 Yaw 会显示成 4321.0 这类怪值。
        freeYaw = MathHelper.wrapDegrees(freeYaw + (float) (cursorDeltaX * 0.15));
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

    /** 按出现顺序收集所有非空分组名（主界面筛选下拉与编辑页分组候选共用）。 */
    public List<String> getGroups() {
        // LinkedHashSet：既去重（O(1) contains，原 List.contains 是 O(N²)）又保持首次出现顺序
        Set<String> groups = new LinkedHashSet<>();
        for (Viewpoint vp : viewpoints) {
            String group = vp.getGroup();
            if (!group.isEmpty()) {
                groups.add(group);
            }
        }
        return new ArrayList<>(groups);
    }

    public String getCurrentDimension() {
        return currentDimension;
    }

    public String getCurrentContext() {
        return currentContext;
    }

    /**
     * 若当前世界的「上下文 / 维度」与已加载的不一致，则从磁盘重新加载，返回是否发生了重载。
     *
     * <p>换服务器/存档（上下文变化）或换维度都会改变存储文件路径，必须重载；
     * 而<b>同一上下文内的本地增删改都已直接作用于内存列表并落盘</b>，无需再读盘 ——
     * 这样既省掉每次编辑/删除/克隆的一次磁盘 IO，也消除了「重载换对象、旧引用成孤儿」的隐患。
     */
    public boolean ensureContextCurrent() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld world = client.world;
        if (world == null) return false;

        RegistryKey<World> dimKey = world.getRegistryKey();
        Identifier dimId = dimKey.getValue();
        String dimension = dimId.toString();
        String context = resolveContext(client);

        if (dimension.equals(currentDimension) && context.equals(currentContext)) {
            return false;
        }

        currentDimension = dimension;
        currentContext = context;
        viewpoints = ViewpointStorage.load(currentContext, currentDimension);
        return true;
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
        createViewpoint(name);
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

    /**
     * 按对象身份移除单个书签并落盘。
     *
     * <p>{@code Viewpoint} 未覆写 {@code equals}，所以列表移除天然是身份语义；调用方须保证传入的对象
     * 仍在当前列表里（编辑页直接持有引用，刷新列表时上下文未变、不会换对象）。
     * <p>批量删除请用 {@link #removeViewpoints(List)}（一次落盘）。
     */
    public void removeViewpoint(Viewpoint vp) {
        if (vp != null && viewpoints.remove(vp)) {
            saveViewpoints();
        }
    }

    /** 批量删除：一次落盘（逐个按身份移除会重复 save）。按对象身份匹配，与拖拽/勾选一致。 */
    public void removeViewpoints(List<Viewpoint> toRemove) {
        if (toRemove == null || toRemove.isEmpty()) {
            return;
        }
        java.util.Set<Viewpoint> set = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        set.addAll(toRemove);
        boolean changed = viewpoints.removeIf(set::contains);
        if (changed) {
            saveViewpoints();
        }
    }

    /**
     * 克隆书签：复制全部字段（含分组），插入到原书签之后并落盘，返回克隆出的新书签；失败返回 {@code null}。
     * <p>克隆名由调用方生成（语言文件里的「%s 副本」）；{@code indexOf} 是身份语义
     * （{@code Viewpoint} 未覆写 equals），传入的对象必须还在当前列表里。
     */
    public Viewpoint cloneViewpointAfter(Viewpoint original, String cloneName) {
        if (original == null) return null;
        int idx = viewpoints.indexOf(original);
        if (idx < 0) return null;
        Viewpoint copy = new Viewpoint(cloneName, original.getDimension(),
                original.getX(), original.getY(), original.getZ(),
                original.getYaw(), original.getPitch());
        copy.setGroup(original.getGroup());
        viewpoints.add(idx + 1, copy);
        saveViewpoints();
        return copy;
    }

    public void switchToViewpoint(Viewpoint vp) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        // 选了 Tweakeroo 实现：把相机整个交给它（它自己管移动、输入与视角恢复）
        if (config.isUseTweakerooFreecam() && TweakerooFreecam.isAvailable()) {
            if (viewActive) {
                // 从自己的自由视角切过去时先退出自带实现，别让两套相机同时生效
                restore();
            }
            if (TweakerooFreecam.enableAt(vp)) {
                tweakerooViewActive = true;
                activeViewpoint = vp;
                return;
            }
            // 它没接住（世界未就绪 / API 不匹配 / 维度对不上）→ 先清掉「Tweakeroo 接管中」的标志，
            // 否则回落到自带实现后会两个标志同时为真，restore() 会先清 Tweakeroo 而漏掉自带相机。
            tweakerooViewActive = false;
        }

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
            freeYaw = MathHelper.wrapDegrees(startYaw);
            freePitch = startPitch;
        } else {
            transitionActive = false;
            freeX = targetX;
            freeY = targetY;
            freeZ = targetZ;
            freeYaw = MathHelper.wrapDegrees(targetYaw);
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
        freeYaw = MathHelper.wrapDegrees(fromYaw + Angles.shortestDelta(fromYaw, targetYaw) * (float) eased);
        freePitch = fromPitch + (targetPitch - fromPitch) * (float) eased;

        prevX = freeX;
        prevY = freeY;
        prevZ = freeZ;

        if (t >= 1.0) {
            transitionActive = false;
            freeYaw = MathHelper.wrapDegrees(targetYaw);
            freePitch = targetPitch;
        }
    }

    public void restore() {
        // Tweakeroo 接管中：关掉它的自由相机（视角恢复、相机复位都由它自己做）。
        // ⚠️ 不在这里早退：万一两个标志同时为真（Tweakeroo 开启失败后回落到自带实现等），
        // 自带相机也必须一起清掉，否则会卡在自带自由视角里、savedPerspective 也回不去。
        if (tweakerooViewActive) {
            TweakerooFreecam.disable();
            tweakerooViewActive = false;
        }
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
        // 换世界时把可能残留的 Tweakeroo 自由相机也收掉（它自己也会在进世界时关，这里是兜底）
        if (tweakerooViewActive) {
            TweakerooFreecam.disable();
            tweakerooViewActive = false;
        }
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

        // 换了世界就要丢掉血量基线：新世界满血 / 旧世界残血会被误判成「受到伤害」
        lastHealth = Float.NaN;
        lastAbsorption = Float.NaN;
    }
}
