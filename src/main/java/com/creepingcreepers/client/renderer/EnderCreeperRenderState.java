package com.creepingcreepers.client.renderer;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/**
 * Render state for Ender Creeper.
 * Stores data needed for rendering effects like swelling and flash.
 */
public class EnderCreeperRenderState extends LivingEntityRenderState {
    /** Swelling progress (0.0 to 1.0) for scale and flash effects */
    public float swelling;

    /** White overlay progress for the flash effect (0.0 to 1.0) */
    public float whiteOverlayProgress;
}
