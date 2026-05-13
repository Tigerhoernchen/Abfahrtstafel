package de.tiger.abfahrtstafel;

import com.bergerkiller.bukkit.common.map.MapColorPalette;
import com.bergerkiller.bukkit.common.map.MapDisplay;
import com.bergerkiller.bukkit.common.map.MapFont;
import com.bergerkiller.bukkit.common.map.MapTexture;
import java.awt.Shape;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.List;

public class DepartureDisplay extends MapDisplay {

    private static final byte BLUE = MapColorPalette.COLOR_BLUE;
    private static final byte WHITE = MapColorPalette.COLOR_WHITE;
    private static final byte BLACK = MapColorPalette.COLOR_BLACK;
    private static final byte RED = MapColorPalette.getColor(200, 30, 30);
    private static final byte DARK_BLUE = MapColorPalette.getColor(0, 25, 120);
    private static final byte WARNING = MapColorPalette.getColor(255, 180, 0);

    private int ticks = 0;
    private int textScroll = 0;

    @Override
    public void onAttached() {
        setGlobal(true);
        drawDisplay();
    }

    @Override
    public void onTick() {
        ticks++;

        int scrollSpeed = AbfahrtstafelPlugin
                .getInstance()
                .getTextScrollSpeedTicks();

        if (scrollSpeed < 1) {
            scrollSpeed = 1;
        }

        if (ticks % scrollSpeed == 0) {
            textScroll++;
        }

        int updateTicks = AbfahrtstafelPlugin.getInstance().getDisplayUpdateTicks();

        if (updateTicks < 1) {
            updateTicks = 10;
        }

        if (ticks >= updateTicks) {
            ticks = 0;
            drawDisplay();
        }
    }

    private void drawDisplay() {
        int width = getWidth();
        int height = getHeight();

        String displayType = properties.get("displayType", "station");
        String station = properties.get("station", "Start");
        String railGroup = properties.get("railGroup", "G1");

        List<WarnMessage> warnings = AbfahrtstafelPlugin
                .getInstance()
                .getWarningManager()
                .getActiveWarnings(station, railGroup);

        getLayer().fill(BLUE);

        if (displayType.equals("platform")) {
            List<Departure> departures = AbfahrtstafelPlugin
                    .getInstance()
                    .getScheduleManager()
                    .getNextDepartures(station, railGroup, 1);

            List<Departure> stationDepartures = AbfahrtstafelPlugin
                    .getInstance()
                    .getScheduleManager()
                    .getNextDeparturesForStation(station, 3);

            drawPlatformDisplay(width, height, station, railGroup, departures, stationDepartures, warnings);
        } else {
            List<Departure> departures = AbfahrtstafelPlugin
                    .getInstance()
                    .getScheduleManager()
                    .getNextDeparturesForStation(
                            station,
                            AbfahrtstafelPlugin.getInstance().getStationDisplayMaxEntries()
                    );

            drawStationDisplay(width, height, station, departures, warnings);
        }
    }

    private void drawStationDisplay(int width, int height, String station,
                                    List<Departure> departures,
                                    List<WarnMessage> warnings) {

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        g.setColor(new Color(43, 45, 141));
        g.fillRect(0, 0, width, height);

        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        String currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));

        // Kopfbereich
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 18));
        g.drawString(currentTime, 8, 22);

        g.setFont(new Font("Arial", Font.PLAIN, 18));
        g.drawString("Abfahrten von " + station, 95, 22);

        // Dynamische Spaltenpositionen
        int xPlanned = 8;
        int xExpected = 65;
        int xLine = 128;
        int xVia = 172;
        int xDestination = Math.max(300, width - 210);
        int xPlatform = width - 88;
        int xWarning = width - 52;

        int viaWidth = xDestination - xVia - 8;
        int destinationWidth = xPlatform - xDestination - 8;
        int warningWidth = width - xWarning - 8;

        // Spaltenkopf
        int headerY = 48;
        g.setFont(new Font("Arial", Font.BOLD, 12));
        g.setColor(Color.WHITE);

        g.drawString("Geplant", xPlanned, headerY);
        g.drawString("Erwartet", xExpected, headerY);
        g.drawString("Linie", xLine, headerY);
        g.drawString("Über", xVia, headerY);
        g.drawString("Ziel", xDestination, headerY);
        g.drawString("Gleis", xPlatform, headerY);

        g.drawLine(4, 54, width - 4, 54);

        int y = 74;
        int rowHeight = 20;

        g.setFont(new Font("Arial", Font.PLAIN, 13));

        for (Departure departure : departures) {
            if (y > height - 12) {
                break;
            }

            String planned = departure.getTime();
            String expected = "";

            if (departure.getDelayMinutes() > 0) {
                expected = LocalTime.parse(departure.getTime())
                        .plusMinutes(departure.getDelayMinutes())
                        .format(DateTimeFormatter.ofPattern("HH:mm"));
            }

            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.PLAIN, 13));

            g.drawString(planned, xPlanned, y);
            g.drawString(expected, xExpected, y);
            g.drawString(departure.getLine(), xLine, y);

            drawScrollingTextIfNeeded(g, departure.getVia(), xVia, y, viaWidth, 13);
            drawPingPongScrollingText(g, departure.getDestination(), xDestination, y, destinationWidth, 13);

            g.drawString(departure.getPlatform(), xPlatform, y);

            List<WarnMessage> rowWarnings = AbfahrtstafelPlugin
                    .getInstance()
                    .getWarningManager()
                    .getActiveWarnings(station, departure.getPlatform(), departure.getLine());

            String rowWarningText = buildWarningText(rowWarnings);

            if (!rowWarningText.isEmpty() && warningWidth > 12) {
                g.setColor(new Color(255, 200, 0));
                g.setFont(new Font("Arial", Font.PLAIN, 13));
                drawScrollingTextIfNeeded(g, rowWarningText, xWarning, y, warningWidth, 13);
            }

            y += rowHeight;
        }

        if (departures.isEmpty()) {
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.PLAIN, 16));
            g.drawString("Keine Abfahrten gefunden", 8, 80);
        }

        g.dispose();
        getLayer().draw(MapTexture.fromImage(image), 0, 0);
    }

    private String buildWarningText(List<WarnMessage> warnings) {
        if (warnings.isEmpty()) {
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

    private String shortenText(Graphics2D g, String text, int maxWidth) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        if (g.getFontMetrics().stringWidth(text) <= maxWidth) {
            return text;
        }

        String shortened = text;

        while (shortened.length() > 3 &&
                g.getFontMetrics().stringWidth(shortened + "...") > maxWidth) {
            shortened = shortened.substring(0, shortened.length() - 1);
        }

        return shortened + "...";
    }

    private void drawPlatformDisplay(int width, int height,
                                     String station,
                                     String railGroup,
                                     List<Departure> departures,
                                     List<Departure> stationDepartures,
                                     List<WarnMessage> warnings) {

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        g.setColor(new Color(43, 45, 141));
        g.fillRect(0, 0, width, height);

        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int x = 10;

        if (departures.isEmpty()) {
            String currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));

            g.setColor(Color.WHITE);

            g.setFont(new Font("Arial", Font.PLAIN, 18));
            g.drawString("Keine Abfahrt", x, 30);

            int timeWidth = g.getFontMetrics().stringWidth(currentTime);
            g.drawString(currentTime, width - timeWidth - 10, 30);

            g.setFont(new Font("Arial", Font.PLAIN, 16));
            g.drawString(station + "  Gl. " + railGroup, x, 58);
            if (!warnings.isEmpty()) {
                StringBuilder warningBuilder = new StringBuilder();

                for (int i = 0; i < warnings.size(); i++) {
                    if (i > 0) {
                        warningBuilder.append("  ***  ");
                    }

                    warningBuilder.append(warnings.get(i).getMessage());
                }

                // Textbreite mit derselben Schrift berechnen wie beim Zeichnen
                g.setFont(new Font("Arial", Font.PLAIN, 16));

                String stationRailText = station + "  Gl. " + railGroup;
                int stationRailWidth = g.getFontMetrics().stringWidth(stationRailText);

                // Danach kleinere Schrift für Warnung verwenden
                g.setFont(new Font("Arial", Font.PLAIN, 13));

                int warningX = x + stationRailWidth + 10;
                int warningY = 58;
                int availableWidth = width - warningX - 10;

                if (availableWidth > 20) {
                    g.setColor(Color.WHITE);
                    g.fillRect(warningX - 2, 43, availableWidth + 4, 18);

                    g.setColor(new Color(43, 45, 141));

                    drawScrollingTextIfNeeded(
                            g,
                            warningBuilder.toString(),
                            warningX,
                            warningY,
                            availableWidth,
                            13
                    );
                }

                g.setColor(Color.WHITE);
            }

            if (!stationDepartures.isEmpty()) {
                g.drawLine(x, 72, width - 10, 72);

                g.setFont(new Font("Arial", Font.PLAIN, 14));

                int y = 92;

                for (Departure stationDeparture : stationDepartures) {
                    if (y > height - 8) {
                        break;
                    }

                    String timeText = stationDeparture.getTime();
                    String destinationText = stationDeparture.getDestination();
                    String platformText = stationDeparture.getPlatform();

                    // Spalten
                    int xTime = x;
                    int xPlatform;
                    int platformWidth = g.getFontMetrics().stringWidth(platformText);

                    xPlatform = width - platformWidth - 10;

                    // Zeit zeichnen
                    g.drawString(timeText, xTime, y);

                    // Ziel als Lauftext
                    int departureTimeWidth = g.getFontMetrics().stringWidth(timeText);
                    int xDestination = xTime + departureTimeWidth + 8;
                    int destinationWidth = xPlatform - xDestination - 8;

                    if (destinationWidth > 10) {
                        drawPingPongScrollingText(
                                g,
                                destinationText,
                                xDestination,
                                y,
                                destinationWidth,
                                14
                        );
                    }

                    // Gleis rechtsbündig
                    g.drawString(platformText, xPlatform, y);

                    y += 18;
                }
            }

            g.dispose();

            getLayer().draw(MapTexture.fromImage(image), 0, 0);
            return;
        }

        Departure departure = departures.get(0);

        warnings = AbfahrtstafelPlugin
                .getInstance()
                .getWarningManager()
                .getActiveWarnings(
                        station,
                        departure.getPlatform(),
                        departure.getLine()
                );

        // Linie
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 16));
        g.drawString(departure.getLine(), x, 22);

        // Warnung rechts neben Linie
        if (!warnings.isEmpty()) {
            g.setFont(new Font("Arial", Font.PLAIN, 14));

            StringBuilder warningBuilder = new StringBuilder();

            for (int i = 0; i < warnings.size(); i++) {
                if (i > 0) {
                    warningBuilder.append("  ***  ");
                }

                warningBuilder.append(warnings.get(i).getMessage());
            }

            String warningText = warningBuilder.toString();

            int warningX = x + 48;
            int warningY = 22;
            int availableWidth = width - warningX - 8;

            // Fester weißer Hintergrund (bleibt stehen)
            g.setColor(Color.WHITE);
            g.fillRect(
                    warningX - 2,
                    7,
                    availableWidth + 4,
                    18
            );

            // Text in Blau
            g.setColor(new Color(32, 45, 141));

            // Text ggf. als Lauftext innerhalb des festen Kastens
            drawScrollingTextIfNeeded(
                    g,
                    warningText,
                    warningX,
                    warningY,
                    availableWidth,
                    14
            );
        }

        // Abfahrtszeit
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 26));
        g.drawString(departure.getTime(), x, 55);

        int afterTimeX = x + g.getFontMetrics().stringWidth(departure.getTime()) + 8;

        // Verspätungskasten
        if (departure.getDelayMinutes() > 0) {
            String delayText = "+" + departure.getDelayMinutes();

            int delayWidth = g.getFontMetrics().stringWidth(delayText) + 8;

            g.setColor(Color.WHITE);
            g.fillRect(afterTimeX, 32, delayWidth, 26);

            g.setColor(new Color(43, 45, 141));
            g.setFont(new Font("Arial", Font.PLAIN, 24));
            g.drawString(delayText, afterTimeX + 3, 55);
        }

        // Zielbahnhof unter der Zeit (mit Pendel-Lauftext)
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 22));

        int destinationY = 82;
        int destinationWidth = width - 20;

        drawPingPongScrollingText(
                g,
                departure.getDestination(),
                x,
                destinationY,
                destinationWidth,
                22
        );

        // Zwischenhalte unten
        if (!departure.getVia().isEmpty()) {
            g.setFont(new Font("Arial", Font.PLAIN, 14));
            g.setColor(Color.WHITE);

            String viaText = "über " + departure.getVia();
            drawScrollingTextIfNeeded(g, viaText, x, 106, width - 20, 14);
        }

        g.dispose();

        getLayer().draw(MapTexture.fromImage(image), 0, 0);
    }

    private void drawPingPongScrollingText(Graphics2D g,
                                           String text,
                                           int x,
                                           int y,
                                           int availableWidth,
                                           int fontHeight) {
        if (text == null || text.isEmpty()) {
            return;
        }

        int textWidth = g.getFontMetrics().stringWidth(text);

        // Text passt komplett hinein
        if (textWidth <= availableWidth) {
            g.drawString(text, x, y);
            return;
        }

        int overflow = textWidth - availableWidth;

        // Animation:
        // 1. Nach links scrollen
        // 2. Pause am Ende
        // 3. Schneller zurück scrollen
        // 4. Pause am Anfang
        //
        // Geschwindigkeit:
        // - Hinweg: 1 Pixel pro Tick
        // - Rückweg: 3 Pixel pro Tick

        int forwardTicks = overflow;
        int pauseEndTicks = 30;
        int backwardTicks = Math.max(1, overflow / 3);
        int pauseStartTicks = 20;

        int cycleLength =
                forwardTicks +
                        pauseEndTicks +
                        backwardTicks +
                        pauseStartTicks;

        int t = textScroll % cycleLength;

        int offset;

        // Phase 1: Langsam nach links
        if (t < forwardTicks) {
            offset = t;
        }
        // Phase 2: Pause am Ende
        else if (t < forwardTicks + pauseEndTicks) {
            offset = overflow;
        }
        // Phase 3: Schneller zurück
        else if (t < forwardTicks + pauseEndTicks + backwardTicks) {
            int backT = t - forwardTicks - pauseEndTicks;

            double progress = (double) backT / (double) backwardTicks;
            offset = overflow - (int) Math.round(progress * overflow);
        }
        // Phase 4: Pause am Anfang
        else {
            offset = 0;
        }

        Shape oldClip = g.getClip();
        g.setClip(x, y - fontHeight, availableWidth, fontHeight + 8);

        g.drawString(text, x - offset, y);

        g.setClip(oldClip);
    }

    private String formatTimeWithDelay(Departure departure) {
        if (departure.getDelayMinutes() <= 0) {
            return departure.getTime();
        }

        return departure.getTime() + " +" + departure.getDelayMinutes();
    }

    private void drawWarningOrFooter(int width, int height,
                                     List<WarnMessage> warnings,
                                     String fallbackText) {

        getLayer().fillRectangle(0, height - 14, width, 14, BLACK);

        if (warnings.isEmpty()) {
            drawText(6, height - 10, fallbackText, WHITE);
            return;
        }

        StringBuilder text = new StringBuilder();

        for (int i = 0; i < warnings.size(); i++) {
            if (i > 0) {
                text.append("  ***  ");
            }

            text.append(warnings.get(i).getMessage());
        }

        String warningText = text.toString();

        int textWidth = warningText.length() * 6;
        int availableWidth = width - 12;

        if (textWidth <= availableWidth) {
            drawText(6, height - 10, warningText, WARNING);
            return;
        }

        int totalScrollWidth = textWidth + availableWidth;
        int offset = textScroll % totalScrollWidth;
        int x = availableWidth - offset + 6;

        drawText(x, height - 10, warningText, WARNING);
    }

    private void drawScrollingTextIfNeeded(Graphics2D g, String text, int x, int y, int availableWidth, int fontHeight) {
        int textWidth = g.getFontMetrics().stringWidth(text);

        if (textWidth <= availableWidth) {
            g.drawString(text, x, y);
            return;
        }

        Shape oldClip = g.getClip();
        g.setClip(x, y - fontHeight, availableWidth, fontHeight + 6);

        String repeatedText = text + "  ***  ";
        int repeatedWidth = g.getFontMetrics().stringWidth(repeatedText);

        int offset = textScroll % repeatedWidth;
        int startX = x - offset;

        for (int drawX = startX; drawX < x + availableWidth; drawX += repeatedWidth) {
            g.drawString(repeatedText, drawX, y);
        }

        g.setClip(oldClip);
    }

    private void drawText(int x, int y, String text, byte color) {
        getLayer().draw(MapFont.MINECRAFT, x, y, color, text);
    }
}