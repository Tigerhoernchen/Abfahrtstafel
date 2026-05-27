package de.tiger.abfahrtstafel;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class SoundMessageManager {

    private final AbfahrtstafelPlugin plugin;
    private final List<SoundMessage> soundMessages = new ArrayList<>();
    private final Random random = new Random();

    public SoundMessageManager(AbfahrtstafelPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        soundMessages.clear();

        File file = new File(plugin.getDataFolder(), "SoundMessages.yml");

        if (!file.exists()) {
            plugin.saveResource("SoundMessages.yml", false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<Map<?, ?>> rawMessages = config.getMapList("soundMessages");

        for (Map<?, ?> rawMessage : rawMessages) {
            int id = Integer.parseInt(String.valueOf(rawMessage.get("id")));
            String description = getString(rawMessage, "description", "");
            boolean enabled = Boolean.parseBoolean(getString(rawMessage, "enabled", "false"));
            String groupsText = getString(rawMessage, "groups", "");
            String sound = getString(rawMessage, "sound", "");
            String mode = getString(rawMessage, "mode", "interval");

            int intervalSeconds = getInt(rawMessage, "intervalSeconds", 300);
            int minIntervalSeconds = getInt(rawMessage, "minIntervalSeconds", 600);
            int maxIntervalSeconds = getInt(rawMessage, "maxIntervalSeconds", 1200);

            List<String> groups = parseGroups(groupsText);
            List<LocalTime> times = parseTimes(getString(rawMessage, "times", ""));

            SoundMessage message = new SoundMessage(
                    id,
                    description,
                    enabled,
                    groups,
                    sound,
                    mode,
                    intervalSeconds,
                    minIntervalSeconds,
                    maxIntervalSeconds,
                    times
            );

            scheduleNext(message, true);
            soundMessages.add(message);
        }

        plugin.getLogger().info("SoundMessages geladen: " + soundMessages.size());
    }

    public void tick() {
        long nowMillis = System.currentTimeMillis();

        for (SoundMessage message : soundMessages) {
            if (!message.isEnabled()) {
                continue;
            }

            if (message.getSound() == null || message.getSound().isBlank()) {
                continue;
            }

            String mode = message.getMode();

            if ("interval".equalsIgnoreCase(mode) || "random".equalsIgnoreCase(mode)) {
                if (nowMillis >= message.getNextPlayMillis()) {
                    play(message);
                    scheduleNext(message, false);
                }

                continue;
            }

            if ("time".equalsIgnoreCase(mode)) {
                checkTimeMode(message);
            }
        }
    }

    public List<SoundMessage> getSoundMessages() {
        return soundMessages;
    }

    public boolean setEnabled(int id, boolean enabled) {
        File file = new File(plugin.getDataFolder(), "SoundMessages.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        List<Map<?, ?>> rawMessages = config.getMapList("soundMessages");
        boolean changed = false;

        for (int i = 0; i < rawMessages.size(); i++) {
            Map<?, ?> rawMessage = rawMessages.get(i);

            int messageId = Integer.parseInt(String.valueOf(rawMessage.get("id")));

            if (messageId == id) {
                @SuppressWarnings("unchecked")
                Map<String, Object> editable = (Map<String, Object>) rawMessage;

                editable.put("enabled", enabled);
                changed = true;
                break;
            }
        }

        if (!changed) {
            return false;
        }

        config.set("soundMessages", rawMessages);

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("SoundMessages.yml konnte nicht gespeichert werden: " + e.getMessage());
            return false;
        }

        load();
        return true;
    }

    public boolean playNow(int id) {
        for (SoundMessage message : soundMessages) {
            if (message.getId() == id) {
                play(message);
                scheduleNext(message, false);
                return true;
            }
        }

        return false;
    }

    private void play(SoundMessage message) {
        for (String group : message.getGroups()) {
            playGroup(group, message.getSound());
        }
    }

    private void playGroup(String group, String sound) {
        if (group == null || group.isBlank()) {
            return;
        }

        if ("global".equalsIgnoreCase(group)) {
            for (SoundBox box : plugin.getSoundManager().getSoundBoxes()) {
                plugin.getSoundManager().playPlatformSound(
                        box.getStation(),
                        box.getRailGroup(),
                        sound
                );
            }

            return;
        }

        if (group.toLowerCase().startsWith("lines:")) {
            String lineName = group.substring("lines:".length()).trim();

            for (SoundBox box : plugin.getSoundManager().getSoundBoxes()) {
                List<Departure> departures = plugin
                        .getScheduleManager()
                        .getNextDepartures(
                                box.getStation(),
                                box.getRailGroup(),
                                plugin.getStationDisplayMaxEntries()
                        );

                boolean lineActive = false;

                for (Departure departure : departures) {
                    if (departure.getLine().equalsIgnoreCase(lineName)) {
                        lineActive = true;
                        break;
                    }
                }

                if (!lineActive) {
                    continue;
                }

                plugin.getSoundManager().playPlatformSound(
                        box.getStation(),
                        box.getRailGroup(),
                        sound
                );
            }

            return;
        }

        if (group.contains(":")) {
            String[] parts = group.split(":", 2);
            String station = plugin.getStationAliasManager().resolve(parts[0].trim());
            String railGroup = parts[1].trim();

            plugin.getSoundManager().playPlatformSound(
                    station,
                    railGroup,
                    sound
            );

            return;
        }

        String station = plugin.getStationAliasManager().resolve(group.trim());

        for (SoundBox box : plugin.getSoundManager().getSoundBoxes()) {
            if (!box.getStation().equalsIgnoreCase(station)) {
                continue;
            }

            plugin.getSoundManager().playPlatformSound(
                    box.getStation(),
                    box.getRailGroup(),
                    sound
            );
        }
    }

    private void scheduleNext(SoundMessage message, boolean initial) {
        long now = System.currentTimeMillis();

        if ("random".equalsIgnoreCase(message.getMode())) {
            int min = Math.max(1, message.getMinIntervalSeconds());
            int max = Math.max(min, message.getMaxIntervalSeconds());
            int seconds = min + random.nextInt((max - min) + 1);

            if (initial) {
                seconds += plugin.getConfig().getInt("soundMessagesInitialDelaySeconds", 60);
            }

            message.setNextPlayMillis(now + (seconds * 1000L));
            return;
        }

        int seconds = Math.max(1, message.getIntervalSeconds());

        if (initial) {
            seconds += plugin.getConfig().getInt("soundMessagesInitialDelaySeconds", 60);
        }

        message.setNextPlayMillis(now + (seconds * 1000L));
    }

    private void checkTimeMode(SoundMessage message) {
        LocalTime now = LocalTime.now();

        String minuteKey = now.getHour() + ":" + now.getMinute();

        for (LocalTime time : message.getTimes()) {
            if (time.getHour() != now.getHour() || time.getMinute() != now.getMinute()) {
                continue;
            }

            String playKey = message.getId() + ":" + minuteKey;

            if (message.getPlayedMinuteKeys().contains(playKey)) {
                continue;
            }

            play(message);
            message.getPlayedMinuteKeys().add(playKey);
            cleanupPlayedKeys(message);
        }
    }

    private void cleanupPlayedKeys(SoundMessage message) {
        if (message.getPlayedMinuteKeys().size() <= 100) {
            return;
        }

        message.getPlayedMinuteKeys().clear();
    }

    private List<String> parseGroups(String groupsText) {
        List<String> groups = new ArrayList<>();

        if (groupsText == null || groupsText.isBlank()) {
            return groups;
        }

        for (String group : groupsText.split(",")) {
            String trimmed = group.trim();

            if (!trimmed.isBlank()) {
                groups.add(trimmed);
            }
        }

        return groups;
    }

    private List<LocalTime> parseTimes(String timesText) {
        List<LocalTime> times = new ArrayList<>();

        if (timesText == null || timesText.isBlank()) {
            return times;
        }

        for (String part : timesText.split(",")) {
            try {
                times.add(LocalTime.parse(part.trim()));
            } catch (Exception ignored) {
            }
        }

        return times;
    }

    private String getString(Map<?, ?> map, String key, String fallback) {
        Object value = map.get(key);

        if (value == null) {
            return fallback;
        }

        return String.valueOf(value);
    }

    private int getInt(Map<?, ?> map, String key, int fallback) {
        Object value = map.get(key);

        if (value == null) {
            return fallback;
        }

        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}