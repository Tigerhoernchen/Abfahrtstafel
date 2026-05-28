package de.tiger.abfahrtstafel;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

                int orderIndex = Integer.parseInt(String.valueOf(rawGroup.get("orderIndex")));
                String groupName = String.valueOf(rawGroup.get("name"));
                String parentStation = String.valueOf(rawGroup.get("parentStation"));
                String departuresText = String.valueOf(rawGroup.get("departures"));

                boolean finalStop = departuresText.equalsIgnoreCase("final");
                boolean onDemand = departuresText.equalsIgnoreCase("ondemand");

                List<LocalTime> departures = new ArrayList<>();

                if (!finalStop && !onDemand) {
                    for (String part : departuresText.split(",")) {
                        departures.add(parseTime(part.trim()));
                    }
                }

                String arrivalPlatformSound = getString(rawGroup, "arrivalPlatformSound", "");
                String arrivalTrainSound = getString(rawGroup, "arrivalTrainSound", "");

                orderedRailGroups.add(new OrderedRailGroup(
                        orderIndex,
                        groupName,
                        parentStation,
                        finalStop,
                        departures,
                        arrivalPlatformSound,
                        arrivalTrainSound,
                        onDemand
                ));
            }

            orderedRailGroups.sort(Comparator.comparingInt(OrderedRailGroup::getOrderIndex));
            trainLines.add(new TrainLine(name, description, orderedRailGroups));
        }

        plugin.getLogger().info("TrainLines geladen: " + trainLines.size() + " Linien");
    }

    private String getString(Map<?, ?> map, String key, String defaultValue) {
        Object value = map.get(key);

        if (value == null) {
            return defaultValue;
        }

        return String.valueOf(value);
    }

    public List<String> getStationNames() {
        Set<String> stations = new HashSet<>();

        for (TrainLine trainLine : trainLines) {
            for (OrderedRailGroup group : trainLine.getOrderedRailGroups()) {
                stations.add(group.getParentStation());
            }
        }

        return stations.stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    public List<String> getRailGroupNames() {
        Set<String> railGroups = new HashSet<>();

        for (TrainLine trainLine : trainLines) {
            for (OrderedRailGroup group : trainLine.getOrderedRailGroups()) {
                if (!group.isFinalStop()) {
                    railGroups.add(group.getName());
                }
            }
        }

        return railGroups.stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    public boolean processNextDeparture(String stationName, String railGroupName) {
        LocalTime now = LocalTime.now();
        ProcessCandidate bestCandidate = null;

        for (TrainLine trainLine : trainLines) {
            for (OrderedRailGroup group : trainLine.getOrderedRailGroups()) {
                boolean matchingStation = group.getParentStation().equalsIgnoreCase(stationName);
                boolean matchingRailGroup = group.getName().equalsIgnoreCase(railGroupName);

                if (!matchingStation || !matchingRailGroup || group.isFinalStop()) {
                    continue;
                }

                if (group.isOnDemand()) {
                    plugin.getRuntimeStateManager().clearArrival(
                            group.getParentStation(),
                            group.getName()
                    );

                    return true;
                }

                for (LocalTime departureTime : group.getDepartures()) {
                    if (isHiddenByStateOrTimeout(group.getParentStation(), group.getName(), trainLine.getName(), departureTime, now)) {
                        continue;
                    }

                    long minutesLate = Duration.between(departureTime, now).toMinutes();

                    if (minutesLate < 0) {
                        continue;
                    }

                    if (minutesLate > 12 * 60) {
                        continue;
                    }

                    if (minutesLate > plugin.getDepartureTimeoutMinutes()) {
                        continue;
                    }

                    if (bestCandidate == null || minutesLate < bestCandidate.minutesUntil()) {
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

    public List<Departure> getNextDepartures(String stationName, String railGroupName, int amount) {
        LocalTime now = LocalTime.now();
        int lookAheadMinutes = plugin.getPlatformLookAheadMinutes();

        List<DepartureCandidate> candidates = new ArrayList<>();

        for (TrainLine trainLine : trainLines) {
            List<OrderedRailGroup> groups = trainLine.getOrderedRailGroups();

            for (int i = 0; i < groups.size(); i++) {
                OrderedRailGroup group = groups.get(i);

                boolean matchingStation = group.getParentStation().equalsIgnoreCase(stationName);
                boolean matchingRailGroup = group.getName().equalsIgnoreCase(railGroupName);

                if (!matchingStation || !matchingRailGroup || group.isFinalStop()) {
                    continue;
                }

                String destination = findFinalDestination(groups);
                String via = buildViaText(groups, i);

                if (group.isOnDemand()) {
                    candidates.add(new DepartureCandidate(
                            0,
                            "",
                            trainLine.getName(),
                            destination,
                            via,
                            group.getName(),
                            0,
                            true
                    ));

                    continue;
                }

                for (LocalTime departureTime : group.getDepartures()) {
                    if (isHiddenByStateOrTimeout(group.getParentStation(), group.getName(), trainLine.getName(), departureTime, now)) {
                        continue;
                    }

                    long minutes = minutesUntil(now, departureTime);

                    if (lookAheadMinutes >= 0 && minutes > lookAheadMinutes) {
                        continue;
                    }

                    long delayMinutes = calculateDelayMinutes(now, departureTime);

                    candidates.add(new DepartureCandidate(
                            minutes,
                            departureTime.format(TIME_FORMATTER),
                            trainLine.getName(),
                            destination,
                            via,
                            group.getName(),
                            delayMinutes,
                            false
                    ));
                }
            }
        }

        return toDepartures(candidates, amount);
    }

    public List<Departure> getNextDeparturesForStation(String stationName, int amount) {
        LocalTime now = LocalTime.now();
        int lookAheadMinutes = plugin.getStationLookAheadMinutes();

        List<DepartureCandidate> candidates = new ArrayList<>();

        for (TrainLine trainLine : trainLines) {
            List<OrderedRailGroup> groups = trainLine.getOrderedRailGroups();

            for (int i = 0; i < groups.size(); i++) {
                OrderedRailGroup group = groups.get(i);

                boolean matchingStation = group.getParentStation().equalsIgnoreCase(stationName);

                if (!matchingStation || group.isFinalStop()) {
                    continue;
                }

                String destination = findFinalDestination(groups);
                String via = buildViaText(groups, i);

                if (group.isOnDemand()) {
                    candidates.add(new DepartureCandidate(
                            0,
                            "",
                            trainLine.getName(),
                            destination,
                            via,
                            group.getName(),
                            0,
                            true
                    ));

                    continue;
                }

                for (LocalTime departureTime : group.getDepartures()) {
                    if (isHiddenByStateOrTimeout(group.getParentStation(), group.getName(), trainLine.getName(), departureTime, now)) {
                        continue;
                    }

                    long minutes = minutesUntil(now, departureTime);

                    if (lookAheadMinutes >= 0 && minutes > lookAheadMinutes) {
                        continue;
                    }

                    long delayMinutes = calculateDelayMinutes(now, departureTime);

                    candidates.add(new DepartureCandidate(
                            minutes,
                            departureTime.format(TIME_FORMATTER),
                            trainLine.getName(),
                            destination,
                            via,
                            group.getName(),
                            delayMinutes,
                            false
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
        if (plugin.getRuntimeStateManager().isProcessed(station, railGroup, line, departureTime)) {
            return true;
        }

        long minutesLate = Duration.between(departureTime, now).toMinutes();

        if (minutesLate < 0) {
            return false;
        }

        if (minutesLate > 12 * 60) {
            return false;
        }

        if (minutesLate > plugin.getDepartureTimeoutMinutes()) {
            return true;
        }

        return hasNextDepartureReached(station, railGroup, line, departureTime, now);
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

            for (OrderedRailGroup group : trainLine.getOrderedRailGroups()) {
                boolean matchingStation = group.getParentStation().equalsIgnoreCase(station);
                boolean matchingRailGroup = group.getName().equalsIgnoreCase(railGroup);

                if (!matchingStation || !matchingRailGroup || group.isFinalStop() || group.isOnDemand()) {
                    continue;
                }

                for (LocalTime otherTime : group.getDepartures()) {
                    if (otherTime.isAfter(departureTime) && !otherTime.isAfter(now)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private List<Departure> toDepartures(List<DepartureCandidate> candidates, int amount) {
        return candidates.stream()
                .sorted(Comparator.comparingLong(DepartureCandidate::minutes))
                .limit(amount)
                .map(candidate -> new Departure(
                        candidate.time(),
                        candidate.line(),
                        candidate.destination(),
                        candidate.via(),
                        candidate.platform(),
                        candidate.delayMinutes(),
                        candidate.onDemand()
                ))
                .toList();
    }

    private String findFinalDestination(List<OrderedRailGroup> groups) {
        if (groups.isEmpty()) {
            return "Unbekannt";
        }

        return groups.get(groups.size() - 1).getParentStation();
    }

    private String buildViaText(List<OrderedRailGroup> groups, int currentIndex) {
        List<String> stations = new ArrayList<>();

        for (int i = currentIndex + 1; i < groups.size() - 1; i++) {
            stations.add(groups.get(i).getParentStation());
        }

        return String.join(" - ", stations);
    }

    private long minutesUntil(LocalTime now, LocalTime departureTime) {
        long minutes = Duration.between(now, departureTime).toMinutes();

        if (minutes < 0 && Math.abs(minutes) <= plugin.getDepartureTimeoutMinutes()) {
            return minutes;
        }

        if (minutes < 0) {
            minutes += 24 * 60;
        }

        return minutes;
    }

    private long calculateDelayMinutes(LocalTime now, LocalTime departureTime) {
        long rawMinutes = Duration.between(departureTime, now).toMinutes();

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
            long delayMinutes,
            boolean onDemand
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

    public record ArrivalCandidate(
            long minutes,
            String station,
            String railGroup,
            String line,
            LocalTime departureTime,
            String destination,
            String via,
            long delayMinutes,
            OrderedRailGroup orderedRailGroup
    ) {
    }

    public ArrivalCandidate findNextArrivalCandidate(String stationName, String railGroupName) {
        LocalTime now = LocalTime.now();
        ArrivalCandidate bestCandidate = null;

        for (TrainLine trainLine : trainLines) {
            List<OrderedRailGroup> groups = trainLine.getOrderedRailGroups();

            for (int i = 0; i < groups.size(); i++) {
                OrderedRailGroup group = groups.get(i);

                boolean matchingStation = group.getParentStation().equalsIgnoreCase(stationName);
                boolean matchingRailGroup = group.getName().equalsIgnoreCase(railGroupName);

                if (!matchingStation || !matchingRailGroup) {
                    continue;
                }

                String destination = findFinalDestination(groups);
                String via = buildViaText(groups, i);

                if (group.isFinalStop()) {
                    boolean hasSound =
                            !group.getArrivalPlatformSound().isBlank()
                                    || !group.getArrivalTrainSound().isBlank();

                    if (!hasSound) {
                        continue;
                    }

                    return new ArrivalCandidate(
                            0,
                            group.getParentStation(),
                            group.getName(),
                            trainLine.getName(),
                            LocalTime.now(),
                            destination,
                            via,
                            0,
                            group
                    );
                }

                if (group.isOnDemand()) {
                    return new ArrivalCandidate(
                            0,
                            group.getParentStation(),
                            group.getName(),
                            trainLine.getName(),
                            null,
                            destination,
                            via,
                            0,
                            group
                    );
                }

                for (LocalTime departureTime : group.getDepartures()) {
                    if (isHiddenByStateOrTimeout(group.getParentStation(), group.getName(), trainLine.getName(), departureTime, now)) {
                        continue;
                    }

                    long minutes = minutesUntil(now, departureTime);
                    int arrivalLookAheadMinutes = plugin.getArrivalTriggerLookAheadMinutes();

                    if (minutes < -plugin.getDepartureTimeoutMinutes()) {
                        continue;
                    }

                    if (arrivalLookAheadMinutes >= 0 && minutes > arrivalLookAheadMinutes) {
                        continue;
                    }

                    long delayMinutes = calculateDelayMinutes(now, departureTime);

                    if (bestCandidate == null || minutes < bestCandidate.minutes()) {
                        bestCandidate = new ArrivalCandidate(
                                minutes,
                                group.getParentStation(),
                                group.getName(),
                                trainLine.getName(),
                                departureTime,
                                destination,
                                via,
                                delayMinutes,
                                group
                        );
                    }
                }
            }
        }

        return bestCandidate;
    }
}