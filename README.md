# HeadIndex
Head Index is an easy-to-use server-side head database mod for the Fabric Loader. 
Head Index provides easy access to thousands of heads provided by minecraft-heads.com. 
Unlike a plugin, Head Index can also be used in singleplayer without the need for a server. 
New heads are added all the time and will be automatically added whenever you restart the server without needing to update. 
Head Index also supports setting an item, xp, head trade-in, or economy (Common Economy API) cost for survival servers.
The menus use a custom background through a [Polymer](https://polymer.pb4.eu) resource pack. Players without the pack get the regular chest layout.

Head Index is powered by [Minecraft Heads](https://minecraft-heads.com).
To use the mod, you must enter a free [license key](https://minecraft-heads.com/wiki/minecraft-heads/api-v2-for-users) into the config file.

# Usage
### Commands

- `/head` - Opens head GUI

- `/head menu` - Also opens head GUI

- `/head search <search>` - Opens search menu

- `/head player <playername>` - Get a specific player head

### Config
`config/head-index.json`
```json
{
  "_licenseComment": "Enter your license key below. Register here: https://minecraft-heads.com/wiki/minecraft-heads/api-v2-for-users",
  "license": "LICENSE_HERE",
  "_permissionComment": "The default permission level for the commands. Set to 0 to allow all players access",
  "permissionLevel": 2,
  "_economyComment": "The type of economy to use. Set to FREE to disable economy, ITEM to use an item, TAG to use a tag, ECONOMY to use an economy currency, LEVEL to use minecraft levels, LEVELPOINTS to use level points, or HEAD to trade in any player or mob head for another head",
  "economyType": "HEAD",
  "_costComment": "The identifier for the item, tag or currency to use for the cost, only needed if economyType is set to ITEM, TAG, or ECONOMY",
  "costType": "minecraft:diamond",
  "_costAmountComment": "The amount of the item, head, currency or level to use for the cost",
  "costAmount": 1
}
```

### Head trade-in
`HEAD` is the default. With `"economyType": "HEAD"` players pay for a head with another head instead of an item or currency. Any head from the `minecraft:skulls` tag
(player heads, including database heads, and the zombie, skeleton, wither skeleton, creeper, piglin and dragon heads) is accepted.
The head menu has a marked trade-in slot in the bottom right corner. Players put heads in it, and picking a head takes `costAmount` heads
(`1` is a 1:1 trade) out of that slot. Heads left in the slot are returned when the menu closes.
The player name lookup in the main menu has no trade-in slot, so it takes the heads from the player's inventory instead.

### Resource pack
Head Index adds its menu textures to the Polymer resource pack. You need to host and send the pack to players, for example with
[Polymer AutoHost](https://modrinth.com/mod/polymer). The textures can be regenerated with `python3 tools/generate_gui_textures.py`.

### Permissions - Compatible with LuckPerms, PlayerRoles, or any other farbic permission manager
You can adjust the default permission level in the config file to allow all players access without a permission manager.

`headindex.menu` - Grants /head and /head menu - Defaults to level 2 OP

`headindex.search` - Grants /head search and button in GUI - Defaults to level 2 OP

`headindex.playername` - Grants custom heads in GUI - Defaults to level 2 OP
