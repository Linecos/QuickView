package dev.quickview.mixin;

import dev.quickview.QuickViewManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    private void quickview$cancelLookChange(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci) {
        QuickViewManager manager = QuickViewManager.getInstance();
        if (!manager.isNativeViewActive()) return;

        Entity self = (Entity) (Object) this;
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && self == client.player) {
            manager.applyFreecamLook(cursorDeltaX, cursorDeltaY);
            ci.cancel();
        }
    }
}
