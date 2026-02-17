/*
 * ============================================================================
 * EnderCreeperEyesLayer.java
 * ============================================================================
 *
 * WHAT THIS FILE DOES:
 * Renders the glowing eyes layer for the Ender Creeper, similar to how
 * Enderman eyes glow in the dark.
 *
 * WHY IT EXISTS:
 * To give the Ender Creeper the distinctive glowing eyes effect that
 * Endermen have, making it feel like a true hybrid creature.
 *
 * HOW IT WORKS:
 * Uses a separate eyes texture rendered with full brightness (emissive)
 * so the eyes glow regardless of ambient light level.
 *
 * @author Chompslabs
 * @version 1.0.0
 */
package com.creepingcreepers.client.renderer;

import com.creepingcreepers.CreepingCreepersMod;
import com.creepingcreepers.client.model.EnderCreeperModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Render layer that draws glowing eyes on the Ender Creeper.
 *
 * The eyes are rendered using the EYES render type which ignores
 * lighting calculations, making them appear to glow in the dark.
 */
@OnlyIn(Dist.CLIENT)
public class EnderCreeperEyesLayer extends EyesLayer<EnderCreeperRenderState, EnderCreeperModel> {

    /**
     * The render type for the glowing eyes overlay.
     */
    private static final RenderType ENDER_CREEPER_EYES = RenderTypes.eyes(
            Identifier.fromNamespaceAndPath(CreepingCreepersMod.MOD_ID, "textures/entity/ender_creeper_eyes.png")
    );

    /**
     * Creates a new eyes layer.
     *
     * @param renderer The parent renderer
     */
    public EnderCreeperEyesLayer(RenderLayerParent<EnderCreeperRenderState, EnderCreeperModel> renderer) {
        super(renderer);
    }

    /**
     * Returns the render type for the glowing eyes.
     *
     * @return The eyes render type
     */
    @Override
    public RenderType renderType() {
        return ENDER_CREEPER_EYES;
    }
}
