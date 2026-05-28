package de.tiger.abfahrtstafel;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RuntimeStateManager {

    private final Set<String> processedDepartures = new HashSet<>();

    /**
     * Key = station|railGroup
     * Value = line|time der eingefahrenen Fahrt
     */
    private final Map<String, ArrivalState> activeArrivals = new HashMap<>();

    public boolean isProcessed(String station, String railGroup, String line, LocalTime time) {
        return processedDepartures.contains(createDepartureKey(station, railGroup, line, time));
    }

    public void markProcessed(String station, String railGroup, String line, LocalTime time) {
        processedDepartures.add(createDepartureKey(station, railGroup, line, time));
        clearArrival(station, railGroup);
    }

    public void setArrival(String station,
                           String railGroup,
                           String line,
                           LocalTime departureTime) {

        if (station == null || railGroup == null || line == null) {
            return;
        }

        activeArrivals.put(
                createArrivalKey(station, railGroup),
                new ArrivalState(line, departureTime)
        );
    }

    public void clearArrival(String station, String railGroup) {
        if (station == null || railGroup == null) {
            return;
        }

        activeArrivals.remove(createArrivalKey(station, railGroup));
    }

    public boolean isArrival(String station,
                             String railGroup,
                             String line,
                             String departureTimeText) {

        if (station == null || railGroup == null || line == null) {
            return false;
        }

        ArrivalState state = activeArrivals.get(createArrivalKey(station, railGroup));

        if (state == null) {
            return false;
        }

        if (!state.line().equalsIgnoreCase(line)) {
            return false;
        }

        if (state.departureTime() == null) {
            return true;
        }

        if (departureTimeText == null || departureTimeText.isBlank()) {
            return false;
        }

        try {
            LocalTime departureTime = LocalTime.parse(departureTimeText);
            return state.departureTime().equals(departureTime);
        } catch (Exception e) {
            return false;
        }
    }

    public void clear() {
        processedDepartures.clear();
        activeArrivals.clear();
    }

    private String createDepartureKey(String station,
                                      String railGroup,
                                      String line,
                                      LocalTime time) {

        return LocalDate.now()
                + "|"
                + station.toLowerCase()
                + "|"
                + railGroup.toLowerCase()
                + "|"
                + line.toLowerCase()
                + "|"
                + time;
    }

    private String createArrivalKey(String station, String railGroup) {
        return station.toLowerCase()
                + "|"
                + railGroup.toLowerCase();
    }

    private record ArrivalState(
            String line,
            LocalTime departureTime
    ) {
    }
}