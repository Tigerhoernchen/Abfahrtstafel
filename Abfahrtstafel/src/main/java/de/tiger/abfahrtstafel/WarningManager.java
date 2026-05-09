package de.tiger.abfahrtstafel;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WarningManager {

    private final AbfahrtstafelPlugin plugin;
    private final List<WarnMessage> warnings = new ArrayList<>();

    public WarningManager(AbfahrtstafelPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        warnings.clear();

        File file = new File(plugin.getDataFolder(), "WarnMessages.yml");

        if (!file.exists()) {
            plugin.saveResource("WarnMessages.yml", false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<Map<?, ?>> rawWarnings = config.getMapList("warnMessages");

        for (Map<?, ?> rawWarning : rawWarnings) {
            int id = Integer.parseInt(String.valueOf(rawWarning.get("id")));
            String message = String.valueOf(rawWarning.get("message"));
            boolean active = Boolean.parseBoolean(String.valueOf(rawWarning.get("active")));
            String groupsText = String.valueOf(rawWarning.get("groups"));

            List<String> groups = new ArrayList<>();

            for (String group : groupsText.split(",")) {
                groups.add(group.trim());
            }

            warnings.add(new WarnMessage(id, message, active, groups));
        }

        plugin.getLogger().info("WarnMessages geladen: " + warnings.size() + " Warnungen");
    }

    public List<WarnMessage> getActiveWarnings(String station, String railGroup) {
        List<WarnMessage> result = new ArrayList<>();

        String stationGroup = station;
        String platformGroup = station + ":" + railGroup;

        for (WarnMessage warning : warnings) {
            if (!warning.isActive()) {
                continue;
            }

            for (String group : warning.getGroups()) {
                if (group.equalsIgnoreCase("global")
                        || group.equalsIgnoreCase(stationGroup)
                        || group.equalsIgnoreCase(platformGroup)) {
                    result.add(warning);
                    break;
                }
            }
        }

        return result;
    }

    public List<WarnMessage> getWarnings() {
        return warnings;
    }

    public boolean setActive(int id, boolean active) {
        File file = new File(plugin.getDataFolder(), "WarnMessages.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        List<Map<?, ?>> rawWarnings = config.getMapList("warnMessages");
        boolean changed = false;

        for (int i = 0; i < rawWarnings.size(); i++) {
            Map<?, ?> rawWarning = rawWarnings.get(i);

            int warningId = Integer.parseInt(String.valueOf(rawWarning.get("id")));

            if (warningId == id) {
                @SuppressWarnings("unchecked")
                Map<String, Object> editable = (Map<String, Object>) rawWarning;

                editable.put("active", active);
                changed = true;
                break;
            }
        }

        if (!changed) {
            return false;
        }

        // Komplette Liste wieder zurückschreiben
        config.set("warnMessages", rawWarnings);

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("WarnMessages.yml konnte nicht gespeichert werden: " + e.getMessage());
            return false;
        }

        load();
        return true;
    }
}