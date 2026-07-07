# CobbleDollarsCommandShop-Fabric

Initial Fabric port (Minecraft 1.21.1) of CobbleDollarsCommandShop.

## Implemented core features
- Fabric Loom project setup for MC 1.21.1
- Fabric entrypoint and server lifecycle hooks
- Command-driven shop and bank runtime (`/cdshop ...`)
- Persistent shop + per-player stock state (`config/cobbledollarscommandshops/state.json`)
- Config loading/reload (`config/cobbledollarscommandshops/config.json`)
- Audit logging (`config/cobbledollarscommandshops/audit.log`)
- Basic S2C networking channel for optional client UI state sync
- English/French language resource stubs

## Main commands
- `/cdshop reload`
- `/cdshop bank balance [shopId]`
- `/cdshop bank deposit <amount> [shopId]`
- `/cdshop bank withdraw <amount> [shopId]`
- `/cdshop shop create <shopId>`
- `/cdshop shop setprice <shopId> <price>`
- `/cdshop shop visibility <shopId> <public|private>`
- `/cdshop shop allow <shopId> <player>`
- `/cdshop shop buy <shopId> <quantity>`
- `/cdshop shop stock <shopId>`

## Notes
This is a practical initial Fabric compatibility layer focused on core server behavior. NeoForge-specific integrations that do not have direct Fabric equivalents are intentionally omitted or simplified.
