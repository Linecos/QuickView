package dev.quickview.mixin;

import dev.quickview.QuickViewManager;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends Input {

    @Inject(method = "tick", at = @At("TAIL"))
    private void quickview$zeroMovement(CallbackInfo ci) {
        if (!QuickViewManager.getInstance().isViewActive()) return;
        this.playerInput = PlayerInput.DEFAULT;
        this.movementVector = new Vec2f(0.0f, 0.0f);
    }
}
