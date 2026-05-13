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

        boolean hasDeparture = departures != null && !departures.isEmpty();

        boolean hasDelay = hasDeparture
                && departures.get(0).getDelayMinutes() > 0;

        boolean hasWarnings = placeholders.getOrDefault("warnings", "").trim().length() > 0;

        boolean hasVia = placeholders.getOrDefault("via", "").trim().length() > 0;

        // Klassische Elemente (alte Struktur)
        for (DisplayElement element : layout.getElements()) {
            renderElement(
                    g,
                    element,
                    placeholders,
                    departures,
                    stationDepartures,
                    textScroll,
                    canvasWidth,
                    canvasHeight
            );
        }

        // Neue Sections
        if (layout.getSections() != null) {
            for (DisplaySection section : layout.getSections()) {

                if (!shouldRender(section.getWhen(), placeholders, departures)) {
                    continue;
                }

                for (DisplayElement element : section.getElements()) {
                    renderElement(
                            g,
                            element,
                            placeholders,
                            departures,
                            stationDepartures,
                            textScroll,
                            canvasWidth,
                            canvasHeight
                    );
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

        if ("text".equalsIgnoreCase(element.getType())
                || "warning".equalsIgnoreCase(element.getType())) {
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

            drawDepartureList(
                    g,
                    element,
                    placeholders,
                    listData,
                    textScroll,
                    canvasWidth,
                    canvasHeight
            );
        }
    }

    private boolean shouldRender(String condition,
                                 Map<String, String> placeholders,
                                 List<Departure> departures) {

        if (condition == null || condition.isBlank() || "always".equalsIgnoreCase(condition)) {
            return true;
        }

        boolean hasDeparture = departures != null && !departures.isEmpty();

        boolean hasDelay = hasDeparture
                && departures.get(0).getDelayMinutes() > 0;

        boolean hasWarnings = placeholders
                .getOrDefault("warnings", "")
                .trim()
                .length() > 0;

        boolean hasVia = placeholders
                .getOrDefault("via", "")
                .trim()
                .length() > 0;

        return switch (condition.toLowerCase()) {
            case "has_departure" -> hasDeparture;
            case "no_departure" -> !hasDeparture;
            case "has_delay" -> hasDelay;
            case "has_warnings" -> hasWarnings;
            case "has_via" -> hasVia;
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

        Font font = new Font("SansSerif", Font.PLAIN, element.getFontSize());
        g.setFont(font);
        g.setColor(parseColor(element.getColor()));

        int x = element.getX();
        int y = element.getY();

        int width;

        if ("fill".equalsIgnoreCase(element.getWidth())) {
            width = canvasWidth - x;
        } else {
            width = parseIntSafe(element.getWidth(), 50);
        }

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
                element.getColor()
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
        int rowHeight = element.getRowHeight();
        int maxRows = element.getMaxRows();

        if (rowHeight <= 0) {
            rowHeight = 20;
        }

        if (maxRows <= 0) {
            maxRows = departures.size();
        }

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

        Font font = new Font("SansSerif", Font.PLAIN, column.getFontSize());
        g.setFont(font);
        g.setColor(parseColor(column.getColor()));

        int x = baseX + column.getX();

        int width;

        if ("fill".equalsIgnoreCase(column.getWidth())) {
            width = canvasWidth - x;
        } else {
            width = parseIntSafe(column.getWidth(), 50);
        }

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
                column.getColor()
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

        drawTextInBox(g, placeholders.getOrDefault("time", ""), x + 8, y, 50, 13, "left", "none", textScroll, null, "#FFFFFF");
        drawTextInBox(g, placeholders.getOrDefault("expected", ""), x + 65, y, 55, 13, "left", "none", textScroll, null, "#FFFFFF");
        drawTextInBox(g, placeholders.getOrDefault("line", ""), x + 128, y, 40, 13, "left", "none", textScroll, null, "#FFFFFF");
        drawTextInBox(g, placeholders.getOrDefault("via", ""), x + 172, y, 120, 13, "left", "continuous", textScroll, null, "#FFFFFF");
        drawTextInBox(g, placeholders.getOrDefault("destination", ""), Math.max(300, canvasWidth - 210), y, 110, 13, "left", "pingpong", textScroll, null, "#FFFFFF");
        drawTextInBox(g, placeholders.getOrDefault("track", ""), canvasWidth - 88, y, 40, 13, "right", "none", textScroll, null, "#FFFFFF");
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
                               String textColor) {

        if (text == null || text.isEmpty() || width <= 0) {
            return;
        }

        FontMetrics metrics = g.getFontMetrics();
        int textWidth = metrics.stringWidth(text);
        if (background != null && !background.isEmpty()) {
            g.setColor(parseColor(background));
            g.fillRect(
                    x,
                    y - metrics.getAscent() - 2,
                    width,
                    metrics.getHeight() + 4
            );

            g.setColor(parseColor(textColor));
        }

        if (textWidth <= width) {
            int drawX = x;

            if ("center".equalsIgnoreCase(align)) {
                drawX = x + (width - textWidth) / 2;
            } else if ("right".equalsIgnoreCase(align)) {
                drawX = x + width - textWidth;
            }

            g.drawString(text, drawX, y);
            return;
        }

        Shape oldClip = g.getClip();
        g.setClip(x, y - fontSize, width, fontSize + 8);

        if (scrollMode == null) {
            scrollMode = "none";
        }

        if ("continuous".equalsIgnoreCase(scrollMode)) {
            String repeatedText = text + "   ***   ";
            int repeatedWidth = metrics.stringWidth(repeatedText);

            if (repeatedWidth <= 0) {
                g.setClip(oldClip);
                return;
            }

            int offset = textScroll % repeatedWidth;
            int startX = x - offset;

            for (int drawX = startX; drawX < x + width; drawX += repeatedWidth) {
                g.drawString(repeatedText, drawX, y);
            }

            g.setClip(oldClip);
            return;
        }

        if ("pingpong".equalsIgnoreCase(scrollMode)) {
            int overflow = textWidth - width;

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

            g.drawString(text, x - offset, y);

            g.setClip(oldClip);
            return;
        }

        String shortened = text;

        while (shortened.length() > 3
                && metrics.stringWidth(shortened + "...") > width) {
            shortened = shortened.substring(0, shortened.length() - 1);
        }

        g.drawString(shortened + "...", x, y);
        g.setClip(oldClip);
    }

    private void drawSeparator(Graphics2D g,
                               DisplayElement element,
                               int canvasWidth) {

        int x = element.getX();
        int y = element.getY();

        int width;

        if ("fill".equalsIgnoreCase(element.getWidth())) {
            width = canvasWidth - x;
        } else {
            width = parseIntSafe(element.getWidth(), 50);
        }

        g.setColor(parseColor(element.getColor()));
        g.fillRect(x, y, width, Math.max(1, element.getThickness()));
    }

    private void drawRectangle(Graphics2D g,
                               DisplayElement element,
                               int canvasWidth,
                               int canvasHeight) {

        int x = element.getX();
        int y = element.getY();

        int width;
        if ("fill".equalsIgnoreCase(element.getWidth())) {
            width = canvasWidth - x;
        } else {
            width = parseIntSafe(element.getWidth(), 0);
        }

        int height;
        if (element.getHeight() <= 0) {
            height = canvasHeight - y;
        } else {
            height = element.getHeight();
        }

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

        // Bedingte Blöcke: {?key:...}
        while (true) {
            int start = result.indexOf("{?");

            if (start == -1) {
                break;
            }

            int colon = result.indexOf(':', start);
            if (colon == -1) {
                break;
            }

            // Passende schließende Klammer finden (verschachtelte {...} erlauben)
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

            String replacement;
            if (value.isEmpty()) {
                replacement = "";
            } else {
                replacement = content;
            }

            result = result.substring(0, start)
                    + replacement
                    + result.substring(end + 1);
        }

        // Normale Platzhalter ersetzen
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
}