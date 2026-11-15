package com.example.knifeclient.mixin;

import com.example.knifeclient.KnifeAnim;
import com.example.knifeclient.Knives;
import com.example.knifeclient.Modules;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** First person: draw a knife in your empty main hand instead of the bare fist. */
@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {
    @Shadow
    protected abstract void renderFirstPersonItem(AbstractClientPlayerEntity player, float tickProgress, float pitch,
                                                  Hand hand, float swingProgress, ItemStack item, float equipProgress,
                                                  MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light);

    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"), cancellable = true)
    private void knifeclient$knife(AbstractClientPlayerEntity player, float tickProgress, float pitch, Hand hand,
                                   float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices,
                                   VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (Modules.KNIVES.enabled && hand == Hand.MAIN_HAND && item.isEmpty() && !player.isInvisible()) {
            Knives.frame();
            // with Custom Swing on, we draw the swing ourselves, so the game gets 0 for its own swing
            float gameSwing = Modules.KNIVES_ANIM.value && Modules.KNIVES_SWING.value ? 0f : swingProgress;
            matrices.push();
            KnifeAnim.apply(matrices, Knives.index(), swingProgress, player);
            this.renderFirstPersonItem(player, tickProgress, pitch, hand, gameSwing, Knives.current(),
                    equipProgress, matrices, vertexConsumers, light);
            matrices.pop();
            ci.cancel();
        }
    }
}
