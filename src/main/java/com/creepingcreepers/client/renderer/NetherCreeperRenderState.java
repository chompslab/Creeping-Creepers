/*
 * ============================================================================
 * NetherCreeperRenderState.java
 * ============================================================================
 *
 * WHAT THIS FILE DOES:
 * Holds rendering data for the Nether Creeper that needs to be passed
 * from the entity to the renderer each frame.
 *
 * WHY IT EXISTS:
 * In Minecraft 1.21+, renderers use a separate "render state" object
 * to pass data from the entity to the renderer. This decouples the
 * rendering logic from the entity logic.
 *
 * @author Chompslabs
 * @version 1.0.0
 */
package com.creepingcreepers.client.renderer;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/**
 * Render state for the Nether Creeper.
 * Contains data needed for rendering animations and temperature-based effects.
 */
public class NetherCreeperRenderState extends LivingEntityRenderState {

    /**
     * The current swell amount (0 to 1).
     * Used for the scaling animation before explosion.
     */
    public float swelling;

    /**
     * Whether the creeper is currently cold (out of lava).
     * Used to determine which texture to use and whether to shiver.
     */
    public boolean isCold;

    /**
     * Counter for shivering animation when cold.
     * Increments each tick while cold, used for shake effect.
     */
    public int shiveringTicks;

    /** White overlay progress for the flash effect (0.0 to 1.0) */
    public float whiteOverlayProgress;
}
