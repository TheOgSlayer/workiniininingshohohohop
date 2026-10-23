# QuickShop

A Paper plugin with a spam-clickable **Quick Buy** chest GUI, category shops, an enchanted-book shop and a **shards** currency.

## Build
Requires JDK 21 and Maven (or just push to GitHub: the workflow in `.github/workflows/build.yml` builds the jar on every push and uploads it as an artifact).

    mvn clean package        # -> target/QuickShop-1.0.0.jar

To target a different server, change **three** places: `<java.version>` and `<paper.version>` in `pom.xml`,
`api-version` in `plugin.yml`, and `java-version` in the workflow.
(Paper 1.20.5+ needs Java 21. The code uses 1.20.5+/1.21 APIs, so older servers need small changes.)

## Install
Drop the jar in `plugins/`. Vault + an economy plugin (e.g. EssentialsX) are only needed for money purchases; the plugin loads without them.

## Commands
| Command | Who | |
|---|---|---|
| `/shop`, `/quickbuy` | everyone | open Quick Buy (works even if another plugin owns `/shop`) |
| `/shop reload` | `quickshop.reload` | reload config.yml + prices.yml |
| `/shards` | everyone | your balance |
| `/shards <player>` | everyone | another player's balance |
| `/shards pay <player> <amount>` | everyone | send shards |
| `/shards give/take/set <player> <amount>` | `quickshop.shards.admin` | admin |

## Using Quick Buy
* Click an empty slot -> pick a shop -> pick an item (-> pick an amount) -> it is saved in that slot.
* Left click a saved slot = buy. Right click = remove. Buying never refreshes the GUI and has no cooldown.
* The stack size shown in a slot is the amount you get per click. The lore shows the total price for one click.

## Files
* `prices.yml` - all prices (per item). Sample prices are included, replace them with yours. The `shards` section is in shards and its keys are mob names. `books` lists enchantments and per-level prices.
* `config.yml` - shards per kill, kill notifications, spawner command template.
* `data.yml` - generated: Quick Buy layouts and shard balances (saved every 30s and on shutdown).

## Notes
* Playtime shards: 3 shards for every 5 minutes online (configurable under `shards.playtime`). Progress is saved, so relogging does not reset it, and the player gets a chat message.
* Spawner purchases run the console command from `config.yml` (`givespawner {player} {mob} {amount}`). If it fails the shards are refunded.
* Spawner purchases do not do an "inventory full" check, because the spawner command gives the item, not this plugin.
