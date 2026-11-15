package com.example.knifeclient.mixin;

import com.example.knifeclient.Zoom;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Zoom: narrows the field of view while the zoom key is held.
 *
 * The old version assumed getFov returns a double. In 1.21.2+ it returns a float, so the cast
 * blew up (ClassCastException) the moment you pressed the zoom key. This handles both.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void knifeclient$zoom(CallbackInfoReturnable cir) {
        double f = Zoom.factor();
        if (f <= 1.001) return;
        Object v = cir.getReturnValue();
        if (v instanceof Float fov) {
            cir.setReturnValue((float) (fov / f));
        } else if (v instanceof Double fov) {
            cir.setReturnValue(fov / f);
        }
    }
}
