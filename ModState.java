package com.example.knifeclient;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.PlayerEntityModel;

/** Tracks which model parts to hide/tint/scale while the local player is being drawn. */
public final class ModState {
    public static boolean nextIsSelf = false;
    public static boolean renderingSelf = false;
    public static int tintColor = 0xFFFFFFFF;

    private static ModelPart head, hat, rightLeg, rightPants, tintLeg, tintPants;
    private static ModelPart scaleHead, scaleHat;
    private static float headScale = 1f;

    public static void begin(PlayerEntityModel model) {
        end();
        renderingSelf = nextIsSelf;
        if (!renderingSelf) return;

        if (Modules.HEADLESS.enabled) {
            head = model.head;
            hat = Modules.HEADLESS_HAT.value ? model.hat : null;
        } else if (Modules.HEADSIZE.enabled) {
            // The game resets every part's scale to 1.0 while it poses the model, so scaling here
            // (before the pose) does nothing. We only remember which parts to scale; the scale is
            // applied in ModelPartMixin right before each part is drawn.
            scaleHead = model.head;
            scaleHat = model.hat;
            headScale = (float) Modules.HEAD_SIZE.value;
        }

        if (Modules.KORBLOX.enabled) {
            if (Modules.KORBLOX_MODE.index == 0) {
                rightLeg = model.rightLeg;
                rightPants = model.rightPants;
            } else {
                tintLeg = model.rightLeg;
                tintPants = model.rightPants;
                tintColor = 0xFF000000 | Modules.KORBLOX_COLOR.rgb(0.0);
            }
        }
    }

    public static void end() {
        renderingSelf = false;
        head = hat = rightLeg = rightPants = tintLeg = tintPants = null;
        scaleHead = scaleHat = null;
        headScale = 1f;
    }

    public static boolean isHidden(ModelPart part) {
        return part == head || part == hat || part == rightLeg || part == rightPants;
    }

    public static boolean isTinted(ModelPart part) {
        return part == tintLeg || part == tintPants;
    }

    /** Scale to apply to this part right now (1.0 = leave alone). */
    public static float scaleFor(ModelPart part) {
        return (part != null && (part == scaleHead || part == scaleHat)) ? headScale : 1f;
    }
}
