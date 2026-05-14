package de.tiger.abfahrtstafel;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.util.List;
import java.util.Map;

public class LayoutRenderer {

    public void render(Graphics2D g,
                       DisplayLayout layout,
                       Map<String, String> placeholders,
                       List<Departure> departures,
                       List<Departure> stationDepartures,
                       int textScroll,
                       int canvasWidth,
                       int canvasHeight) {

        if (layout.getBackground() != null && !layout.getBackground().isEmpty()) {
            g.setColor(parseColor(layout.getBackground()));
            g.fillRect(0, 0, canvasWidth, canvasHeight);
        }

        for (DisplayElement element : layout.getElements()) {
            renderElement(g, element, placeholders, departures, stationDepartures, textScroll, canvasWidth, canvasHeight);
        }

        if (layout.getSections() != null) {
            for (DisplaySection section : layout.getSections()) {
                if (!shouldRender(section.getWhen(), placeholders, departures)) {
                    continue;
                }

                for (DisplayElement element : section.getElements()) {
                    renderElement(g, element, placeholders, departures, stationDepartures, textScroll, canvasWidth, canvasHeight);
                }
            }
        }
    }

    private void renderElement(Graphics2D g,
                               DisplayElement element,
                               Map<String, String> placeholders,
                               List<Departure> departures,
                               List<Departure> stationDepartures,
                               int textScroll,
                               int canvasWidth,
                               int canvasHeight) {

        if (!shouldRender(element.getShowWhen(), placeholders, departures)) {
            return;
        }

        if ("text".equalsIgnoreCase(element.getType()) || "warning".equalsIgnoreCase(element.getType())) {
            drawText(g, element, placeholders, textScroll, canvasWidth);
        }

        if ("separator".equalsIgnoreCase(element.getType())) {
            drawSeparator(g, element, canvasWidth);
        }

        if ("rectangle".equalsIgnoreCase(element.getType())) {
            drawRectangle(g, element, canvasWidth, canvasHeight);
        }

        if ("list".equalsIgnoreCase(element.getType())) {
            List<Departure> listData = departures;

            if ("stationDepartures".equalsIgnoreCase(element.getSource())) {
                listData = stationDepartures;
            }

            drawDepartureList(g, element, placeholders, listData, textScroll, canvasWidth, canvasHeight);
        }
    }

    private boolean shouldRender(String condition,
                                 Map<String, String> placeholders,
                                 List<Departure> departures) {

        if (condition == null || condition.isBlank() || "always".equalsIgnoreCase(condition) || "default".equalsIgnoreCase(condition)) {
            return true;
        }

        String[] conditions = condition.split(",");

        for (String singleCondition : conditions) {
            if (!shouldRenderSingle(singleCondition.trim(), placeholders, departures)) {
                return false;
            }
        }

        return true;
    }

    private boolean shouldRenderSingle(String condition,
                                       Map<String, String> placeholders,
                                       List<Departure> departures) {

        boolean hasDeparture = departures != null && !departures.isEmpty();
        boolean hasDelay = hasDeparture && departures.get(0).getDelayMinutes() > 0;
        boolean hasWarnings = !placeholders.getOrDefault("warnings", "").trim().isEmpty();
        boolean hasVia = !placeholders.getOrDefault("via", "").trim().isEmpty();

        boolean onArrival = "true".equalsIgnoreCase(
                placeholders.getOrDefault("onArrival", "false")
        );

        return switch (condition.toLowerCase()) {
            case "has_departure" -> hasDeparture;
            case "no_departure" -> !hasDeparture;
            case "has_delay" -> hasDelay;
            case "no_delay" -> !hasDelay;
            case "has_warnings" -> hasWarnings;
            case "no_warnings" -> !hasWarnings;
            case "has_via" -> hasVia;
            case "no_via" -> !hasVia;
            case "on_arrival" -> onArrival;
            default -> true;
        };
    }

    private void drawText(Graphics2D g,
                          DisplayElement element,
                          Map<String, String> placeholders,
                          int textScroll,
                          int canvasWidth) {

        String text = replacePlaceholders(element.getValue(), placeholders);

        if (text == null || text.isEmpty()) {
            return;
        }

        Font font = createFont(element.getFont(), element.getFontStyle(), element.getFontSize());
        g.setFont(font);
        g.setColor(parseColor(element.getColor()));

        int x = element.getX();
        int y = element.getY();

        int width = "fill".equalsIgnoreCase(element.getWidth())
                ? canvasWidth - x
                : parseIntSafe(element.getWidth(), 50);

        drawTextInBox(
                g,
                text,
                x,
                y,
                width,
                element.getFontSize(),
                element.getAlign(),
                element.getScroll(),
                textScroll,
                element.getBackground(),
                element.getColor(),
                element.getScrollSeparator(),
                element.getPadding()
        );
    }

    private void drawDepartureList(Graphics2D g,
                                   DisplayElement element,
                                   Map<String, String> basePlaceholders,
                                   List<Departure> departures,
                                   int textScroll,
                                   int canvasWidth,
                                   int canvasHeight) {

        if (departures == null || departures.isEmpty()) {
            return;
        }

        int startX = element.getX();
        int startY = element.getY();
        int rowHeight = element.getRowHeight() <= 0 ? 20 : element.getRowHeight();
        int maxRows = element.getMaxRows() <= 0 ? departures.size() : element.getMaxRows();

        int availableHeight = element.getHeight() > 0
                ? element.getHeight()
                : canvasHeight - startY;

        int rowsByHeight = availableHeight / rowHeight;
        int rows = Math.min(Math.min(maxRows, rowsByHeight), departures.size());

        for (int i = 0; i < rows; i++) {
            Departure departure = departures.get(i);
            int rowY = startY + (i * rowHeight);

            Map<String, String> rowPlaceholders = new java.util.HashMap<>(basePlaceholders);

            rowPlaceholders.put("line", departure.getLine());
            rowPlaceholders.put("time", departure.getTime());
            rowPlaceholders.put("expected", getExpectedTime(departure));
            rowPlaceholders.put("delay", getDelayText(departure));
            rowPlaceholders.put("delayMinutes", String.valueOf(departure.getDelayMinutes()));
            rowPlaceholders.put("destination", departure.getDestination());
            rowPlaceholders.put("via", departure.getVia());
            rowPlaceholders.put("track", departure.getPlatform());

            List<WarnMessage> rowWarnings = AbfahrtstafelPlugin
                    .getInstance()
                    .getWarningManager()
                    .getActiveWarnings(
                            basePlaceholders.getOrDefault("station", ""),
                            departure.getPlatform(),
                            departure.getLine()
                    );

            rowPlaceholders.put("warnings", buildWarningText(rowWarnings));

            if (element.getColumns() == null || element.getColumns().isEmpty()) {
                drawDefaultListRow(g, rowPlaceholders, startX, rowY, textScroll, canvasWidth);
                continue;
            }

            for (DisplayColumn column : element.getColumns()) {
                drawColumn(g, column, rowPlaceholders, startX, rowY, textScroll, canvasWidth);
            }
        }
    }

    private void drawColumn(Graphics2D g,
                            DisplayColumn column,
                            Map<String, String> placeholders,
                            int baseX,
                            int rowY,
                            int textScroll,
                            int canvasWidth) {

        String text = replacePlaceholders(column.getValue(), placeholders);

        if (text == null || text.isEmpty()) {
            return;
        }

        Font font = createFont(column.getFont(), column.getFontStyle(), column.getFontSize());
        g.setFont(font);
        g.setColor(parseColor(column.getColor()));

        int x = baseX + column.getX();

        int width = "fill".equalsIgnoreCase(column.getWidth())
                ? canvasWidth - x
                : parseIntSafe(column.getWidth(), 50);

        drawTextInBox(
                g,
                text,
                x,
                rowY,
                width,
                column.getFontSize(),
                column.getAlign(),
                column.getScroll(),
                textScroll,
                null,
                column.getColor(),
                column.getScrollSeparator(),
                "0"
        );
    }

    private void drawDefaultListRow(Graphics2D g,
                                    Map<String, String> placeholders,
                                    int x,
                                    int y,
                                    int textScroll,
                                    int canvasWidth) {

        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.setColor(Color.WHITE);

        String separator = "  ***  ";

        drawTextInBox(g, placeholders.getOrDefault("time", ""), x + 8, y, 50, 13, "left", "none", textScroll, null, "#FFFFFF", separator, "0");
        drawTextInBox(g, placeholders.getOrDefault("expected", ""), x + 65, y, 55, 13, "left", "none", textScroll, null, "#FFFFFF", separator, "0");
        drawTextInBox(g, placeholders.getOrDefault("line", ""), x + 128, y, 40, 13, "left", "none", textScroll, null, "#FFFFFF", separator, "0");
        drawTextInBox(g, placeholders.getOrDefault("via", ""), x + 172, y, 120, 13, "left", "continuous", textScroll, null, "#FFFFFF", separator, "0");
        drawTextInBox(g, placeholders.getOrDefault("destination", ""), Math.max(300, canvasWidth - 210), y, 110, 13, "left", "pingpong", textScroll, null, "#FFFFFF", separator, "0");
        drawTextInBox(g, placeholders.getOrDefault("track", ""), canvasWidth - 88, y, 40, 13, "right", "none", textScroll, null, "#FFFFFF", separator, "0");
    }

    private void drawTextInBox(Graphics2D g,
                               String text,
                               int x,
                               int y,
                               int width,
                               int fontSize,
                               String align,
                               String scrollMode,
                               int textScroll,
                               String background,
                               String textColor,
                               String scrollSeparator,
                               String padding) {

        if (text == null || text.isEmpty() || width <= 0) {
            return;
        }

        int[] p = parsePadding(padding);

        int padTop = p[0];
        int padRight = p[1];
        int padBottom = p[2];
        int padLeft = p[3];

        int innerX = x + padLeft;
        int innerWidth = width - padLeft - padRight;

        if (innerWidth <= 0) {
            return;
        }

        FontMetrics metrics = g.getFontMetrics();
        int textWidth = metrics.stringWidth(text);

        if (background != null && !background.isEmpty()) {
            g.setColor(parseColor(background));
            g.fillRect(
                    x,
                    y - metrics.getAscent() - padTop,
                    width,
                    metrics.getHeight() + padTop + padBottom
            );

            g.setColor(parseColor(textColor));
        }

        if (textWidth <= innerWidth) {
            int drawX = innerX;

            if ("center".equalsIgnoreCase(align)) {
                drawX = innerX + (innerWidth - textWidth) / 2;
            } else if ("right".equalsIgnoreCase(align)) {
                drawX = innerX + innerWidth - textWidth;
            }

            g.drawString(text, drawX, y);
            return;
        }

        Shape oldClip = g.getClip();
        g.setClip(innerX, y - fontSize, innerWidth, fontSize + 8);

        if (scrollMode == null) {
            scrollMode = "none";
        }

        if ("continuous".equalsIgnoreCase(scrollMode)) {
            if (scrollSeparator == null || scrollSeparator.isEmpty()) {
                scrollSeparator = "  ***  ";
            }

            String repeatedText = text + scrollSeparator;
            int repeatedWidth = metrics.stringWidth(repeatedText);

            if (repeatedWidth <= 0) {
                g.setClip(oldClip);
                return;
            }

            int offset = textScroll % repeatedWidth;
            int startX = innerX - offset;

            for (int drawX = startX; drawX < innerX + innerWidth; drawX += repeatedWidth) {
                g.drawString(repeatedText, drawX, y);
            }

            g.setClip(oldClip);
            return;
        }

        if ("pingpong".equalsIgnoreCase(scrollMode)) {
            int overflow = textWidth - innerWidth;

            int forwardTicks = overflow;
            int pauseEndTicks = 30;
            int backwardTicks = Math.max(1, overflow / 3);
            int pauseStartTicks = 20;

            int cycleLength = forwardTicks + pauseEndTicks + backwardTicks + pauseStartTicks;
            int t = textScroll % cycleLength;

            int offset;

            if (t < forwardTicks) {
                offset = t;
            } else if (t < forwardTicks + pauseEndTicks) {
                offset = overflow;
            } else if (t < forwardTicks + pauseEndTicks + backwardTicks) {
                int backT = t - forwardTicks - pauseEndTicks;
                double progress = (double) backT / (double) backwardTicks;
                offset = overflow - (int) Math.round(progress * overflow);
            } else {
                offset = 0;
            }

            g.drawString(text, innerX - offset, y);
            g.setClip(oldClip);
            return;
        }

        String shortened = text;

        while (shortened.length() > 3
                && metrics.stringWidth(shortened + "...") > innerWidth) {
            shortened = shortened.substring(0, shortened.length() - 1);
        }

        g.drawString(shortened + "...", innerX, y);
        g.setClip(oldClip);
    }

    private void drawSeparator(Graphics2D g,
                               DisplayElement element,
                               int canvasWidth) {

        int x = element.getX();
        int y = element.getY();

        int width = "fill".equalsIgnoreCase(element.getWidth())
                ? canvasWidth - x
                : parseIntSafe(element.getWidth(), 50);

        g.setColor(parseColor(element.getColor()));
        g.fillRect(x, y, width, Math.max(1, element.getThickness()));
    }

    private void drawRectangle(Graphics2D g,
                               DisplayElement element,
                               int canvasWidth,
                               int canvasHeight) {

        int x = element.getX();
        int y = element.getY();

        int width = "fill".equalsIgnoreCase(element.getWidth())
                ? canvasWidth - x
                : parseIntSafe(element.getWidth(), 0);

        int height = element.getHeight() <= 0
                ? canvasHeight - y
                : element.getHeight();

        if (width <= 0 || height <= 0) {
            return;
        }

        g.setColor(parseColor(element.getColor()));
        g.fillRect(x, y, width, height);
    }

    private String getExpectedTime(Departure departure) {
        if (departure.getDelayMinutes() <= 0) {
            return "";
        }

        return java.time.LocalTime.parse(departure.getTime())
                .plusMinutes(departure.getDelayMinutes())
                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
    }

    private String getDelayText(Departure departure) {
        if (departure.getDelayMinutes() <= 0) {
            return "";
        }

        return "+" + departure.getDelayMinutes();
    }

    private String replacePlaceholders(String text,
                                       Map<String, String> placeholders) {
        String result = text;

        while (true) {
            int start = result.indexOf("{?");

            if (start == -1) {
                break;
            }

            int colon = result.indexOf(':', start);
            if (colon == -1) {
                break;
            }

            int depth = 0;
            int end = -1;

            for (int i = start; i < result.length(); i++) {
                char c = result.charAt(i);

                if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;

                    if (depth == 0) {
                        end = i;
                        break;
                    }
                }
            }

            if (end == -1) {
                break;
            }

            String key = result.substring(start + 2, colon);
            String content = result.substring(colon + 1, end);

            String value = placeholders.getOrDefault(key, "").trim();

            String replacement = value.isEmpty() ? "" : content;

            result = result.substring(0, start)
                    + replacement
                    + result.substring(end + 1);
        }

        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace(
                    "{" + entry.getKey() + "}",
                    entry.getValue() == null ? "" : entry.getValue()
            );
        }

        return result.trim();
    }

    private Color parseColor(String colorString) {
        try {
            return Color.decode(colorString);
        } catch (Exception e) {
            return Color.WHITE;
        }
    }

    private int parseIntSafe(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return fallback;
        }
    }

    private String buildWarningText(List<WarnMessage> warnings) {
        if (warnings == null || warnings.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < warnings.size(); i++) {
            if (i > 0) {
                builder.append(" *** ");
            }

            builder.append(warnings.get(i).getMessage());
        }

        return builder.toString();
    }

    private Font createFont(String fontName, String fontStyle, int fontSize) {
        int style = Font.PLAIN;

        if ("bold".equalsIgnoreCase(fontStyle)) {
            style = Font.BOLD;
        } else if ("italic".equalsIgnoreCase(fontStyle)) {
            style = Font.ITALIC;
        } else if ("bold_italic".equalsIgnoreCase(fontStyle)
                || "boldItalic".equalsIgnoreCase(fontStyle)) {
            style = Font.BOLD | Font.ITALIC;
        }

        if (fontName == null || fontName.isBlank()) {
            fontName = "SansSerif";
        }

        return new Font(fontName, style, fontSize);
    }

    private int[] parsePadding(String padding) {
        int[] result = new int[]{0, 0, 0, 0};

        if (padding == null || padding.isBlank()) {
            return result;
        }

        String[] parts = padding.trim().split("\\s+");

        try {
            if (parts.length == 1) {
                int all = Integer.parseInt(parts[0]);
                result[0] = all;
                result[1] = all;
                result[2] = all;
                result[3] = all;
            } else if (parts.length == 2) {
                int vertical = Integer.parseInt(parts[0]);
                int horizontal = Integer.parseInt(parts[1]);

                result[0] = vertical;
                result[1] = horizontal;
                result[2] = vertical;
                result[3] = horizontal;
            } else if (parts.length == 3) {
                int top = Integer.parseInt(parts[0]);
                int horizontal = Integer.parseInt(parts[1]);
                int bottom = Integer.parseInt(parts[2]);

                result[0] = top;
                result[1] = horizontal;
                result[2] = bottom;
                result[3] = horizontal;
            } else if (parts.length >= 4) {
                result[0] = Integer.parseInt(parts[0]);
                result[1] = Integer.parseInt(parts[1]);
                result[2] = Integer.parseInt(parts[2]);
                result[3] = Integer.parseInt(parts[3]);
            }
        } catch (NumberFormatException ignored) {
        }

        return result;
    }
}