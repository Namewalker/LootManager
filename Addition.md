==================================================
CONTAINER REGISTRY SYSTEM
==================================================

Purpose:

Avoid repeated chunk scanning.

==================================================

INITIAL DISCOVERY

- Scan region/world
- Discover containers
- Save coordinates to registry

==================================================

REGISTRY STORAGE

containers.yml

worlds:

  Amberlight:

    containers:

      - x: 100
        y: 64
        z: 200
        type: CHEST

==================================================

REGISTRY UPDATES

BlockPlaceEvent

- Add container

BlockBreakEvent

- Remove container

==================================================

REGENERATION

Region regeneration should use
registered containers first.

Chunk scans should only occur when:

- Registry rebuild requested
- Region created
- Admin executes discovery scan

==================================================

COMMANDS

/loot registry rebuild <world>

/loot registry stats

/loot registry validate

==================================================

notes: this will change /loot fillworld to require /loot registry rebuild
to have been ran beforehand. generating loot in a region will also require
the rebuild to have been ran beforehand.
