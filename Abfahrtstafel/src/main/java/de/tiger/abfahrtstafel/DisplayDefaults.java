package de.tiger.abfahrtstafel;

public class DisplayDefaults {

    private final String font;
    private final int fontSize;
    private final String color;
    private final String scroll;
    private final String fontStyle;
    private final String padding;
    private final String scrollSeparator;

    public DisplayDefaults(String font,
                           int fontSize,
                           String color,
                           String scroll,
                           String fontStyle,
                           String padding,
                           String scrollSeparator) {
        this.font = font;
        this.fontSize = fontSize;
        this.color = color;
        this.scroll = scroll;
        this.fontStyle = fontStyle;
        this.padding = padding;
        this.scrollSeparator = scrollSeparator;
    }

    public String getFont() {
        return font;
    }

    public int getFontSize() {
        return fontSize;
    }

    public String getColor() {
        return color;
    }

    public String getScroll() {
        return scroll;
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

    public static DisplayDefaults defaultValues() {
        return new DisplayDefaults(
                "SansSerif",
                14,
                "#FFFFFF",
                "none",
                "plain",
                "0",
                "   ***   "
        );
    }
}