package de.tiger.abfahrtstafel;

import java.util.List;

public class WarnMessage {

    private final int id;
    private final String message;
    private final boolean active;
    private final List<String> groups;

    public WarnMessage(int id, String message, boolean active, List<String> groups) {
        this.id = id;
        this.message = message;
        this.active = active;
        this.groups = groups;
    }

    public int getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }

    public boolean isActive() {
        return active;
    }

    public List<String> getGroups() {
        return groups;
    }
}