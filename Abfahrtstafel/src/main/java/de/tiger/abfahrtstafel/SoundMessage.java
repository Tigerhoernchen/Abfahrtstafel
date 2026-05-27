package de.tiger.abfahrtstafel;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class SoundMessage {

    private final int id;
    private final String description;
    private boolean enabled;
    private final List<String> groups;
    private final String sound;
    private final String mode;
    private final int intervalSeconds;
    private final int minIntervalSeconds;
    private final int maxIntervalSeconds;
    private final List<LocalTime> times;

    private long nextPlayMillis;
    private final List<String> playedMinuteKeys = new ArrayList<>();

    public SoundMessage(int id,
                        String description,
                        boolean enabled,
                        List<String> groups,
                        String sound,
                        String mode,
                        int intervalSeconds,
                        int minIntervalSeconds,
                        int maxIntervalSeconds,
                        List<LocalTime> times) {
        this.id = id;
        this.description = description;
        this.enabled = enabled;
        this.groups = groups;
        this.sound = sound;
        this.mode = mode;
        this.intervalSeconds = intervalSeconds;
        this.minIntervalSeconds = minIntervalSeconds;
        this.maxIntervalSeconds = maxIntervalSeconds;
        this.times = times;
    }

    public int getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getGroups() {
        return groups;
    }

    public String getSound() {
        return sound;
    }

    public String getMode() {
        return mode;
    }

    public int getIntervalSeconds() {
        return intervalSeconds;
    }

    public int getMinIntervalSeconds() {
        return minIntervalSeconds;
    }

    public int getMaxIntervalSeconds() {
        return maxIntervalSeconds;
    }

    public List<LocalTime> getTimes() {
        return times;
    }

    public long getNextPlayMillis() {
        return nextPlayMillis;
    }

    public void setNextPlayMillis(long nextPlayMillis) {
        this.nextPlayMillis = nextPlayMillis;
    }

    public List<String> getPlayedMinuteKeys() {
        return playedMinuteKeys;
    }
}