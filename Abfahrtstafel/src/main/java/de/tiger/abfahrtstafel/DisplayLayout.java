package de.tiger.abfahrtstafel;

import java.util.List;

public class DisplayLayout {
    private final String name;
    private final String displayType;
    private final int widthBlocks;
    private final int heightBlocks;
    private final String background;
    private final List<DisplayElement> elements;
    private final List<DisplaySection> sections;

    public DisplayLayout(String name,
                         String displayType,
                         int widthBlocks,
                         int heightBlocks,
                         String background,
                         List<DisplayElement> elements,
                         List<DisplaySection> sections) {
        this.name = name;
        this.displayType = displayType;
        this.widthBlocks = widthBlocks;
        this.heightBlocks = heightBlocks;
        this.background = background;
        this.elements = elements;
        this.sections = sections;
    }

    public String getName() {
        return name;
    }

    public String getDisplayType() {
        return displayType;
    }

    public int getWidthBlocks() {
        return widthBlocks;
    }

    public int getHeightBlocks() {
        return heightBlocks;
    }

    public String getBackground() {
        return background;
    }

    public List<DisplayElement> getElements() {
        return elements;
    }

    public List<DisplaySection> getSections() {
        return sections;
    }
}