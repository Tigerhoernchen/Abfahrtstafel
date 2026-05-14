package de.tiger.abfahrtstafel;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.bergerkiller.bukkit.tc.controller.MinecartGroup;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SoundManager {

    private final AbfahrtstafelPlugin plugin;
    private final List<SoundBox> soundBoxes = new ArrayList<>();

    public SoundManager(AbfahrtstafelPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        soundBoxes.clear();

        File file = new File(plugin.getDataFolder(), "SoundBoxes.yml");

        if (!file.exists()) {
            plugin.saveResource("SoundBoxes.yml", false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<Map<?, ?>> rawBoxes = config.getMapList("soundBoxes");

        for (Map<?, ?> rawBox : rawBoxes) {
            int id = (int) getDouble(rawBox, "id", getNextId());

            String station = getString(rawBox, "station", "");
            String railGroup = getString(rawBox, "railGroup", "");
            String world = getString(rawBox, "world", "world");

            double x = getDouble(rawBox, "x", 0.0);
            double y = getDouble(rawBox, "y", 0.0);
            double z = getDouble(rawBox, "z", 0.0);

            float radius = (float) getDouble(rawBox, "radius", 24.0);
            float volume = (float) getDouble(rawBox, "volume", 1.0);
            float pitch = (float) getDouble(rawBox, "pitch", 1.0);

            soundBoxes.add(new SoundBox(
                    id,
                    station,
                    railGroup,
                    world,
                    x,
                    y,
                    z,
                    radius,
                    volume,
                    pitch
            ));
        }

        plugin.getLogger().info("SoundBoxes geladen: " + soundBoxes.size());
    }

    public List<SoundBox> getMatchingSoundBoxes(String station, String railGroup) {
        List<SoundBox> matches = new ArrayList<>();

        for (SoundBox box : soundBoxes) {
            if (!box.getStation().equalsIgnoreCase(station)) {
                continue;
            }

            if (!box.getRailGroup().equalsIgnoreCase(railGroup)) {
                continue;
            }

            matches.add(box);
        }

        return matches;
    }

    public void playPlatformSound(String station,
                                  String railGroup,
                                  String soundName) {

        if (soundName == null || soundName.isBlank()) {
            return;
        }

        for (SoundBox box : getMatchingSoundBoxes(station, railGroup)) {
            World world = Bukkit.getWorld(box.getWorld());

            if (world == null) {
                continue;
            }

            Location location = new Location(
                    world,
                    box.getX(),
                    box.getY(),
                    box.getZ()
            );

            double radiusSquared = box.getRadius() * box.getRadius();

            for (Player player : world.getPlayers()) {
                if (player.getLocation().distanceSquared(location) > radiusSquared) {
                    continue;
                }

                player.playSound(
                        location,
                        soundName,
                        plugin.getAnnouncementSoundCategory(),
                        box.getVolume(),
                        box.getPitch()
                );
            }
        }
    }

    public void playTrainSound(MinecartGroup group,
                               String soundName) {

        if (group == null || soundName == null || soundName.isBlank()) {
            return;
        }

        for (int i = 0; i < group.size(); i++) {
            org.bukkit.entity.Entity entity = group.get(i).getEntity().getEntity();

            if (entity == null) {
                continue;
            }

            for (org.bukkit.entity.Entity passenger : entity.getPassengers()) {
                if (!(passenger instanceof Player player)) {
                    continue;
                }

                player.playSound(
                        player,
                        soundName,
                        plugin.getAnnouncementSoundCategory(),
                        1.0f,
                        1.0f
                );
            }
        }
    }

    public List<SoundBox> getSoundBoxes() {
        return new ArrayList<>(soundBoxes);
    }

    public SoundBox createSoundBox(String station,
                                   String railGroup,
                                   Location location) {

        if (location == null || location.getWorld() == null) {
            return null;
        }

        int id = getNextId();

        float radius = (float) plugin.getConfig()
                .getDouble("soundBoxDefaultRadius", 24.0);

        float volume = (float) plugin.getConfig()
                .getDouble("soundBoxDefaultVolume", 1.0);

        float pitch = (float) plugin.getConfig()
                .getDouble("soundBoxDefaultPitch", 1.0);

        SoundBox box = new SoundBox(
                id,
                station,
                railGroup,
                location.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                radius,
                volume,
                pitch
        );

        soundBoxes.add(box);
        save();

        return box;
    }

    public boolean removeSoundBox(int id) {
        boolean removed = soundBoxes.removeIf(box -> box.getId() == id);

        if (removed) {
            save();
        }

        return removed;
    }

    public void save() {
        File file = new File(plugin.getDataFolder(), "SoundBoxes.yml");
        YamlConfiguration config = new YamlConfiguration();

        List<Map<String, Object>> rawBoxes = new ArrayList<>();

        for (SoundBox box : soundBoxes) {
            Map<String, Object> map = new java.util.LinkedHashMap<>();

            map.put("id", box.getId());
            map.put("station", box.getStation());
            map.put("railGroup", box.getRailGroup());
            map.put("world", box.getWorld());
            map.put("x", box.getX());
            map.put("y", box.getY());
            map.put("z", box.getZ());
            map.put("radius", box.getRadius());
            map.put("volume", box.getVolume());
            map.put("pitch", box.getPitch());

            rawBoxes.add(map);
        }

        config.set("soundBoxes", rawBoxes);

        try {
            config.save(file);
        } catch (Exception e) {
            plugin.getLogger().warning("SoundBoxes.yml konnte nicht gespeichert werden.");
            e.printStackTrace();
        }
    }

    private int getNextId() {
        int maxId = 0;

        for (SoundBox box : soundBoxes) {
            if (box.getId() > maxId) {
                maxId = box.getId();
            }
        }

        return maxId + 1;
    }

    private String getString(Map<?, ?> map,
                             String key,
                             String defaultValue) {

        Object value = map.get(key);

        if (value == null) {
            return defaultValue;
        }

        return String.valueOf(value);
    }

    private double getDouble(Map<?, ?> map,
                             String key,
                             double defaultValue) {

        Object value = map.get(key);

        if (value == null) {
            return defaultValue;
        }

        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}