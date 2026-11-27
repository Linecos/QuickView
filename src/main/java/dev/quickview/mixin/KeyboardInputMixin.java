package dev.quickview.mixin;

import dev.quickview.QuickViewManager;
//? if >=1.21.2 {
import net.minecraft.client.player.ClientInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
//?} else {
import net.minecraft.client.player.Input;
//?}
import net.minecraft.client.player.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
//? if >=1.21.2 {
public abstract class KeyboardInputMixin extends ClientInput {
//?} else {
public abstract class KeyboardInputMixin extends Input {
//?}

    //? if >=1.21.2 {
    @Inject(method = "tick", at = @At("TAIL"))
    private void quickview$zeroMovement(CallbackInfo ci) {
        QuickViewManager manager = QuickViewManager.getInstance();
        if (!manager.isNativeViewActive()) return;

        Input input = this.keyPresses;
        manager.applyFreecamMovement(input);
        this.keyPresses = Input.EMPTY;
        this.moveVector = new Vec2(0.0f, 0.0f);
    }
    //?} else {
    @Inject(method = "tick", at = @At("TAIL"))
    private void quickview$zeroMovement(boolean forward, float sideways, CallbackInfo ci) {
        QuickViewManager manager = QuickViewManager.getInstance();
        if (!manager.isNativeViewActive()) return;

        manager.applyFreecamMovement(this);
        this.forwardImpulse = 0.0f;
        this.leftImpulse = 0.0f;
        this.jumping = false;
        this.shiftKeyDown = false;
        this.up = false;
        this.down = false;
        this.left = false;
        this.right = false;
    }
    //?}
}
