package de.tiger.abfahrtstafel;

public class DisplayColumn {

    private final String value;
    private final int x;
    private final String width;
    private final String align;
    private final String color;
    private final int fontSize;
    private final String scroll;

    public DisplayColumn(String value, int x, String width, String align,
                         String color, int fontSize, String scroll) {
        this.value = value;
        this.x = x;
        this.width = width;
        this.align = align;
        this.color = color;
        this.fontSize = fontSize;
        this.scroll = scroll;
    }

    public String getValue() {
        return value;
    }

    public int getX() {
        return x;
    }

    public String getWidth() {
        return width;
    }

    public String getAlign() {
        return align;
    }

    public String getColor() {
        return color;
    }

    public int getFontSize() {
        return fontSize;
    }

    public String getScroll() {
        return scroll;
    }
}