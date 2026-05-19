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
    private final String description;
    private final DisplayDefaults defaults;
    private final boolean invisibleFrames;

    public DisplayLayout(String name,
                         String displayType,
                         int widthBlocks,
                         int heightBlocks,
                         String background,
                         String description,
                         DisplayDefaults defaults,
                         List<DisplayElement> elements,
                         List<DisplaySection> sections,
                         boolean invisibleFrames) {
        this.name = name;
        this.displayType = displayType;
        this.widthBlocks = widthBlocks;
        this.heightBlocks = heightBlocks;
        this.background = background;
        this.description = description;
        this.defaults = defaults;
        this.elements = elements;
        this.sections = sections;
        this.invisibleFrames = invisibleFrames;
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

    public String getDescription() {
        return description;
    }

    public DisplayDefaults getDefaults() {
        return defaults;
    }

    public boolean isInvisibleFrames() {
        return invisibleFrames;
    }
}