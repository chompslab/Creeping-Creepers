/*
 * ============================================================================
 * WitherCreeperRenderer.java
 * ============================================================================
 *
 * WHAT THIS FILE DOES:
 * Handles the rendering of the Wither Creeper entity. This includes:
 * - Applying the texture
 * - Applying the model
 * - Handling the swelling animation (scale and color flash)
 *
 * WHY IT EXISTS:
 * Every entity needs a renderer to be visible in-game. The renderer
 * connects the entity logic to its visual representation.
 *
 * RENDERING PIPELINE:
 * 1. Forge calls render() each frame for visible entities
 * 2. We apply transformations (position, rotation, scale)
 * 3. We bind the texture
 * 4. We render the model
 *
 * SPECIAL EFFECTS:
 * - Swelling: Entity scales up before explosion
 *
 * @author Chompslabs
 * @version 1.0.0
 */
package com.creepingcreepers.client.renderer;

import com.creepingcreepers.CreepingCreepersMod;
import com.creepingcreepers.client.model.WitherCreeperModel;
import com.creepingcreepers.entity.withercreeper.WitherCreeperEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Renderer for the Wither Creeper entity.
 *
 * Handles texture binding, model rendering, and swelling animation.
 */
public class WitherCreeperRenderer extends MobRenderer<WitherCreeperEntity, WitherCreeperRenderState, WitherCreeperModel> {

    /**
     * The texture location for the Wither Creeper.
     */
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(CreepingCreepersMod.MOD_ID, "textures/entity/wither_creeper.png");

    /**
     * Creates a new renderer.
     *
     * @param context The renderer provider context
     */
    public WitherCreeperRenderer(EntityRendererProvider.Context context) {
        super(context, new WitherCreeperModel(context.bakeLayer(WitherCreeperModel.LAYER_LOCATION)), 0.5F);
    }

    /**
     * Creates a new render state instance.
     *
     * @return A new WitherCreeperRenderState
     */
    @Override
    public WitherCreeperRenderState createRenderState() {
        return new WitherCreeperRenderState();
    }

    /**
     * Extracts render data from the entity into the render state.
     *
     * @param entity The entity being rendered
     * @param state The render state to populate
     * @param partialTick Partial tick for interpolation
     */
    @Override
    public void extractRenderState(WitherCreeperEntity entity, WitherCreeperRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        // Copy swelling value for animation
        state.swelling = entity.getSwelling(partialTick);

        // Calculate white overlay progress for flash effect (vanilla creeper behavior)
        float swelling = state.swelling;
        int flashTick = (int) (swelling * 10.0F) % 2;
        state.whiteOverlayProgress = (flashTick == 0) ? 0.0F : Mth.clamp(swelling, 0.5F, 1.0F);
    }

    /**
     * Gets the texture location for the entity.
     *
     * @param state The render state
     * @return The texture identifier
     */
    @Override
    public Identifier getTextureLocation(WitherCreeperRenderState state) {
        return TEXTURE;
    }

    /**
     * Applies scaling transformations based on swell state.
     *
     * @param state The render state
     * @param poseStack The pose stack for transformations
     */
    /**
     * Returns the white overlay progress for the flash effect.
     * This makes the creeper flash white when about to explode.
     */
    @Override
    protected float getWhiteOverlayProgress(WitherCreeperRenderState state) {
        return state.whiteOverlayProgress;
    }

    @Override
    protected void scale(WitherCreeperRenderState state, PoseStack poseStack) {
        float swelling = state.swelling;
        float scale = 1.0F + Mth.sin(swelling * 100.0F) * swelling * 0.01F;
        scale += swelling * 0.3F;
        poseStack.scale(scale, scale, scale);
    }
}
