/*
 * ============================================================================
 * EnderCreeperModel.java
 * ============================================================================
 * 
 * WHAT THIS FILE DOES:
 * Defines the 3D model for the Ender Creeper entity.
 * Uses the same model structure as the vanilla Creeper.
 * 
 * WHY IT EXISTS:
 * Every entity needs a model to be rendered. This class defines:
 * - The bone/part structure (head, body, legs)
 * - The dimensions and positions of each part
 * - Animation logic (swelling effect)
 * 
 * MODEL STRUCTURE:
 * - Head: The creeper's distinctive face
 * - Body: The tall rectangular body
 * - Legs: Four short legs at the corners
 * 
 * HOW TO MODIFY:
 * To create a different model, change the CubeListBuilder definitions
 * in createBodyLayer(). Each cube has:
 * - texOffs: UV coordinates on the texture
 * - addBox: Position and size of the cube
 * 
 * @author Chompslabs
 * @version 1.0.0
 */
package com.creepingcreepers.client.model;

import com.creepingcreepers.CreepingCreepersMod;
import com.creepingcreepers.client.renderer.EnderCreeperRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Model class for the Ender Creeper.
 * 
 * Based on vanilla Creeper model with the same bone structure.
 * Extends EntityModel for proper animation support.
 */
public class EnderCreeperModel extends EntityModel<EnderCreeperRenderState> {
    
    /**
     * Layer location for model registration.
     * Format: modid:entity_name
     *
     * Uses Identifier.fromNamespaceAndPath() for MC 1.21.11 compatibility.
     */
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(CreepingCreepersMod.MOD_ID, "ender_creeper"), "main");

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
    public EnderCreeperModel(ModelPart root) {
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
    public void setupAnim(EnderCreeperRenderState state) {
        super.setupAnim(state);

        // Animate head to look at target
        this.head.yRot = state.yRot * ((float) Math.PI / 180F);
        this.head.xRot = state.xRot * ((float) Math.PI / 180F);

        // Animate legs for walking
        // Uses sine wave for smooth back-and-forth motion
        float legSwing = Mth.cos(state.walkAnimationPos * 0.6662F) * 1.4F * state.walkAnimationSpeed;
        this.leg1.xRot = legSwing;
        this.leg2.xRot = -legSwing;
        this.leg3.xRot = -legSwing;
        this.leg4.xRot = legSwing;
    }
}
