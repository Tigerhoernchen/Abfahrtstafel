package de.tiger.abfahrtstafel;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class ScheduleManager {

    private final AbfahrtstafelPlugin plugin;
    private final List<TrainLine> trainLines = new ArrayList<>();

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm");

    public ScheduleManager(AbfahrtstafelPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        trainLines.clear();

        File file = new File(plugin.getDataFolder(), "TrainLines.yml");

        if (!file.exists()) {
            plugin.saveResource("TrainLines.yml", false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<Map<?, ?>> rawTrainLines = config.getMapList("trainLines");

        for (Map<?, ?> rawTrainLine : rawTrainLines) {
            String name = String.valueOf(rawTrainLine.get("name"));
            String description = String.valueOf(rawTrainLine.get("description"));

            List<OrderedRailGroup> orderedRailGroups = new ArrayList<>();

            Object rawGroupsObject = rawTrainLine.get("orderedRailGroups");
            if (!(rawGroupsObject instanceof List<?> rawGroups)) {
                continue;
            }

            for (Object rawGroupObject : rawGroups) {
                if (!(rawGroupObject instanceof Map<?, ?> rawGroup)) {
                    continue;
                }

                int orderIndex = Integer.parseInt(
                        String.valueOf(rawGroup.get("orderIndex"))
                );

                String groupName =
                        String.valueOf(rawGroup.get("name"));

                String parentStation =
                        String.valueOf(rawGroup.get("parentStation"));

                String departuresText =
                        String.valueOf(rawGroup.get("departures"));

                boolean finalStop =
                        departuresText.equalsIgnoreCase("final");

                List<LocalTime> departures = new ArrayList<>();

                if (!finalStop) {
                    for (String part : departuresText.split(",")) {
                        departures.add(parseTime(part.trim()));
                    }
                }

                orderedRailGroups.add(new OrderedRailGroup(
                        orderIndex,
                        groupName,
                        parentStation,
                        finalStop,
                        departures
                ));
            }

            orderedRailGroups.sort(
                    Comparator.comparingInt(OrderedRailGroup::getOrderIndex)
            );

            trainLines.add(new TrainLine(
                    name,
                    description,
                    orderedRailGroups
            ));
        }

        plugin.getLogger().info(
                "TrainLines geladen: " + trainLines.size() + " Linien"
        );
    }

    public boolean processNextDeparture(String stationName, String railGroupName) {
        LocalTime now = LocalTime.now();
        ProcessCandidate bestCandidate = null;

        for (TrainLine trainLine : trainLines) {
            List<OrderedRailGroup> groups =
                    trainLine.getOrderedRailGroups();

            for (OrderedRailGroup group : groups) {
                boolean matchingStation =
                        group.getParentStation().equalsIgnoreCase(stationName);

                boolean matchingRailGroup =
                        group.getName().equalsIgnoreCase(railGroupName);

                if (!matchingStation ||
                        !matchingRailGroup ||
                        group.isFinalStop()) {
                    continue;
                }

                for (LocalTime departureTime : group.getDepartures()) {
                    if (isHiddenByStateOrTimeout(
                            group.getParentStation(),
                            group.getName(),
                            trainLine.getName(),
                            departureTime,
                            now
                    )) {
                        continue;
                    }

                    long minutesLate =
                            Duration.between(departureTime, now).toMinutes();

                    if (minutesLate < 0) {
                        continue;
                    }

                    // Fahrten nach Mitternacht ignorieren
                    if (minutesLate > 12 * 60) {
                        continue;
                    }

                    if (minutesLate > plugin.getDepartureTimeoutMinutes()) {
                        continue;
                    }

                    if (bestCandidate == null ||
                            minutesLate < bestCandidate.minutesUntil()) {
                        bestCandidate = new ProcessCandidate(
                                group.getParentStation(),
                                group.getName(),
                                trainLine.getName(),
                                departureTime,
                                minutesLate
                        );
                    }
                }
            }
        }

        if (bestCandidate == null) {
            return false;
        }

        plugin.getRuntimeStateManager().markProcessed(
                bestCandidate.station(),
                bestCandidate.railGroup(),
                bestCandidate.line(),
                bestCandidate.time()
        );

        return true;
    }

    public List<Departure> getNextDepartures(
            String stationName,
            String railGroupName,
            int amount
    ) {
        LocalTime now = LocalTime.now();
        int lookAheadMinutes = plugin.getPlatformLookAheadMinutes();

        List<DepartureCandidate> candidates = new ArrayList<>();

        for (TrainLine trainLine : trainLines) {
            List<OrderedRailGroup> groups =
                    trainLine.getOrderedRailGroups();

            for (int i = 0; i < groups.size(); i++) {
                OrderedRailGroup group = groups.get(i);

                boolean matchingStation =
                        group.getParentStation().equalsIgnoreCase(stationName);

                boolean matchingRailGroup =
                        group.getName().equalsIgnoreCase(railGroupName);

                if (!matchingStation ||
                        !matchingRailGroup ||
                        group.isFinalStop()) {
                    continue;
                }

                String destination = findFinalDestination(groups);
                String via = buildViaText(groups, i);

                for (LocalTime departureTime : group.getDepartures()) {
                    if (isHiddenByStateOrTimeout(
                            group.getParentStation(),
                            group.getName(),
                            trainLine.getName(),
                            departureTime,
                            now
                    )) {
                        continue;
                    }

                    long minutes = minutesUntil(now, departureTime);

                    if (lookAheadMinutes >= 0 &&
                            minutes > lookAheadMinutes) {
                        continue;
                    }

                    long delayMinutes =
                            calculateDelayMinutes(now, departureTime);

                    candidates.add(new DepartureCandidate(
                            minutes,
                            departureTime.format(TIME_FORMATTER),
                            trainLine.getName(),
                            destination,
                            via,
                            group.getName(),
                            delayMinutes
                    ));
                }
            }
        }

        return toDepartures(candidates, amount);
    }

    public List<Departure> getNextDeparturesForStation(
            String stationName,
            int amount
    ) {
        LocalTime now = LocalTime.now();
        int lookAheadMinutes = plugin.getStationLookAheadMinutes();

        List<DepartureCandidate> candidates = new ArrayList<>();

        for (TrainLine trainLine : trainLines) {
            List<OrderedRailGroup> groups =
                    trainLine.getOrderedRailGroups();

            for (int i = 0; i < groups.size(); i++) {
                OrderedRailGroup group = groups.get(i);

                boolean matchingStation =
                        group.getParentStation().equalsIgnoreCase(stationName);

                if (!matchingStation || group.isFinalStop()) {
                    continue;
                }

                String destination = findFinalDestination(groups);
                String via = buildViaText(groups, i);

                for (LocalTime departureTime : group.getDepartures()) {
                    if (isHiddenByStateOrTimeout(
                            group.getParentStation(),
                            group.getName(),
                            trainLine.getName(),
                            departureTime,
                            now
                    )) {
                        continue;
                    }

                    long minutes = minutesUntil(now, departureTime);

                    if (lookAheadMinutes >= 0 &&
                            minutes > lookAheadMinutes) {
                        continue;
                    }

                    long delayMinutes =
                            calculateDelayMinutes(now, departureTime);

                    candidates.add(new DepartureCandidate(
                            minutes,
                            departureTime.format(TIME_FORMATTER),
                            trainLine.getName(),
                            destination,
                            via,
                            group.getName(),
                            delayMinutes
                    ));
                }
            }
        }

        return toDepartures(candidates, amount);
    }

    private boolean isHiddenByStateOrTimeout(
            String station,
            String railGroup,
            String line,
            LocalTime departureTime,
            LocalTime now
    ) {
        if (plugin.getRuntimeStateManager().isProcessed(
                station,
                railGroup,
                line,
                departureTime
        )) {
            return true;
        }

        long minutesLate =
                Duration.between(departureTime, now).toMinutes();

        // Noch in der Zukunft
        if (minutesLate < 0) {
            return false;
        }

        // Wahrscheinlich Fahrt nach Mitternacht am Folgetag
        // Beispiel:
        // Jetzt 23:20, Abfahrt 01:02 -> 1338 Minuten
        if (minutesLate > 12 * 60) {
            return false;
        }

        // Timeout überschritten
        if (minutesLate > plugin.getDepartureTimeoutMinutes()) {
            return true;
        }

        // Nächste planmäßige Fahrt bereits erreicht
        return hasNextDepartureReached(
                station,
                railGroup,
                line,
                departureTime,
                now
        );
    }

    private boolean hasNextDepartureReached(
            String station,
            String railGroup,
            String line,
            LocalTime departureTime,
            LocalTime now
    ) {
        for (TrainLine trainLine : trainLines) {
            if (!trainLine.getName().equalsIgnoreCase(line)) {
                continue;
            }

            for (OrderedRailGroup group :
                    trainLine.getOrderedRailGroups()) {

                boolean matchingStation =
                        group.getParentStation().equalsIgnoreCase(station);

                boolean matchingRailGroup =
                        group.getName().equalsIgnoreCase(railGroup);

                if (!matchingStation ||
                        !matchingRailGroup ||
                        group.isFinalStop()) {
                    continue;
                }

                for (LocalTime otherTime : group.getDepartures()) {
                    if (otherTime.isAfter(departureTime) &&
                            !otherTime.isAfter(now)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private List<Departure> toDepartures(
            List<DepartureCandidate> candidates,
            int amount
    ) {
        return candidates.stream()
                .sorted(Comparator.comparingLong(
                        DepartureCandidate::minutes
                ))
                .limit(amount)
                .map(candidate -> new Departure(
                        candidate.time(),
                        candidate.line(),
                        candidate.destination(),
                        candidate.via(),
                        candidate.platform(),
                        candidate.delayMinutes()
                ))
                .toList();
    }

    private String findFinalDestination(List<OrderedRailGroup> groups) {
        if (groups.isEmpty()) {
            return "Unbekannt";
        }

        return groups.get(groups.size() - 1)
                .getParentStation();
    }

    private String buildViaText(
            List<OrderedRailGroup> groups,
            int currentIndex
    ) {
        List<String> stations = new ArrayList<>();

        for (int i = currentIndex + 1;
             i < groups.size() - 1;
             i++) {
            stations.add(groups.get(i).getParentStation());
        }

        return String.join(" - ", stations);
    }

    private long minutesUntil(LocalTime now, LocalTime departureTime) {
        long minutes =
                Duration.between(now, departureTime).toMinutes();

        // Überfällige Abfahrt innerhalb Timeout
        if (minutes < 0 &&
                Math.abs(minutes)
                        <= plugin.getDepartureTimeoutMinutes()) {
            return minutes;
        }

        // Fahrt nach Mitternacht
        if (minutes < 0) {
            minutes += 24 * 60;
        }

        return minutes;
    }

    private long calculateDelayMinutes(
            LocalTime now,
            LocalTime departureTime
    ) {
        long rawMinutes =
                Duration.between(departureTime, now).toMinutes();

        // Fahrt nach Mitternacht -> keine Verspätung
        if (rawMinutes > 12 * 60) {
            return 0;
        }

        return Math.max(0, rawMinutes);
    }

    private LocalTime parseTime(String text) {
        String[] parts = text.split(":");

        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);

        return LocalTime.of(hour, minute);
    }

    private record DepartureCandidate(
            long minutes,
            String time,
            String line,
            String destination,
            String via,
            String platform,
            long delayMinutes
    ) {
    }

    private record ProcessCandidate(
            String station,
            String railGroup,
            String line,
            LocalTime time,
            long minutesUntil
    ) {
    }
}