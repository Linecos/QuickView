package dev.quickview.mixin;

import dev.quickview.QuickViewManager;
import dev.quickview.Viewpoint;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {

    @Shadow
    protected abstract void setPos(double x, double y, double z);

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Shadow
    private boolean thirdPerson;

    @Inject(method = "update", at = @At("TAIL"))
    private void quickview$overrideViewpoint(World world, Entity entity, boolean thirdPerson, boolean inverseView, float tickProgress, CallbackInfo ci) {
        QuickViewManager manager = QuickViewManager.getInstance();
        if (!manager.isViewActive()) return;

        Viewpoint vp = manager.getActiveViewpoint();
        if (vp == null) return;

        this.setPos(vp.getX(), vp.getY(), vp.getZ());
        this.setRotation(vp.getYaw(), vp.getPitch());
        this.thirdPerson = true;
    }
}
