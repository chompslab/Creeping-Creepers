

























# Creeping Creepers Mod

A Minecraft Forge mod that adds unique Creeper variants with special abilities!

## Overview

**Creeping Creepers** introduces terrifying new Creeper variants to Minecraft. The first variant is the **Ender Creeper** - a hybrid mob combining the explosive nature of Creepers with the teleportation abilities of Endermen.

## Requirements

- **Minecraft**: 1.21.11
- **Forge**: 52.0.40+
- **Java**: 21+
- **IDE**: IntelliJ IDEA (recommended) or Eclipse

## Installation for Development

1. Clone or download this repository
2. Open in IntelliJ IDEA
3. Import as Gradle project
4. Run `./gradlew genIntellijRuns` (or `./gradlew genEclipseRuns` for Eclipse)
5. Refresh Gradle project
6. Run the `runClient` configuration

## Building

```bash
./gradlew build
```

The compiled mod JAR will be in `build/libs/`

## The Ender Creeper

### Behavior

The Ender Creeper is a neutral mob that becomes hostile when you look directly at it (like an Enderman).

| Trait | Description |
|-------|-------------|
| **Neutral** | Won't attack unless provoked by staring |
| **Teleportation** | Teleports to reposition, alternating with walking |
| **Explosion** | Explodes like a Creeper when close to target |
| **Dragon's Breath** | Spawns a damaging cloud on explosion |
| **Player Teleport** | Players hit by explosion are teleported to cloud center |
| **Water Weakness** | Takes damage from water and rain |
| **Cat Fear** | Runs away from cats and ocelots |

### Stats (Configurable)

| Stat | Default Value |
|------|---------------|
| Health | 30 HP |
| Speed | 0.28 |
| Attack Damage | 7.0 |
| Explosion Radius | 4 blocks |
| Fuse Time | 1.5 seconds |
| Teleport Cooldown | 2 seconds |
| Teleport Range | 32 blocks |

### Spawning

- Spawns in the same conditions as Endermen
- Requires light level 0 (default)
- Spawn weight: 5 (configurable)
- Spawns alone or in pairs

### Drops

- Gunpowder (0-2, affected by Looting)
- Ender Pearl (0-1, affected by Looting)

## Configuration

All values are configurable in `config/creepingcreepers-common.toml`:

```toml
[ender_creeper]
    [ender_creeper.stats]
        health = 30.0
        movement_speed = 0.28
        attack_damage = 7.0
    
    [ender_creeper.explosion]
        explosion_radius = 4
        fuse_time = 30
    
    [ender_creeper.teleportation]
        teleport_cooldown = 40
        teleport_range = 32
        min_teleport_distance = 4
    
    [ender_creeper.dragons_breath]
        duration = 100
        radius = 3.0
        amplifier = 1
    
    [ender_creeper.environmental]
        water_damage = 1.0
        afraid_of_cats = true
    
    [ender_creeper.spawning]
        spawn_weight = 5
        min_group_size = 1
        max_group_size = 2
        max_light_level = 0
    
    [ender_creeper.aggro]
        stare_range = 64.0
        anger_time = 400
```

## Project Structure

```
creepingcreepers/
├── src/main/java/com/creepingcreepers/
│   ├── CreepingCreepersMod.java      # Main mod class
│   ├── config/
│   │   └── CreepingCreepersConfig.java  # All config values
│   ├── entity/
│   │   ├── base/
│   │   │   └── AbstractVariantCreeper.java  # Base class for variants
│   │   └── endercreeper/
│   │       └── EnderCreeperEntity.java  # Ender Creeper implementation
│   ├── registry/
│   │   ├── ModEntities.java          # Entity registration
│   │   └── ModItems.java             # Item (spawn egg) registration
│   ├── ai/goal/
│   │   ├── EnderCreeperSwellGoal.java  # Explosion charging
│   │   └── TeleportToTargetGoal.java   # Teleportation AI
│   ├── client/
│   │   ├── ClientModEvents.java      # Client-side registration
│   │   ├── model/
│   │   │   └── EnderCreeperModel.java  # Entity model
│   │   └── renderer/
│   │       └── EnderCreeperRenderer.java  # Entity renderer
│   └── event/
│       └── ModEventHandlers.java     # Event handlers
├── src/main/resources/
│   ├── META-INF/mods.toml           # Mod metadata
│   ├── pack.mcmeta                   # Resource pack info
│   ├── assets/creepingcreepers/
│   │   ├── lang/en_us.json          # Translations
│   │   ├── models/item/             # Item models
│   │   └── textures/entity/         # Entity textures
│   └── data/creepingcreepers/
│       └── loot_tables/entities/    # Loot tables
├── build.gradle                      # Build configuration
├── gradle.properties                 # Project properties
└── settings.gradle                   # Gradle settings
```

## Adding New Creeper Variants

The mod is designed for easy extension. To add a new variant:

### 1. Create the Entity Class

```java
public class MyNewCreeperEntity extends AbstractVariantCreeper {
    
    public MyNewCreeperEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }
    
    @Override
    protected float getExplosionRadius() {
        return MyConfig.EXPLOSION_RADIUS.get().floatValue();
    }
    
    @Override
    protected void createCustomExplosionEffects() {
        // Add your special effects here
    }
    
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 20.0)
            .add(Attributes.MOVEMENT_SPEED, 0.25);
    }
}
```

### 2. Register the Entity (ModEntities.java)

```java
public static final RegistryObject<EntityType<MyNewCreeperEntity>> MY_CREEPER = 
    ENTITY_TYPES.register("my_creeper", () ->
        EntityType.Builder.<MyNewCreeperEntity>of(MyNewCreeperEntity::new, MobCategory.MONSTER)
            .sized(0.6F, 1.7F)
            .clientTrackingRange(8)
            .build("my_creeper")
    );
```

### 3. Register Attributes (ModEntities.java)

```java
public static void registerAttributes(EntityAttributeCreationEvent event) {
    event.put(MY_CREEPER.get(), MyNewCreeperEntity.createAttributes().build());
}
```

### 4. Create a Spawn Egg (ModItems.java)

```java
public static final RegistryObject<Item> MY_CREEPER_SPAWN_EGG = ITEMS.register(
    "my_creeper_spawn_egg",
    () -> new ForgeSpawnEggItem(ModEntities.MY_CREEPER, 0xPRIMARY, 0xSECONDARY, new Item.Properties())
);
```

### 5. Add Config Values

Add your balance values to `CreepingCreepersConfig.java`

### 6. Create Resources

- Texture: `assets/creepingcreepers/textures/entity/my_creeper.png`
- Lang: Add entries to `en_us.json`
- Model: Create `my_creeper_spawn_egg.json` in models/item/
- Loot Table: Create `my_creeper.json` in data/loot_tables/entities/

### 7. Create Renderer and Model

Create client-side classes in the `client` package following the existing patterns.

## License

MIT License

## Credits

- Developed by CreepingCreepersTeam
- Built with Minecraft Forge
- Inspired by vanilla Minecraft's Creeper and Enderman mobs
