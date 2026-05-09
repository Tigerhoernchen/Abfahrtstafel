package de.tiger.abfahrtstafel;

import java.util.List;

public class TrainLine {

    private final String name;
    private final String description;
    private final List<OrderedRailGroup> orderedRailGroups;

    public TrainLine(String name, String description, List<OrderedRailGroup> orderedRailGroups) {
        this.name = name;
        this.description = description;
        this.orderedRailGroups = orderedRailGroups;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<OrderedRailGroup> getOrderedRailGroups() {
        return orderedRailGroups;
    }
}