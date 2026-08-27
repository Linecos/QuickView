package dev.quickview;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.PlayerInput;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class QuickViewManager {
    private static final QuickViewManager INSTANCE = new QuickViewManager();
    private static final double FLY_SPEED = 0.25;
    private static final double FLY_SPEED_FAST = 0.6;

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

    public void applyFreecamLook(double cursorDeltaX, double cursorDeltaY) {
        if (!viewActive || !freeMoveEnabled) return;

        freeYaw += (float) (cursorDeltaX * 0.15);
        freePitch += (float) (cursorDeltaY * 0.15);
        freePitch = Math.max(-90.0f, Math.min(90.0f, freePitch));
    }

    public void applyFreecamMovement(PlayerInput input) {
        if (!viewActive || !freeMoveEnabled) return;

        double forward = (input.forward() ? 1.0 : 0.0) - (input.backward() ? 1.0 : 0.0);
        double strafe = (input.right() ? 1.0 : 0.0) - (input.left() ? 1.0 : 0.0);
        double vertical = (input.jump() ? 1.0 : 0.0) - (input.sneak() ? 1.0 : 0.0);

        if (forward == 0.0 && strafe == 0.0 && vertical == 0.0) return;

        double horizontalLength = Math.sqrt(forward * forward + strafe * strafe);
        if (horizontalLength > 0.0) {
            forward /= horizontalLength;
            strafe /= horizontalLength;
        }

        double yaw = Math.toRadians(freeYaw);
        double pitch = Math.toRadians(freePitch);
        double cosYaw = Math.cos(yaw);
        double sinYaw = Math.sin(yaw);
        double cosPitch = Math.cos(pitch);
        double sinPitch = Math.sin(pitch);

        double fx = -sinYaw * cosPitch;
        double fy = -sinPitch;
        double fz = cosYaw * cosPitch;
        double rx = -cosYaw;
        double rz = -sinYaw;

        double speed = input.sprint() ? FLY_SPEED_FAST : FLY_SPEED;

        freeX += (fx * forward + rx * strafe) * speed;
        freeY += (fy * forward + vertical) * speed;
        freeZ += (fz * forward + rz * strafe) * speed;
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
    }

    public void restore() {
        viewActive = false;
        activeViewpoint = null;
    }
}
