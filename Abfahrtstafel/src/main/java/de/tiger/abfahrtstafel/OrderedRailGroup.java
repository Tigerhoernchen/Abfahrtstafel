package de.tiger.abfahrtstafel;

import java.time.LocalTime;
import java.util.List;

public class OrderedRailGroup {

    private final int orderIndex;
    private final String name;
    private final String parentStation;
    private final boolean finalStop;
    private final List<LocalTime> departures;

    public OrderedRailGroup(int orderIndex, String name, String parentStation, boolean finalStop, List<LocalTime> departures) {
        this.orderIndex = orderIndex;
        this.name = name;
        this.parentStation = parentStation;
        this.finalStop = finalStop;
        this.departures = departures;
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
}