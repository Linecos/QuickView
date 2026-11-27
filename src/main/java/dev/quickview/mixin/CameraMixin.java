package dev.quickview.mixin;

import dev.quickview.QuickViewManager;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {

    @Shadow
    protected abstract void setPosition(double x, double y, double z);

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Shadow
    private boolean detached;

    //? if >=26.1 {
    @Inject(
            method = "update",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Camera;alignWithEntity(F)V",
                    shift = At.Shift.AFTER
            )
    )
    private void quickview$overrideViewpoint(net.minecraft.client.DeltaTracker deltaTracker, CallbackInfo ci) {
        quickview$applyOverride(deltaTracker.getGameTimeDeltaPartialTick(false));
    }
    //?} else if >=1.21.11 {
    @Inject(method = "setup", at = @At("TAIL"))
    private void quickview$overrideViewpoint(Level world, Entity entity, boolean thirdPerson, boolean inverseView, float tickProgress, CallbackInfo ci) {
        quickview$applyOverride(tickProgress);
    }
    //?} else {
    @Inject(method = "setup", at = @At("TAIL"))
    private void quickview$overrideViewpoint(net.minecraft.world.level.BlockGetter world, Entity entity, boolean thirdPerson, boolean inverseView, float tickProgress, CallbackInfo ci) {
        quickview$applyOverride(tickProgress);
    }
    //?}

    private void quickview$applyOverride(float tickProgress) {
        QuickViewManager manager = QuickViewManager.getInstance();
        if (!manager.isNativeViewActive()) return;

        // 平滑过渡：先把插值推进到当前时刻，再取坐标
        manager.updateTransition();

        double t = tickProgress;
        double x = manager.getPrevFreeX() + (manager.getFreeX() - manager.getPrevFreeX()) * t;
        double y = manager.getPrevFreeY() + (manager.getFreeY() - manager.getPrevFreeY()) * t;
        double z = manager.getPrevFreeZ() + (manager.getFreeZ() - manager.getPrevFreeZ()) * t;
        float yaw = manager.getFreeYaw();
        float pitch = manager.getFreePitch();

        Minecraft client = Minecraft.getInstance();
        CameraType perspective = client.options.getCameraType();
        boolean showBody = !perspective.isFirstPerson();

        if (showBody) {
            double distance = 4.0;
            double yawRad = Math.toRadians(yaw);
            double pitchRad = Math.toRadians(pitch);
            double cosPitch = Math.cos(pitchRad);
            double forwardX = -Math.sin(yawRad) * cosPitch;
            double forwardY = -Math.sin(pitchRad);
            double forwardZ = Math.cos(yawRad) * cosPitch;

            if (perspective.isMirrored()) {
                x += forwardX * distance;
                y += forwardY * distance;
                z += forwardZ * distance;
                yaw += 180.0f;
                pitch = -pitch;
            } else {
                x -= forwardX * distance;
                y -= forwardY * distance;
                z -= forwardZ * distance;
            }
        }

        this.setPosition(x, y, z);
        this.setRotation(yaw, pitch);
        this.detached = true;
    }
}