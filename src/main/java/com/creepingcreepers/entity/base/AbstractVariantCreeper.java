/*
 * ============================================================================
 * AbstractVariantCreeper.java
 * ============================================================================
 * 
 * WHAT THIS FILE DOES:
 * Provides a shared base class for all Creeper variants in this mod.
 * Contains common functionality like explosion logic, swell mechanics,
 * and basic AI setup that all variants share.
 * 
 * WHY IT EXISTS:
 * - Reduces code duplication across variant implementations
 * - Ensures consistent behavior for core Creeper mechanics
 * - Makes adding new variants simple (extend and override)
 * - Follows the Open/Closed principle (open for extension, closed for modification)
 * 
 * HOW TO CREATE A NEW VARIANT:
 * 1. Create a new class extending AbstractVariantCreeper
 * 2. Override getExplosionRadius() with your config value
 * 3. Override createCustomExplosionEffects() for special effects
 * 4. Override registerGoals() to add variant-specific AI
 * 5. Call super.registerGoals() to keep base AI working
 * 
 * CREEPER MECHANICS EXPLAINED:
 * - Swell: The "charging up" animation before explosion
 * - Ignited: When the creeper is lit by flint and steel
 * - Explosion Radius: How big the explosion is (3 for vanilla)
 * 
 * @author Chompslabs
 * @version 1.0.0
 */
package com.creepingcreepers.entity.base;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

import javax.annotation.Nullable;

/**
 * Abstract base class for all Creeper variants in the mod.
 * 
 * This class handles:
 * - Swell mechanics (the "charging" before explosion)
 * - Explosion logic
 * - Data synchronization between server and client
 * - Common sounds and effects
 * 
 * Subclasses should override methods to customize behavior while
 * keeping the core Creeper mechanics intact.
 */
public abstract class AbstractVariantCreeper extends Monster {
    
    // =========================================================================
    // SYNCED DATA PARAMETERS
    // =========================================================================
    // These are special data fields that automatically sync between server and client.
    // Essential for animations and visual effects to work in multiplayer.
    
    /**
     * Current swell state (-1 to max fuse time).
     * Negative = not swelling, Positive = swelling progress.
     * Used for the expansion animation before explosion.
     */
    private static final EntityDataAccessor<Integer> DATA_SWELL_DIR = 
            SynchedEntityData.defineId(AbstractVariantCreeper.class, EntityDataSerializers.INT);
    
    /**
     * Whether the creeper has been ignited (by flint and steel).
     * Once true, the creeper will explode regardless of target.
     */
    private static final EntityDataAccessor<Boolean> DATA_IS_IGNITED = 
            SynchedEntityData.defineId(AbstractVariantCreeper.class, EntityDataSerializers.BOOLEAN);
    
    // =========================================================================
    // INSTANCE FIELDS
    // =========================================================================
    
    /**
     * Previous tick's swell amount. Used for smooth interpolation in rendering.
     */
    protected int oldSwell;
    
    /**
     * Current swell amount (0 to maxSwell).
     * When this reaches maxSwell, the creeper explodes.
     */
    protected int swell;
    
    /** Default fuse time in ticks (1.5 seconds). */
    private static final int DEFAULT_FUSE_TIME = 30;

    /**
     * Maximum swell value before explosion.
     * Derived from config, default 30 ticks (1.5 seconds).
     */
    protected int maxSwell = DEFAULT_FUSE_TIME;
    
    // =========================================================================
    // CONSTRUCTOR
    // =========================================================================
    
    /**
     * Creates a new variant creeper.
     * 
     * @param entityType The registered entity type
     * @param level The world this entity exists in
     */
    protected AbstractVariantCreeper(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }
    
    // =========================================================================
    // DATA SYNCHRONIZATION
    // =========================================================================

    /**
     * Defines synced data fields for client-server synchronization.
     * Called automatically during entity construction.
     *
     * In MC 1.20.5+/1.21, this method receives a SynchedEntityData.Builder
     * instead of directly accessing this.entityData.
     *
     * @param builder The builder used to define synced data entries
     */
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        // -1 means not swelling
        builder.define(DATA_SWELL_DIR, -1);
        builder.define(DATA_IS_IGNITED, false);
    }
    
    // =========================================================================
    // SERIALIZATION (Save/Load)
    // =========================================================================
    
    /**
     * Saves creeper state for world saving.
     *
     * @param output The value output to save to
     */
    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putShort("Fuse", (short) this.maxSwell);
        output.putBoolean("ignited", this.isIgnited());
    }
    
    /**
     * Loads creeper state when world loads.
     *
     * @param input The value input to read from
     */
    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.maxSwell = input.getShortOr("Fuse", (short) this.maxSwell);
        this.setIgnited(input.getBooleanOr("ignited", false));
    }
    
    // =========================================================================
    // TICK AND UPDATE LOGIC
    // =========================================================================
    
    /**
     * Called every game tick (20 times per second).
     * Handles swell progression and explosion triggering.
     */
    @Override
    public void tick() {
        // Store old swell for smooth rendering interpolation
        this.oldSwell = this.swell;
        
        // Handle swell progression while alive
        if (this.isAlive()) {
            int swellDirection = this.getSwellDir();
            
            if (swellDirection > 0 && this.swell == 0) {
                // Starting to swell - play fuse sound
                this.playSound(this.getFuseSound(), 1.0F, 0.5F);
                this.gameEvent(GameEvent.PRIME_FUSE);
            }
            
            // Update swell based on direction
            this.swell += swellDirection;
            
            // Clamp swell to valid range
            if (this.swell < 0) {
                this.swell = 0;
            }
            
            // Check if we've reached max swell
            if (this.swell >= this.maxSwell) {
                this.swell = this.maxSwell;
                this.explode();
            }
        }
        
        super.tick();
    }
    
    // =========================================================================
    // EXPLOSION LOGIC
    // =========================================================================
    
    /**
     * Triggers the creeper explosion.
     * Called when swell reaches maximum or entity is killed while ignited.
     * 
     * Override createCustomExplosionEffects() to add special effects
     * without modifying the core explosion logic.
     */
    protected void explode() {
        if (!this.level().isClientSide()) {
            // Store position before explosion (entity may be discarded)
            double explosionX = this.getX();
            double explosionY = this.getY();
            double explosionZ = this.getZ();
            float radius = this.getExplosionRadius();

            // Create the explosion FIRST - this applies damage to entities
            // Parameters: entity, x, y, z, radius, interaction
            this.level().explode(
                    this,
                    explosionX,
                    explosionY,
                    explosionZ,
                    radius,
                    Level.ExplosionInteraction.MOB
            );

            // Create custom effects AFTER explosion - damage is already applied
            // This allows effects like teleportation to happen after damage registers
            this.createCustomExplosionEffects();

            // Remove the creeper
            this.discard();
        }
    }
    
    /**
     * Gets the explosion radius for this creeper variant.
     *
     * Must be implemented by subclasses to read from config.
     * Vanilla creeper radius is 3 for reference.
     *
     * @return The explosion radius in blocks
     */
    protected abstract float getExplosionRadius();
    
    /**
     * Creates custom effects when the creeper explodes.
     *
     * Override this in subclasses to add special effects like:
     * - Dragon's Breath clouds
     * - Lightning strikes
     * - Potion effects
     * - Player teleportation
     *
     * Called on server side only, AFTER the explosion damage is applied.
     * This ensures damage registers before effects like teleportation.
     */
    protected void createCustomExplosionEffects() {
        // Base implementation does nothing
        // Subclasses override for special effects
    }
    
    // =========================================================================
    // SWELL (CHARGING) MECHANICS
    // =========================================================================
    
    /**
     * Gets the current swell direction.
     * 
     * @return -1 if deflating, 1 if swelling, 0 if stable
     */
    public int getSwellDir() {
        return this.entityData.get(DATA_SWELL_DIR);
    }
    
    /**
     * Sets the swell direction.
     * Called by AI goals to start/stop the charging animation.
     * 
     * @param state -1 to deflate, 1 to swell
     */
    public void setSwellDir(int state) {
        this.entityData.set(DATA_SWELL_DIR, state);
    }
    
    /**
     * Gets the swell amount interpolated for smooth rendering.
     * 
     * @param partialTick Partial tick for frame interpolation
     * @return Interpolated swell value between 0 and 1
     */
    public float getSwelling(float partialTick) {
        return (this.oldSwell + (this.swell - this.oldSwell) * partialTick) / (float)(this.maxSwell - 2);
    }
    
    // =========================================================================
    // IGNITION (Flint and Steel)
    // =========================================================================
    
    /**
     * Checks if the creeper has been ignited.
     * 
     * @return true if ignited by flint and steel
     */
    public boolean isIgnited() {
        return this.entityData.get(DATA_IS_IGNITED);
    }
    
    /**
     * Sets the ignition state.
     * 
     * @param ignited true to ignite the creeper
     */
    public void setIgnited(boolean ignited) {
        this.entityData.set(DATA_IS_IGNITED, ignited);
    }
    
    /**
     * Ignites the creeper, starting the explosion countdown.
     * Called when a player uses flint and steel on the creeper.
     */
    public void ignite() {
        this.setIgnited(true);
    }
    
    // =========================================================================
    // SOUNDS
    // =========================================================================
    
    /**
     * Gets the sound played when the creeper starts swelling.
     * Override to use custom sounds.
     * 
     * @return The fuse sound event
     */
    protected SoundEvent getFuseSound() {
        return SoundEvents.CREEPER_PRIMED;
    }
    
    /**
     * Gets the ambient sound (randomly played while idle).
     * Creepers are silent by default.
     * 
     * @return null for silent, or a sound event
     */
    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }
    
    /**
     * Gets the sound played when the creeper takes damage.
     * 
     * @param damageSource The source of damage
     * @return The hurt sound event
     */
    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.CREEPER_HURT;
    }
    
    /**
     * Gets the sound played when the creeper dies.
     * 
     * @return The death sound event
     */
    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.CREEPER_DEATH;
    }
    
    // =========================================================================
    // UTILITY METHODS
    // =========================================================================

    /**
     * Sets the maximum swell time (fuse duration).
     * 
     * @param ticks Ticks until explosion after starting to swell
     */
    public void setMaxSwell(int ticks) {
        this.maxSwell = ticks;
    }
    
    /**
     * Gets the maximum swell time.
     *
     * @return Ticks until explosion
     */
    public int getMaxSwell() {
        return this.maxSwell;
    }

    /**
     * Gets the current swell amount.
     * Used by AI goals to check if creeper is still charged.
     *
     * @return Current swell value (0 to maxSwell)
     */
    public int getSwell() {
        return this.swell;
    }

    // =========================================================================
    // COMBAT
    // =========================================================================

    /**
     * Prevents all melee damage from creeper variants.
     *
     * This is the same approach vanilla Minecraft's Creeper uses.
     * MeleeAttackGoal calls this method when the mob is within melee range,
     * but creepers deal damage through explosions, not melee attacks.
     * Returning true signals "attack handled" without dealing any damage.
     *
     * @param level The server level
     * @param target The entity being attacked
     * @return true always (attack is "handled" but no damage is dealt)
     */
    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        return true;
    }
}
