/*
 * ============================================================================
 * NetherCreeperRenderer.java
 * ============================================================================
 *
 * WHAT THIS FILE DOES:
 * Handles the rendering of the Nether Creeper entity. This includes:
 * - Applying the appropriate texture (warm or cold)
 * - Applying the model
 * - Handling the swelling animation (scale and color flash)
 * - Handling the shivering effect when cold
 *
 * WHY IT EXISTS:
 * Every entity needs a renderer to be visible in-game. The renderer
 * connects the entity logic to its visual representation.
 *
 * SPECIAL EFFECTS:
 * - Swelling: Entity scales up before explosion
 * - Texture swap: Different texture when cold vs warm
 * - Shivering: Entity shakes when cold (position offset)
 *
 * @author Chompslabs
 * @version 1.0.0
 */
package com.creepingcreepers.client.renderer;

import com.creepingcreepers.CreepingCreepersMod;
import com.creepingcreepers.client.model.NetherCreeperModel;
import com.creepingcreepers.entity.nethercreeper.NetherCreeperEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Renderer for the Nether Creeper entity.
 *
 * Handles texture binding (warm/cold), model rendering, swelling animation,
 * and shivering effect when cold.
 */
public class NetherCreeperRenderer extends MobRenderer<NetherCreeperEntity, NetherCreeperRenderState, NetherCreeperModel> {

    /**
     * The texture location for the warm (in lava) Nether Creeper.
     */
    private static final Identifier TEXTURE_WARM =
            Identifier.fromNamespaceAndPath(CreepingCreepersMod.MOD_ID, "textures/entity/nether_creeper.png");

    /**
     * The texture location for the cold (out of lava) Nether Creeper.
     */
    private static final Identifier TEXTURE_COLD =
            Identifier.fromNamespaceAndPath(CreepingCreepersMod.MOD_ID, "textures/entity/nether_creeper_cold.png");

    /**
     * Creates a new renderer.
     *
     * @param context The renderer provider context
     */
    public NetherCreeperRenderer(EntityRendererProvider.Context context) {
        super(context, new NetherCreeperModel(context.bakeLayer(NetherCreeperModel.LAYER_LOCATION)), 0.5F);
    }

    /**
     * Creates a new render state instance.
     *
     * @return A new NetherCreeperRenderState
     */
    @Override
    public NetherCreeperRenderState createRenderState() {
        return new NetherCreeperRenderState();
    }

    /**
     * Extracts render data from the entity into the render state.
     *
     * @param entity The entity being rendered
     * @param state The render state to populate
     * @param partialTick Partial tick for interpolation
     */
    @Override
    public void extractRenderState(NetherCreeperEntity entity, NetherCreeperRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        // Copy swelling value for animation
        state.swelling = entity.getSwelling(partialTick);
        // Copy temperature state for texture selection and shivering
        state.isCold = entity.isCold();
        state.shiveringTicks = entity.getShiveringTicks();

        // Calculate white overlay progress for flash effect (vanilla creeper behavior)
        float swelling = state.swelling;
        int flashTick = (int) (swelling * 10.0F) % 2;
        state.whiteOverlayProgress = (flashTick == 0) ? 0.0F : Mth.clamp(swelling, 0.5F, 1.0F);
    }

    /**
     * Gets the texture location based on temperature state.
     *
     * @param state The render state
     * @return The texture identifier (warm or cold)
     */
    @Override
    public Identifier getTextureLocation(NetherCreeperRenderState state) {
        return state.isCold ? TEXTURE_COLD : TEXTURE_WARM;
    }

    /**
     * Applies scaling and shivering transformations.
     *
     * @param state The render state
     * @param poseStack The pose stack for transformations
     */
    /**
     * Returns the white overlay progress for the flash effect.
     * This makes the creeper flash white when about to explode.
     */
    @Override
    protected float getWhiteOverlayProgress(NetherCreeperRenderState state) {
        return state.whiteOverlayProgress;
    }

    @Override
    protected void scale(NetherCreeperRenderState state, PoseStack poseStack) {
        // Swelling animation (same as vanilla creeper)
        float swelling = state.swelling;
        float scale = 1.0F + Mth.sin(swelling * 100.0F) * swelling * 0.01F;
        scale += swelling * 0.3F;
        poseStack.scale(scale, scale, scale);

        // Shivering effect when cold
        if (state.isCold) {
            // Small random-looking offset based on shivering ticks
            float shiverX = Mth.sin(state.shiveringTicks * 1.3F) * 0.02F;
            float shiverZ = Mth.cos(state.shiveringTicks * 1.7F) * 0.02F;
            poseStack.translate(shiverX, 0, shiverZ);
        }
    }
}
