package dev.quickview;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.PlayerInput;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class QuickViewManager {
    private static final QuickViewManager INSTANCE = new QuickViewManager();
    private static final double MOVE_RAMP = 0.15;
    private static final double MOVE_DECELERATION = 0.4;
    private static final double MOVE_SPEED = 0.7;
    private static final double SPRINT_MULTIPLIER = 3.0;

    private List<Viewpoint> viewpoints = new ArrayList<>();
    private Viewpoint activeViewpoint;
    private String currentDimension = "";
    private boolean viewActive = false;
    private boolean freeMoveEnabled = true;

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

    private QuickViewManager() {
    }

    public static QuickViewManager getInstance() {
        return INSTANCE;
    }

    public boolean isViewActive() {
        return viewActive;
    }

    public Viewpoint getActiveViewpoint() {
        return activeViewpoint;
    }

    public boolean isFreeMoveEnabled() {
        return freeMoveEnabled;
    }

    public void toggleFreeMove() {
        freeMoveEnabled = !freeMoveEnabled;
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
        if (!viewActive || !freeMoveEnabled) return;

        freeYaw += (float) (cursorDeltaX * 0.15);
        freePitch += (float) (cursorDeltaY * 0.15);
        freePitch = Math.max(-90.0f, Math.min(90.0f, freePitch));
    }

    public void applyFreecamMovement(PlayerInput input) {
        if (!viewActive) return;

        int forward = 0;
        int strafe = 0;
        int vertical = 0;

        if (freeMoveEnabled) {
            if (input.forward()) forward += 1;
            if (input.backward()) forward -= 1;
            if (input.right()) strafe += 1;
            if (input.left()) strafe -= 1;
            if (input.jump()) vertical += 1;
            if (input.sneak()) vertical -= 1;
        }

        boolean sprint = freeMoveEnabled && input.sprint();

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

    public void loadViewpoints() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld world = client.world;
        if (world == null) return;

        RegistryKey<World> dimKey = world.getRegistryKey();
        Identifier dimId = dimKey.getValue();
        currentDimension = dimId.toString();
        viewpoints = ViewpointStorage.load(currentDimension);
    }

    public void saveViewpoints() {
        ViewpointStorage.save(currentDimension, viewpoints);
    }

    public void addViewpoint(String name) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        double x = player.getX();
        double y = player.getY() + player.getStandingEyeHeight();
        double z = player.getZ();
        float yaw = player.getYaw();
        float pitch = player.getPitch();

        Viewpoint vp = new Viewpoint(name, currentDimension, x, y, z, yaw, pitch);
        viewpoints.add(vp);
        saveViewpoints();
    }

    public Viewpoint createViewpoint(String name) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) return null;

        double x = player.getX();
        double y = player.getY() + player.getStandingEyeHeight();
        double z = player.getZ();
        float yaw = player.getYaw();
        float pitch = player.getPitch();

        Viewpoint vp = new Viewpoint(name, currentDimension, x, y, z, yaw, pitch);
        viewpoints.add(vp);
        saveViewpoints();
        return vp;
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

        activeViewpoint = vp;
        viewActive = true;
        freeX = vp.getX();
        freeY = vp.getY();
        freeZ = vp.getZ();
        freeYaw = vp.getYaw();
        freePitch = vp.getPitch();
        prevX = freeX;
        prevY = freeY;
        prevZ = freeZ;
        velForward = 0.0;
        velStrafe = 0.0;
        velVertical = 0.0;

        savedPerspective = client.options.getPerspective();
        client.options.setPerspective(Perspective.FIRST_PERSON);
    }

    public void restore() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (savedPerspective != null) {
            client.options.setPerspective(savedPerspective);
            savedPerspective = null;
        }
        viewActive = false;
        activeViewpoint = null;
        velForward = 0.0;
        velStrafe = 0.0;
        velVertical = 0.0;
    }
}
