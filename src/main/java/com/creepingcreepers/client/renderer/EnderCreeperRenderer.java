/*
 * ============================================================================
 * EnderCreeperRenderer.java
 * ============================================================================
 * 
 * WHAT THIS FILE DOES:
 * Handles the rendering of the Ender Creeper entity. This includes:
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
 * - Flash: Entity turns white when about to explode
 * - Both effects intensify as explosion approaches
 * 
 * @author Chompslabs
 * @version 1.0.0
 */
package com.creepingcreepers.client.renderer;

import com.creepingcreepers.CreepingCreepersMod;
import com.creepingcreepers.client.model.EnderCreeperModel;
import com.creepingcreepers.entity.endercreeper.EnderCreeperEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class EnderCreeperRenderer extends MobRenderer<EnderCreeperEntity, EnderCreeperRenderState, EnderCreeperModel> {

    /**
     * Cached texture location to avoid allocating a new Identifier every frame.
     */
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(CreepingCreepersMod.MOD_ID, "textures/entity/ender_creeper.png");

    public EnderCreeperRenderer(EntityRendererProvider.Context context) {
        super(context, new EnderCreeperModel(context.bakeLayer(EnderCreeperModel.LAYER_LOCATION)), 0.5F);
        // Add the glowing eyes layer (like Enderman)
        this.addLayer(new EnderCreeperEyesLayer(this));
    }

    @Override
    public EnderCreeperRenderState createRenderState() {
        return new EnderCreeperRenderState();
    }

    @Override
    public void extractRenderState(EnderCreeperEntity entity, EnderCreeperRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        // Copy the swelling value from the entity to the state
        state.swelling = entity.getSwelling(partialTick);

        // Calculate white overlay progress for flash effect
        // Flash alternates rapidly as swelling increases, creating the iconic creeper flash
        float swelling = state.swelling;
        int flashTick = (int) (swelling * 10.0F) % 2;
        state.whiteOverlayProgress = (flashTick == 0) ? 0.0F : Mth.clamp(swelling, 0.5F, 1.0F);
    }

    @Override
    public Identifier getTextureLocation(EnderCreeperRenderState state) {
        return TEXTURE;
    }

    // Scale the creeper as it swells (grows larger before exploding)
    @Override
    protected void scale(EnderCreeperRenderState state, PoseStack poseStack) {
        float swelling = state.swelling;
        float scale = 1.0F + Mth.sin(swelling * 100.0F) * swelling * 0.01F;
        scale += swelling * 0.3F;
        poseStack.scale(scale, scale, scale);
    }

    /**
     * Returns the white overlay progress for the flash effect.
     * This makes the creeper flash white when about to explode.
     *
     * @param state The render state containing swelling data
     * @return White overlay progress (0.0 = no overlay, 1.0 = fully white)
     */
    @Override
    protected float getWhiteOverlayProgress(EnderCreeperRenderState state) {
        return state.whiteOverlayProgress;
    }
}