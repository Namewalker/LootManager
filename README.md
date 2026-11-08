# LootManager

Current implementation version: 1.0.1

PLUGIN NAME:
LootManager

TARGET:
Spigot/Paper 1.12.2
Java 8
Maven

==================================================
OVERVIEW
==================================================

LootManager is a comprehensive loot generation, loot management,
and loot regeneration plugin for Minecraft 1.12.2.

The plugin allows administrators to:

- Scan worlds or regions for containers
- Populate empty containers with randomized loot
- Create unlimited loot tables
- Edit loot tables entirely in-game
- Create regenerating loot regions
- Smart-refill previously generated containers
- Support custom worlds
- Maintain server performance through queued scanning

Designed for:

- Survival Servers
- RPG Servers
- Dungeon Servers
- Adventure Maps
- Exploration Worlds
- Custom World Networks

==================================================
DESIGN GOALS
==================================================

- Fully world-aware
- Fully in-game configurable
- No console-only workflows
- Minimal TPS impact
- Queue-driven processing
- Multi-table support
- Table inheritance system
- Smart refill protection
- Region-based regeneration
- Large-world compatibility

==================================================
SUPPORTED CONTAINERS
==================================================

Default:

- Chest
- Trapped Chest
- Double Chest

Optional:

- Shulker Boxes
- Any InventoryHolder block

Double chests must be treated as a single inventory.

==================================================
FILES
==================================================

config.yml

loot.yml

regions.yml

data.yml

messages.yml

==================================================
WORLD AWARENESS
==================================================

All commands must require explicit world targeting
when applicable.

The plugin must NEVER assume:

- Player world
- Default world
- First loaded world

Examples:

world
Amberlight
DungeonWorld
Events

Commands targeting worlds only affect the specified world.

==================================================
LOOT TABLE SYSTEM
==================================================

Unlimited loot tables supported.

Examples:

starter
village
dungeon
boss
Amberlight_ruins
Amberlight_castle
Amberlight_catacombs

==================================================
TABLE CREATION
==================================================

Command:

/loot table create <name>

Example:

/loot table create dungeon

Creates empty table.

Saved instantly to loot.yml.

==================================================
TABLE MANAGEMENT
==================================================

Commands:

/loot table create <name>

/loot table delete <name>

/loot table rename <old> <new>

/loot table list

/loot table info <table>

/loot table inherit <table> <parent>

/loot table clearinheritance <table>

==================================================
TABLE LIST
==================================================

Command:

/loot table list

Output:

starter

village

dungeon

boss

Amberlight_ruins

==================================================
TABLE INFORMATION
==================================================

Command:

/loot table info <table>

Shows:

Name

Parent Table

Item Count

Region Count

Last Edited

==================================================
TABLE INHERITANCE SYSTEM
==================================================

Purpose:

Avoid duplicate loot definitions.

Example:

starter

contains:

Bread
Coal
Iron

dungeon

inherits:

starter

adds:

Gold
Diamonds

boss

inherits:

dungeon

adds:

Rare Rewards

==================================================
INHERITANCE EXAMPLE
==================================================

loot.yml

tables:

  starter:

    items:
      bread:
      coal:
      iron:

  dungeon:

    inherits: starter

    items:
      gold:
      diamond:

  boss:

    inherits: dungeon

    items:
      nether_star:

==================================================
INHERITANCE RULES
==================================================

A table receives:

Parent Items
+
Own Items

==================================================
INHERITANCE LIMITS
==================================================

Prevent loops.

INVALID:

A -> B

B -> A

INVALID:

A -> B

B -> C

C -> A

Plugin must reject circular references.

==================================================
INHERITANCE COMMANDS
==================================================

Set Parent:

/loot table inherit dungeon starter

Remove Parent:

/loot table clearinheritance dungeon

View Parent:

/loot table info dungeon

==================================================
TABLE EDITOR
==================================================

Command:

/loot edit <table>

Examples:

/loot edit starter

/loot edit dungeon

Opens GUI editor.

==================================================
GUI LOOT EDITOR
==================================================

Features:

- View entries
- Add entries
- Remove entries
- Edit weights
- Edit chances
- Edit amounts

Changes save instantly.

No reload required.

==================================================
ADDING NEW ITEMS
==================================================

GUI Button:

Add Item

Process:

1. Admin inserts item
2. Plugin reads ItemStack
3. Generates default rarity
4. Saves item

Supports:

- Names
- Lore
- Enchantments
- Potion Meta
- Item Flags
- Custom Metadata

==================================================
AUTO-RARITY SYSTEM
==================================================

Example Suggestions:

DIRT

Chance: 75%
Weight: 100

IRON

Chance: 40%
Weight: 20

GOLD

Chance: 25%
Weight: 10

DIAMOND

Chance: 10%
Weight: 5

Values are editable.

==================================================
ITEM SETTINGS
==================================================

Editable:

Chance

Weight

Minimum Amount

Maximum Amount

Minimum Rolls

Maximum Rolls

==================================================
LOOT TABLE FORMAT
==================================================

tables:

  dungeon:

    inherits: starter

    settings:

      min-rolls: 2

      max-rolls: 6

    items:

      diamond:

        material: DIAMOND

        chance: 0.10

        weight: 2

        amount:

          min: 1

          max: 2

==================================================
LOOT GENERATION
==================================================

Process:

1. Select loot table
2. Resolve inheritance chain
3. Determine rolls
4. Evaluate chances
5. Apply weights
6. Generate ItemStacks
7. Randomize slots
8. Insert items

==================================================
RANDOM SLOT PLACEMENT
==================================================

Items must be inserted into random locations.

GOOD:

3
8
14
15
26

BAD:

0
1
2
3
4

==================================================
LOOT WAND
==================================================

Command:

/loot wand

Permission:

loot.wand

Default Item:

GOLD_HOE

Configurable.

==================================================
SELECTION SYSTEM
==================================================

Left Click:

Set Position 1

Right Click:

Set Position 2

==================================================
SELECTION COMMANDS
==================================================

/loot wand

/loot pos1

/loot pos2

/loot clearselection

/loot showselection

/loot info

==================================================
SELECTION VISUALIZATION
==================================================

Optional particle display.

Command:

/loot showselection

==================================================
LOOT REGIONS
==================================================

Permanent named regions.

Each region stores:

World

Position 1

Position 2

Loot Table

Regeneration Time

==================================================
REGION CREATION
==================================================

Commands:

/loot region create <name> <table>

/loot region create <name> <table> <regenMinutes>

Examples:

/loot region create dungeon1 dungeon

/loot region create ruins village 30

If time omitted:

Use config default.

==================================================
REGION STORAGE
==================================================

regions.yml

regions:

  dungeon1:

    world: Amberlight

    table: dungeon

    regen-minutes: 30

    pos1:

      x: 100

      y: 50

      z: 100

    pos2:

      x: 250

      y: 90

      z: 250

==================================================
REGION COMMANDS
==================================================

/loot region create

/loot region remove

/loot region list

/loot region info

/loot region enable

/loot region disable

/loot region settable

/loot region settime

==================================================
AUTOMATIC REGION REGENERATION
==================================================

Every minute:

Plugin checks active regions.

If timer exceeded:

Queue refill scan.

==================================================
SMART REFILL SYSTEM
==================================================

Purpose:

Prevent player storage from being modified.

Only refill containers generated by LootManager.

==================================================
TRACKED DATA
==================================================

Every generated container stores:

World

X

Y

Z

Loot Table

Last Fill Time

Generation ID

==================================================
DATA.YML EXAMPLE
==================================================

generated:

  Amberlight:120:64:400:

    table: dungeon

    generation-id: 542

    last-fill: 123456789

==================================================
REFILL MODES
==================================================

Mode 1

Generate Once

Never refill

-------------------------------

Mode 2

Timer Based

Refill after X minutes

-------------------------------

Mode 3

Region Controlled

Region timer handles refills

==================================================
REFILL RULES
==================================================

Default:

1. Chest must be generated
2. Chest must be empty
3. Region must permit refill

Then refill.

==================================================
REFILL CONFIGURATION
==================================================

refill:

  enabled: true

  refill-only-generated: true

  require-empty-chest: true

  allow-partial-refills: false

  mode: REGION

==================================================
QUEUE BASED SCANNING
==================================================

Plugin must NEVER scan entire worlds instantly.

All operations use queues.

==================================================
CHUNK QUEUE
==================================================

Stores:

World Name

Chunk X

Chunk Z

Only.

Never store Chunk objects.

==================================================
SCAN PROCESS
==================================================

1. Collect chunks
2. Add chunks to queue
3. Process gradually
4. Queue containers
5. Process containers

==================================================
CONTAINER QUEUE
==================================================

Separate queue for container processing.

Purpose:

Avoid lag spikes.

==================================================
PERFORMANCE SETTINGS
==================================================

config.yml

scan:

  chunks-per-tick: 1

  containers-per-tick: 50

  scan-interval-ticks: 1

  max-world-chunks: 100000

==================================================
WORLD SCANNING
==================================================

Command:

/loot fillworld <world> <table>

Example:

/loot fillworld Amberlight dungeon

==================================================
LOADED CHUNK SCANNING
==================================================

Command:

/loot fill loaded <world> <table>

Example:

/loot fill loaded Amberlight dungeon

Only scans already loaded chunks.

==================================================
SELECTION SCANNING
==================================================

Command:

/loot fillselection <table>

Example:

/loot fillselection dungeon

Uses current wand selection.

==================================================
WORLD SCAN SAFETY
==================================================

Config:

scan:

  unload-after-scan: true

Plugin loads chunks gradually.

Unloads chunks after processing.

World scans use the explicitly named world's world-border bounds. The
max-world-chunks limit prevents an accidental scan from loading an
unbounded number of chunks. Reduce the world border or deliberately raise
this limit to scan a larger area. A value of 0 disables the limit.

==================================================
SCAN PROGRESS
==================================================

Command:

/loot progress

Example:

--------------------------------

Loot Scan Progress

World:
Amberlight

Chunks:
145 / 500

Containers:
840 / 2943

Filled:
612

Skipped:
228

--------------------------------

==================================================
SCAN CONTROL
==================================================

Commands:

/loot pause

/loot resume

/loot cancel

==================================================
SCAN LIMITS
==================================================

Default:

One active scan.

Optional queueing:

allow-scan-queue: true

==================================================
PREVIEW SYSTEM
==================================================

Command:

/loot preview <table>

Displays all possible loot.

==================================================
STATISTICS
==================================================

Command:

/loot stats

Displays:

Containers Found

Containers Filled

Containers Skipped

Regions Active

Refills Performed

Most Generated Item

Average Scan Time

==================================================
CONFIG.YML
==================================================

scan:

  chunks-per-tick: 1

  containers-per-tick: 50

  scan-interval-ticks: 1

  unload-after-scan: true

  max-world-chunks: 100000

regions:

  default-regen-minutes: 60

loot:

  fill-empty-only: true

  fill-partially-empty: false

refill:

  enabled: true

  refill-only-generated: true

  require-empty-chest: true

  allow-partial-refills: false

==================================================
MAIN COMMANDS
==================================================

TABLES

/loot table create <name>

/loot table delete <name>

/loot table rename <old> <new>

/loot table list

/loot table info <table>

/loot table inherit <table> <parent>

/loot table clearinheritance <table>

TABLE EDITING

/loot edit <table>

/loot preview <table>

SCANNING

/loot fillworld <world> <table>

/loot fill loaded <world> <table>

/loot fillselection <table>

REGIONS

/loot region create <name> <table>

/loot region create <name> <table> <regenMinutes>

/loot region remove <name>

/loot region list

/loot region info <name>

/loot region enable <name>

/loot region disable <name>

/loot region settable <region> <table>

/loot region settime <region> <minutes>

REFILLS

/loot refill

/loot refill region <region>

SELECTION

/loot wand

/loot pos1

/loot pos2

/loot clearselection

/loot showselection

SCANS

/loot progress

/loot pause

/loot resume

/loot cancel

UTILITY

/loot stats

/loot reload

==================================================
PERMISSIONS
==================================================

loot.admin

loot.table

loot.edit

loot.fill

loot.refill

loot.region

loot.wand

loot.stats

loot.preview

loot.reload

==================================================
IMPLEMENTATION REQUIREMENTS
==================================================

- Java 8
- Spigot/Paper 1.12.2
- Async queue preparation where safe
- Main-thread inventory modification
- World-specific execution
- Full GUI management
- Immediate config persistence
- Table inheritance support
- Circular inheritance prevention
- Smart refill system
- Random slot generation
- Double chest support
- Chunk queue scanning
- Region regeneration system
- TPS-first design philosophy

The plugin should always prefer slower completion times over TPS loss,
and should remain stable even when processing tens of thousands of
containers across large custom worlds such as Amberlight.
