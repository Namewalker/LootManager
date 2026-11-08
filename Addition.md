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

notes: the registry is intentionally cached once per world. After a world is
built once with /loot registry rebuild <world>, fill operations and region
refills can use that cached registry without repeating the rebuild. A rebuild
is only needed again when the cache is explicitly cleared or the world state
has changed and an admin decides to refresh it.
