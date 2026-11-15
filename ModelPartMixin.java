package com.example.knifeclient.mixin;

import com.example.knifeclient.ModState;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Skips drawing (headless / korblox hidden), recolours (korblox tinted) and scales (head size)
 * parts of the local player.
 */
@Mixin(ModelPart.class)
public abstract class ModelPartMixin {
    @Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V",
            at = @At("HEAD"), cancellable = true)
    private void knifeclient$hide(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, int color, CallbackInfo ci) {
        if (!ModState.renderingSelf) return;
        ModelPart self = (ModelPart) (Object) this;
        if (ModState.isHidden(self)) {
            ci.cancel();
            return;
        }
        float s = ModState.scaleFor(self);
        if (s != 1f) {
            self.xScale = s;
            self.yScale = s;
            self.zScale = s;
        }
    }

    @Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V",
            at = @At("RETURN"))
    private void knifeclient$unscale(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, int color, CallbackInfo ci) {
        if (!ModState.renderingSelf) return;
        ModelPart self = (ModelPart) (Object) this;
        if (ModState.scaleFor(self) != 1f) {
            self.xScale = 1f;
            self.yScale = 1f;
            self.zScale = 1f;
        }
    }

    @ModifyVariable(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V",
            at = @At("HEAD"), argsOnly = true, index = 5)
    private int knifeclient$tint(int color) {
        if (ModState.renderingSelf && ModState.isTinted((ModelPart) (Object) this)) {
            return ModState.tintColor;
        }
        return color;
    }
}
