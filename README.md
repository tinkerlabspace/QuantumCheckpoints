# QuantumCheckpoints

A Paper plugin that lets players create checkpoints and restore their saved state later.

## Features

- Create checkpoints at your current position or at coordinates
- Restore inventory, health, hunger, XP, and location from a saved checkpoint
- Optional beam markers for checkpoint visibility
- Configurable checkpoint limits, costs, proximity checks, and confirmation timeout
- Player-controlled auto-checkpoints with server-wide limits
- Admin commands for live config updates without restarting the server

## Requirements

- Paper `1.21.3`
- Java `21`

## Installation

1. Build the plugin jar.
2. Drop it into your server's `plugins/` folder.
3. Start the server once to generate `config.yml`.
4. Adjust the config or use the admin commands in game.

## Player Commands

- `/checkpoint` or `/cp` creates a checkpoint at your current location
- `/cp here` creates a checkpoint at your current location
- `/cp at <x> <z>` creates a checkpoint at the given coordinates
- `/cp list` lists your checkpoints
- `/cp restore <id>` restores a checkpoint
- `/cp delete <id>` deletes a checkpoint
- `/cp deleteall` removes all of your checkpoints
- `/cp auto` shows your auto-checkpoint settings
- `/cp auto on`
- `/cp auto off`
- `/cp auto interval <minutes>`
- `/cp auto reset`
- `/cp confirm`
- `/cp cancel`

## Admin Commands

- `/checkpoints status`
- `/checkpoints reload`
- `/checkpoints enable`
- `/checkpoints disable`
- `/checkpoints clear`
- `/checkpoints limit <number>`
- `/checkpoints cost <material> <amount>`
- `/checkpoints penalty <true|false>`
- `/checkpoints proximity <radius>`
- `/checkpoints beamheight <height>`
- `/checkpoints viewdistance <distance>`
- `/checkpoints timeout <seconds>`
- `/checkpoints auto`
- `/checkpoints auto on`
- `/checkpoints auto off`
- `/checkpoints auto interval <minutes>`
- `/checkpoints auto mininterval <minutes>`

## Permissions

- `quantumcheckpoints.use`: Allows player checkpoint commands
- `quantumcheckpoints.admin`: Allows admin checkpoint commands

## Config

Main config values:

- `checkpoints-enabled`
- `checkpoint-limit`
- `cost.material`
- `cost.amount`
- `penalty-enabled`
- `auto-checkpoint-enabled`
- `auto-checkpoint-interval`
- `auto-checkpoint-min-interval`
- `proximity-radius`
- `beam-height`
- `particle-view-distance`
- `confirmation-timeout`

## Development

Build:

```bash
./gradlew build
```

Run a local Paper server:

```bash
./gradlew runServer
```
