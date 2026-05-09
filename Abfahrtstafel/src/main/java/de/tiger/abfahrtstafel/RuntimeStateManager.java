package de.tiger.abfahrtstafel;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

public class RuntimeStateManager {

    private final Set<String> processedDepartures = new HashSet<>();

    public boolean isProcessed(String station, String railGroup, String line, LocalTime time) {
        return processedDepartures.contains(createKey(station, railGroup, line, time));
    }

    public void markProcessed(String station, String railGroup, String line, LocalTime time) {
        processedDepartures.add(createKey(station, railGroup, line, time));
    }

    public void clear() {
        processedDepartures.clear();
    }

    private String createKey(String station, String railGroup, String line, LocalTime time) {
        return LocalDate.now() + "|" + station.toLowerCase() + "|" + railGroup.toLowerCase() + "|" + line.toLowerCase() + "|" + time;
    }
}