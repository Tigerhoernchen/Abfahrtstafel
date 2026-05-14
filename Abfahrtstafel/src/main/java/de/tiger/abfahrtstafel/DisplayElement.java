package de.tiger.abfahrtstafel;

import java.util.List;

public class DisplayElement {

    private final String type;
    private final String value;
    private final int x;
    private final int y;
    private final String width;
    private final int height;
    private final String align;
    private final String scroll;
    private final int fontSize;
    private final String color;
    private final String background;
    private final int thickness;
    private final int rowHeight;
    private final int maxRows;
    private final List<DisplayColumn> columns;
    private String showWhen;
    private String source;
    private final String font;
    private final String fontStyle;
    private final String padding;
    private final String scrollSeparator;



    public DisplayElement(String type, String value, int x, int y, String width,
                          int height, String align, String scroll, int fontSize,
                          String color, String background, int thickness,
                          int rowHeight, int maxRows,
                          List<DisplayColumn> columns,String showWhen,
                          String source, String font, String fontStyle, String padding, String scrollSeparator) {
        this.type = type;
        this.value = value;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.align = align;
        this.scroll = scroll;
        this.fontSize = fontSize;
        this.color = color;
        this.background = background;
        this.thickness = thickness;
        this.rowHeight = rowHeight;
        this.maxRows = maxRows;
        this.columns = columns;
        this.showWhen = showWhen;
        this.source = source;
        this.font = font;
        this.fontStyle = fontStyle;
        this.padding = padding;
        this.scrollSeparator = scrollSeparator;
    }

    public String getType() { return type; }
    public String getValue() { return value; }
    public int getX() { return x; }
    public int getY() { return y; }
    public String getWidth() { return width; }
    public int getHeight() { return height; }
    public String getAlign() { return align; }
    public String getScroll() { return scroll; }
    public int getFontSize() { return fontSize; }
    public String getColor() { return color; }
    public String getBackground() { return background; }
    public int getThickness() { return thickness; }
    public int getRowHeight() {
        return rowHeight;
    }

    public int getMaxRows() {
        return maxRows;
    }

    public List<DisplayColumn> getColumns() {
        return columns;
    }

    public String getShowWhen() {
        return showWhen;
    }

    public String getSource() {
        return source;
    }

    public String getFont() {
        return font;
    }

    public String getFontStyle() {
        return fontStyle;
    }

    public String getPadding() {
        return padding;
    }

    public String getScrollSeparator() {
        return scrollSeparator;
    }
}