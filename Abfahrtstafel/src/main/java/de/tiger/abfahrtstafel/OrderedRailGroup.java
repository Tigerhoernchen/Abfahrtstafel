package de.tiger.abfahrtstafel;

import java.time.LocalTime;
import java.util.List;

public class OrderedRailGroup {

    private final int orderIndex;
    private final String name;
    private final String parentStation;
    private final boolean finalStop;
    private final List<LocalTime> departures;
    private final String arrivalPlatformSound;
    private final String arrivalTrainSound;
    private final boolean onDemand;

    public OrderedRailGroup(int orderIndex,
                            String name,
                            String parentStation,
                            boolean finalStop,
                            List<LocalTime> departures,
                            String arrivalPlatformSound,
                            String arrivalTrainSound,
                            boolean onDemand) {
        this.orderIndex = orderIndex;
        this.name = name;
        this.parentStation = parentStation;
        this.finalStop = finalStop;
        this.departures = departures;
        this.arrivalPlatformSound = arrivalPlatformSound;
        this.arrivalTrainSound = arrivalTrainSound;
        this.onDemand = onDemand;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public String getName() {
        return name;
    }

    public String getParentStation() {
        return parentStation;
    }

    public boolean isFinalStop() {
        return finalStop;
    }

    public List<LocalTime> getDepartures() {
        return departures;
    }

    public String getArrivalPlatformSound() {
        return arrivalPlatformSound;
    }

    public String getArrivalTrainSound() {
        return arrivalTrainSound;
    }

    public boolean isOnDemand() {
        return onDemand;
    }
}