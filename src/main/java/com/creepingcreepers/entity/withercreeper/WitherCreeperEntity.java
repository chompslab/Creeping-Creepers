/*
 * ============================================================================
 * WitherCreeperEntity.java
 * ============================================================================
 *
 * WHAT THIS FILE DOES:
 * Implements the Wither Creeper - a creeper that spawns in the Nether
 * and inflicts the Wither effect on explosion.
 *
 * BEHAVIOR SUMMARY:
 * - Spawns anywhere in the Nether dimension (23% spawn chance)
 * - Uses vanilla creeper AI with added line of sight requirement for swelling
 * - Chases and explodes near players like a standard creeper
 * - Flees from cats/ocelots within 6-block radius
 * - Immune to fire and wither effect
 * - Explosion inflicts Wither effect and spawns smoke particles
 *
 * @author Chompslabs
 * @version 1.0.0
 */
package com.creepingcreepers.entity.withercreeper;

import com.creepingcreepers.ai.goal.WitherCreeperSwellGoal;
import com.creepingcreepers.config.CreepingCreepersConfig;
import com.creepingcreepers.entity.base.AbstractVariantCreeper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.feline.Ocelot;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;

/**
 * The Wither Creeper entity.
 *
 * A creeper variant that spawns in the Nether dimension
 * and applies the Wither effect to entities caught in its explosion.
 * Uses vanilla creeper AI with added line of sight requirement for swelling.
 */
public class WitherCreeperEntity extends AbstractVariantCreeper {

    // =========================================================================
    // CONSTANTS
    // =========================================================================

    /** Spawn chance percentage (23%). */
    private static final double SPAWN_CHANCE = 0.23;

    /** Particle count scaling factor per unit of explosion radius. */
    private static final int PARTICLES_PER_RADIUS_UNIT = 30;

    /** Range to look at nearby players in blocks. */
    private static final float LOOK_AT_PLAYER_RANGE = 8.0F;

    // =========================================================================
    // CONSTRUCTOR
    // =========================================================================

    /**
     * Creates a new Wither Creeper.
     *
     * @param entityType The registered entity type
     * @param level The world this entity exists in
     */
    public WitherCreeperEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.maxSwell = CreepingCreepersConfig.WITHER_CREEPER_FUSE_TIME.get();
    }

    // =========================================================================
    // ATTRIBUTE CREATION
    // =========================================================================

    /**
     * Creates the attribute supplier for Wither Creepers.
     * Uses default values since config isn't loaded at registration time.
     *
     * @return Builder with all entity attributes
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)  // Default from config
                .add(Attributes.MOVEMENT_SPEED, 0.25)  // Default from config
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    // =========================================================================
    // AI GOALS REGISTRATION
    // =========================================================================

    /**
     * Registers all AI goals for the Wither Creeper.
     * Uses vanilla creeper behavior patterns with line of sight for swelling.
     *
     * GOAL PRIORITY BREAKDOWN:
     * 1 - Swell (explode) when close to target - highest priority
     * 2 - Float in water
     * 3 - Avoid cats/ocelots - flee faster than pursuit
     * 4 - Melee attack (approach target)
     * 5 - Wander around - idle behavior
     * 6 - Look at players - awareness
     * 7 - Random look around - idle animation
     *
     * TARGET GOALS:
     * 1 - Retaliate when hurt
     * 2 - Target nearest player (vanilla creeper behavior)
     */
    @Override
    protected void registerGoals() {
        // === BEHAVIOR GOALS ===

        // Priority 1: Start swelling when close enough to target
        this.goalSelector.addGoal(1, new WitherCreeperSwellGoal(this));

        // Priority 2: Float in water (Wither Creeper is fire immune, not water averse)
        this.goalSelector.addGoal(2, new FloatGoal(this));

        // Priority 3: Run away from cats and ocelots (faster than pursuit speed)
        float fleeSpeed = CreepingCreepersConfig.WITHER_CREEPER_FLEE_SPEED.get().floatValue();
        float catFearRange = CreepingCreepersConfig.WITHER_CREEPER_CAT_FEAR_RANGE.get().floatValue();
        this.goalSelector.addGoal(3, new AvoidEntityGoal<>(this, Ocelot.class, catFearRange, 1.0, fleeSpeed));
        this.goalSelector.addGoal(3, new AvoidEntityGoal<>(this, Cat.class, catFearRange, 1.0, fleeSpeed));

        // Priority 4: Move toward target (MeleeAttackGoal handles pathfinding)
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.0, false));

        // Priority 5: Wander around when idle
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8, 0.0F));

        // Priority 6: Look at nearby players
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, LOOK_AT_PLAYER_RANGE));

        // Priority 7: Random looking around
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        // === TARGETING GOALS ===

        // Target whatever hurt us (provocation response)
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));

        // Target nearest player (vanilla creeper behavior)
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    // =========================================================================
    // EXPLOSION BEHAVIOR
    // =========================================================================

    /**
     * Gets the explosion radius from config.
     *
     * @return Explosion radius in blocks
     */
    @Override
    protected float getExplosionRadius() {
        return CreepingCreepersConfig.WITHER_CREEPER_EXPLOSION_RADIUS.get().floatValue();
    }

    /**
     * Overrides explode() to apply the Wither effect BEFORE the explosion runs.
     *
     * The base class calls level().explode() first, which can kill entities.
     * Dead entities fail the isAlive() check in createCustomExplosionEffects(),
     * meaning players killed by the blast would never receive wither.
     * By collecting and withering entities here — before super.explode() —
     * we guarantee wither is applied to everyone in range regardless of whether
     * the explosion is fatal.
     *
     * Vanilla's Explosion class damages entities within power * 2 blocks, not
     * just power blocks. The wither range must match that to cover all entities
     * that the explosion actually hits.
     */
    @Override
    protected void explode() {
        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            float radius = this.getExplosionRadius();
            // Vanilla explosion damages entities within power * 2 blocks.
            float witherRadius = radius * 2.0f;
            int witherDuration = CreepingCreepersConfig.WITHER_CREEPER_WITHER_DURATION.get();
            int witherAmplifier = CreepingCreepersConfig.WITHER_CREEPER_WITHER_AMPLIFIER.get();

            AABB explosionBox = new AABB(
                    this.getX() - witherRadius, this.getY() - witherRadius, this.getZ() - witherRadius,
                    this.getX() + witherRadius, this.getY() + witherRadius, this.getZ() + witherRadius
            );

            serverLevel.getEntitiesOfClass(
                    LivingEntity.class,
                    explosionBox,
                    entity -> entity != this && entity.isAlive() && isWithinExplosionRadius(entity, witherRadius)
            ).forEach(entity -> entity.addEffect(new MobEffectInstance(
                    MobEffects.WITHER,
                    witherDuration,
                    witherAmplifier
            )));
        }

        super.explode();
    }

    /**
     * Spawns smoke particles after the explosion.
     * Wither application has already been handled in explode().
     */
    @Override
    protected void createCustomExplosionEffects() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        float radius = this.getExplosionRadius();
        int particleCount = (int) (radius * PARTICLES_PER_RADIUS_UNIT);
        serverLevel.sendParticles(
                ParticleTypes.SMOKE,
                this.getX(),
                this.getY() + 0.5,
                this.getZ(),
                particleCount,
                radius * 0.5, radius * 0.5, radius * 0.5,
                0.05
        );
    }

    /**
     * Checks whether any part of an entity's bounding box is within the
     * explosion radius. This mirrors vanilla explosion hit detection, which
     * uses bounding box proximity rather than feet-to-feet distance.
     *
     * @param entity The entity to test
     * @param radius The explosion radius
     * @return true if the entity's bounding box is within the sphere
     */
    private boolean isWithinExplosionRadius(LivingEntity entity, float radius) {
        AABB box = entity.getBoundingBox();
        double dx = Math.max(0, Math.max(this.getX() - box.maxX, box.minX - this.getX()));
        double dy = Math.max(0, Math.max(this.getY() - box.maxY, box.minY - this.getY()));
        double dz = Math.max(0, Math.max(this.getZ() - box.maxZ, box.minZ - this.getZ()));
        return dx * dx + dy * dy + dz * dz <= (double) radius * radius;
    }

    // =========================================================================
    // DAMAGE IMMUNITY
    // =========================================================================

    /**
     * Override to prevent Wither effect from being applied.
     * Fire immunity is handled by EntityType.Builder.fireImmune().
     */
    @Override
    public boolean addEffect(MobEffectInstance effectInstance, @Nullable Entity source) {
        // Block Wither effect - this entity is immune
        if (effectInstance.getEffect() == MobEffects.WITHER) {
            return false;
        }
        return super.addEffect(effectInstance, source);
    }

    // =========================================================================
    // SPAWN CONDITIONS
    // =========================================================================

    /**
     * Checks spawn conditions for Wither Creeper.
     * Spawns anywhere in the Nether dimension.
     * Uses standard monster spawn rules with a 23% spawn chance.
     *
     * @param type The entity type
     * @param level The server level accessor
     * @param spawnReason The spawn reason
     * @param pos The spawn position
     * @param random Random source
     * @return true if spawn is allowed
     */
    public static boolean checkWitherCreeperSpawnRules(
            EntityType<WitherCreeperEntity> type,
            ServerLevelAccessor level,
            EntitySpawnReason spawnReason,
            BlockPos pos,
            RandomSource random
    ) {
        // Must be in the Nether dimension
        if (level.getLevel().dimension() != Level.NETHER) {
            return false;
        }

        // Apply 23% spawn chance (slightly rarer than Wither Skeleton's 28.57%)
        if (random.nextDouble() > SPAWN_CHANCE) {
            return false;
        }

        // Use standard monster spawn rules (checks for valid spawn block, light level, etc.)
        return Monster.checkMonsterSpawnRules(type, level, spawnReason, pos, random);
    }
}
