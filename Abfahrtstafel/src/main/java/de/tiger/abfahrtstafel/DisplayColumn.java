package de.tiger.abfahrtstafel;

import java.util.List;

public class DisplayColumn {

    private final String value;
    private final int x;
    private final String width;
    private final String align;
    private final String color;
    private final int fontSize;
    private final String scroll;
    private final String font;
    private final String fontStyle;
    private final String scrollSeparator;
    private final String background;
    private final String padding;
    private final String showWhen;
    private final boolean blink;
    private final int blinkTicks;
    private final String header;
    private final List<DisplayColumn> variants;

    public DisplayColumn(String value,
                         int x,
                         String width,
                         String align,
                         String color,
                         int fontSize,
                         String scroll,
                         String font,
                         String fontStyle,
                         String scrollSeparator,
                         String background,
                         String padding,
                         String showWhen,
                         boolean blink,
                         int blinkTicks,
                         String header,
                         List<DisplayColumn> variants) {
        this.value = value;
        this.x = x;
        this.width = width;
        this.align = align;
        this.color = color;
        this.fontSize = fontSize;
        this.scroll = scroll;
        this.font = font;
        this.fontStyle = fontStyle;
        this.scrollSeparator = scrollSeparator;
        this.background = background;
        this.padding = padding;
        this.showWhen = showWhen;
        this.blink = blink;
        this.blinkTicks = blinkTicks;
        this.header = header;
        this.variants = variants;
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

    public String getFont() {
        return font;
    }

    public String getFontStyle() {
        return fontStyle;
    }

    public String getScrollSeparator() {
        return scrollSeparator;
    }

    public String getBackground() {
        return background;
    }

    public String getPadding() {
        return padding;
    }

    public String getShowWhen() {
        return showWhen;
    }

    public boolean isBlink() {
        return blink;
    }

    public int getBlinkTicks() {
        return blinkTicks;
    }

    public String getHeader() {
        return header;
    }

    public List<DisplayColumn> getVariants() {
        return variants;
    }
}