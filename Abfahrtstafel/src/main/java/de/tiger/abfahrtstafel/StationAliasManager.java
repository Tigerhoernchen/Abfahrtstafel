package de.tiger.abfahrtstafel;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class StationAliasManager {

    private final AbfahrtstafelPlugin plugin;
    private final Map<String, String> aliases = new HashMap<>();

    public StationAliasManager(AbfahrtstafelPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        aliases.clear();

        File file = new File(plugin.getDataFolder(), "StationAliases.yml");

        if (!file.exists()) {
            plugin.saveResource("StationAliases.yml", false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        if (config.isConfigurationSection("aliases")) {
            for (String key : config.getConfigurationSection("aliases").getKeys(false)) {
                String value = config.getString("aliases." + key, "");

                if (!value.isBlank()) {
                    aliases.put(key.toLowerCase(), value);
                }
            }
        }

        plugin.getLogger().info("StationAliases geladen: " + aliases.size());
    }

    public String resolve(String stationOrAlias) {
        if (stationOrAlias == null || stationOrAlias.isBlank()) {
            return stationOrAlias;
        }

        return aliases.getOrDefault(
                stationOrAlias.toLowerCase(),
                stationOrAlias
        );
    }
}