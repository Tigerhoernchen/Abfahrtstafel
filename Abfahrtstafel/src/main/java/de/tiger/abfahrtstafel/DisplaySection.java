package de.tiger.abfahrtstafel;

import java.util.List;

public class DisplaySection {

    private final String when;
    private final List<DisplayElement> elements;

    public DisplaySection(String when, List<DisplayElement> elements) {
        this.when = when;
        this.elements = elements;
    }

    public String getWhen() {
        return when;
    }

    public List<DisplayElement> getElements() {
        return elements;
    }
}