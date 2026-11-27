package dev.quickview.mixin;

import dev.quickview.QuickViewManager;
//? if >=26.3 {
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
//?} else {
import net.minecraft.client.renderer.ItemInHandRenderer;
//?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >=26.3 {
@Mixin(FirstPersonHandsAndItemsRenderer.class)
//?} else {
@Mixin(ItemInHandRenderer.class)
//?}
public abstract class HeldItemRendererMixin {

    @Inject(method = "renderHandsWithItems", at = @At("HEAD"), cancellable = true)
    private void quickview$hideHeldItem(CallbackInfo ci) {
        if (QuickViewManager.getInstance().isNativeViewActive()) {
            ci.cancel();
        }
    }
}
