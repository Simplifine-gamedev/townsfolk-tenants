# Townsfolk: Tenants

A Minecraft Fabric mod that lets you build hotel rooms and collect rent from villager tenants.

## Features

- **Hotel Room System**: Use vanilla wall signs to designate rooms as hotel rooms
- **Automatic Tenants**: Villager tenants spawn in valid rooms with beds
- **Rent Collection**: Tenants pay emeralds to tip chests daily
- **Room Quality Tiers**: 5 quality levels from Budget to Presidential
- **Dynamic Sign Colors**: Signs change color based on room status (yellow=vacant, green=occupied, red=invalid)
- **Guest Ledger**: View all room information in a book-style GUI

## Requirements

- Minecraft 1.21.1
- Fabric Loader 0.16.5+
- Fabric API
- Townsfolk: Signs (included as jar-in-jar)

## How to Use

1. Build an enclosed room with a bed inside
2. Place a wall sign on the **outside** of the room
3. Right-click the sign while sneaking to designate it as a "Hotel" room
4. The sign will turn yellow (vacant) if the room is valid
5. Use `/hotel fill` to spawn tenants immediately, or wait for natural spawning
6. Place a Tip Chest nearby for tenants to deposit rent

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/hotel info` | Show all hotel rooms and their status | All |
| `/hotel fill` | Spawn tenants in all vacant rooms | OP |
| `/hotel payday` | Force all tenants to pay rent immediately | OP |
| `/hotel evict <room>` | Evict tenant from specific room | OP |
| `/hotel evictall` | Evict all tenants | OP |
| `/hotel reset` | Remove all room data | OP |

## Items

- **Guest Ledger**: Right-click to open a book showing all room information
- **Tip Chest**: Special chest where tenants deposit their rent payments

## Room Validation

A valid hotel room requires:
- Enclosed space (walls, floor, ceiling)
- At least one bed inside
- Wall sign placed on the exterior

## License

MIT License - See LICENSE file for details.

## Links

- [Modrinth](https://modrinth.com/mod/townsfolk-tenants)
- [CurseForge](https://www.curseforge.com/minecraft/mc-mods/townsfolk-tenants)
- [GitHub](https://github.com/Simplifine-gamedev/townsfolk-tenants)
