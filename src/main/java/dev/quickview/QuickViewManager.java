package dev.quickview;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class QuickViewManager {
    private static final QuickViewManager INSTANCE = new QuickViewManager();
    private List<Viewpoint> viewpoints = new ArrayList<>();
    private Viewpoint activeViewpoint;
    private String currentDimension = "";
    private boolean viewActive = false;

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
    }

    public void restore() {
        viewActive = false;
        activeViewpoint = null;
    }
}
