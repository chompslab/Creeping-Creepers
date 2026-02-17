/*
 * ============================================================================
 * NetherCreeperModel.java
 * ============================================================================
 *
 * WHAT THIS FILE DOES:
 * Defines the 3D model for the Nether Creeper entity.
 * Uses the same model structure as the vanilla Creeper with shivering animation.
 *
 * WHY IT EXISTS:
 * Every entity needs a model to be rendered. This class defines:
 * - The bone/part structure (head, body, legs)
 * - The dimensions and positions of each part
 * - Animation logic (walking, swelling, shivering when cold)
 *
 * MODEL STRUCTURE:
 * - Head: The creeper's distinctive face
 * - Body: The tall rectangular body
 * - Legs: Four short legs at the corners
 *
 * SPECIAL ANIMATION:
 * When cold (out of lava), the creeper shivers like a Strider does.
 *
 * @author Chompslabs
 * @version 1.0.0
 */
package com.creepingcreepers.client.model;

import com.creepingcreepers.CreepingCreepersMod;
import com.creepingcreepers.client.renderer.NetherCreeperRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Model class for the Nether Creeper.
 *
 * Based on vanilla Creeper model with added shivering animation when cold.
 */
public class NetherCreeperModel extends EntityModel<NetherCreeperRenderState> {

    /**
     * Layer location for model registration.
     */
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(CreepingCreepersMod.MOD_ID, "nether_creeper"), "main");

    /**
     * The head part for face rendering and rotation.
     */
    private final ModelPart head;

    /**
     * Front right leg.
     */
    private final ModelPart leg1;

    /**
     * Front left leg.
     */
    private final ModelPart leg2;

    /**
     * Back right leg.
     */
    private final ModelPart leg3;

    /**
     * Back left leg.
     */
    private final ModelPart leg4;

    /**
     * Creates a new model from the baked model part.
     *
     * @param root The root model part from the layer definition
     */
    public NetherCreeperModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.leg1 = root.getChild("leg1");
        this.leg2 = root.getChild("leg2");
        this.leg3 = root.getChild("leg3");
        this.leg4 = root.getChild("leg4");
    }

    /**
     * Creates the layer definition for the model.
     *
     * This defines the structure of the model:
     * - Mesh data (cubes and their UV mappings)
     * - Part poses (initial positions and rotations)
     *
     * @return The layer definition for registration
     */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition partDefinition = mesh.getRoot();

        // Head - positioned at top of body
        partDefinition.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 6.0F, 0.0F)
        );

        // Body - the main rectangular body
        partDefinition.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(16, 16)
                        .addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F),
                PartPose.offset(0.0F, 6.0F, 0.0F)
        );

        // Front right leg
        partDefinition.addOrReplaceChild("leg1",
                CubeListBuilder.create()
                        .texOffs(0, 16)
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F),
                PartPose.offset(-2.0F, 18.0F, 4.0F)
        );

        // Front left leg
        partDefinition.addOrReplaceChild("leg2",
                CubeListBuilder.create()
                        .texOffs(0, 16)
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F),
                PartPose.offset(2.0F, 18.0F, 4.0F)
        );

        // Back right leg
        partDefinition.addOrReplaceChild("leg3",
                CubeListBuilder.create()
                        .texOffs(0, 16)
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F),
                PartPose.offset(-2.0F, 18.0F, -4.0F)
        );

        // Back left leg
        partDefinition.addOrReplaceChild("leg4",
                CubeListBuilder.create()
                        .texOffs(0, 16)
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F),
                PartPose.offset(2.0F, 18.0F, -4.0F)
        );

        // Texture is 64x32 pixels (standard creeper texture size)
        return LayerDefinition.create(mesh, 64, 32);
    }

    /**
     * Sets up the model's animation for the current frame.
     *
     * @param state The render state containing all animation data
     */
    @Override
    public void setupAnim(NetherCreeperRenderState state) {
        super.setupAnim(state);

        // Animate head to look at target
        this.head.yRot = state.yRot * ((float) Math.PI / 180F);
        this.head.xRot = state.xRot * ((float) Math.PI / 180F);

        // Base leg animation for walking
        float legSwing = Mth.cos(state.walkAnimationPos * 0.6662F) * 1.4F * state.walkAnimationSpeed;

        // Add shivering effect when cold
        if (state.isCold) {
            // Calculate shiver offset based on shivering ticks
            // Fast oscillation for shaking effect
            float shiverAmount = Mth.sin(state.shiveringTicks * 1.5F) * 0.1F;
            float shiverOffset = Mth.cos(state.shiveringTicks * 1.2F) * 0.05F;

            // Apply shiver to legs (alternating for shaky effect)
            this.leg1.xRot = legSwing + shiverAmount;
            this.leg2.xRot = -legSwing - shiverAmount;
            this.leg3.xRot = -legSwing + shiverAmount;
            this.leg4.xRot = legSwing - shiverAmount;

            // Head shakes slightly when cold
            this.head.zRot = shiverOffset;
        } else {
            // Normal walking animation when warm
            this.leg1.xRot = legSwing;
            this.leg2.xRot = -legSwing;
            this.leg3.xRot = -legSwing;
            this.leg4.xRot = legSwing;

            // No head shake when warm
            this.head.zRot = 0;
        }
    }
}
