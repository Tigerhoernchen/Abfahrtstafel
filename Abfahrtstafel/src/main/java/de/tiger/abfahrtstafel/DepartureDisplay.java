package de.tiger.abfahrtstafel;

import com.bergerkiller.bukkit.common.map.MapDisplay;
import com.bergerkiller.bukkit.common.map.MapTexture;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DepartureDisplay extends MapDisplay {

    private final LayoutRenderer layoutRenderer = new LayoutRenderer();

    private int updateTicksCounter = 0;
    private int scrollTicksCounter = 0;
    private int textScroll = 0;

    @Override
    public void onAttached() {
        setGlobal(true);
        drawDisplay();
    }

    @Override
    public void onTick() {
        scrollTicksCounter++;
        updateTicksCounter++;

        int scrollSpeed = AbfahrtstafelPlugin
                .getInstance()
                .getTextScrollSpeedTicks();

        if (scrollSpeed < 1) {
            scrollSpeed = 1;
        }

        if (scrollTicksCounter >= scrollSpeed) {
            scrollTicksCounter = 0;
            textScroll++;
        }

        int updateTicks = AbfahrtstafelPlugin
                .getInstance()
                .getDisplayUpdateTicks();

        if (updateTicks < 1) {
            updateTicks = 10;
        }

        if (updateTicksCounter >= updateTicks) {
            updateTicksCounter = 0;
            drawDisplay();
        }
    }

    private void drawDisplay() {
        int width = getWidth();
        int height = getHeight();

        String displayType = properties.get("displayType", "platform");
        String station = properties.get("station", "Start");
        String railGroup = properties.get("railGroup", "G1");

        String layoutName = properties.get(
                "layout",
                displayType.equalsIgnoreCase("station")
                        ? "station-large"
                        : "platform-small"
        );

        DisplayLayout layout = AbfahrtstafelPlugin
                .getInstance()
                .getDisplayLayoutManager()
                .getLayout(layoutName);

        if (layout == null) {
            return;
        }

        List<Departure> renderDepartures = new ArrayList<>();
        List<Departure> stationDepartures = new ArrayList<>();

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("station", station);
        placeholders.put("track", railGroup);
        placeholders.put("currentTime", LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        placeholders.put("currentDate", LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));

        if (displayType.equalsIgnoreCase("platform")) {
            renderDepartures = AbfahrtstafelPlugin
                    .getInstance()
                    .getScheduleManager()
                    .getNextDepartures(station, railGroup, 1);

            stationDepartures = AbfahrtstafelPlugin
                    .getInstance()
                    .getScheduleManager()
                    .getNextDeparturesForStation(station, 5);

            if (!renderDepartures.isEmpty()) {
                Departure departure = renderDepartures.get(0);

                List<WarnMessage> warnings = AbfahrtstafelPlugin
                        .getInstance()
                        .getWarningManager()
                        .getActiveWarnings(
                                station,
                                departure.getPlatform(),
                                departure.getLine()
                        );

                fillDeparturePlaceholders(placeholders, departure, warnings);
            } else {
                List<WarnMessage> warnings = AbfahrtstafelPlugin
                        .getInstance()
                        .getWarningManager()
                        .getActiveWarnings(station, railGroup);

                fillEmptyPlatformPlaceholders(placeholders, warnings);
            }
        } else {
            renderDepartures = AbfahrtstafelPlugin
                    .getInstance()
                    .getScheduleManager()
                    .getNextDeparturesForStation(
                            station,
                            AbfahrtstafelPlugin.getInstance().getStationDisplayMaxEntries()
                    );

            stationDepartures = renderDepartures;

            if (!renderDepartures.isEmpty()) {
                Departure firstDeparture = renderDepartures.get(0);

                List<WarnMessage> warnings = AbfahrtstafelPlugin
                        .getInstance()
                        .getWarningManager()
                        .getActiveWarnings(
                                station,
                                firstDeparture.getPlatform(),
                                firstDeparture.getLine()
                        );

                fillDeparturePlaceholders(placeholders, firstDeparture, warnings);
            } else {
                fillEmptyStationPlaceholders(placeholders);
            }
        }

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        g.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON
        );

        layoutRenderer.render(
                g,
                layout,
                placeholders,
                renderDepartures,
                stationDepartures,
                textScroll,
                width,
                height
        );

        g.dispose();
        getLayer().draw(MapTexture.fromImage(image), 0, 0);
    }

    private void fillDeparturePlaceholders(Map<String, String> placeholders,
                                           Departure departure,
                                           List<WarnMessage> warnings) {
        placeholders.put("line", departure.getLine());
        placeholders.put("time", departure.getTime());
        placeholders.put("expected", getExpectedTime(departure));
        placeholders.put("delay", getDelayText(departure));
        placeholders.put("delayMinutes", String.valueOf(departure.getDelayMinutes()));
        placeholders.put("destination", departure.getDestination());
        placeholders.put("via", departure.getVia());
        placeholders.put("track", departure.getPlatform());
        placeholders.put("warnings", buildWarningText(warnings));
    }

    private void fillEmptyPlatformPlaceholders(Map<String, String> placeholders,
                                               List<WarnMessage> warnings) {
        placeholders.put("line", "");
        placeholders.put("time", "");
        placeholders.put("expected", "");
        placeholders.put("delay", "");
        placeholders.put("delayMinutes", "0");
        placeholders.put("destination", "Keine Abfahrt");
        placeholders.put("via", "");
        placeholders.put("warnings", buildWarningText(warnings));
    }

    private void fillEmptyStationPlaceholders(Map<String, String> placeholders) {
        placeholders.put("line", "");
        placeholders.put("time", "");
        placeholders.put("expected", "");
        placeholders.put("delay", "");
        placeholders.put("delayMinutes", "0");
        placeholders.put("destination", "Keine Abfahrten gefunden");
        placeholders.put("via", "");
        placeholders.put("warnings", "");
    }

    private String getExpectedTime(Departure departure) {
        if (departure.getDelayMinutes() <= 0) {
            return "";
        }

        return LocalTime.parse(departure.getTime())
                .plusMinutes(departure.getDelayMinutes())
                .format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    private String getDelayText(Departure departure) {
        if (departure.getDelayMinutes() <= 0) {
            return "";
        }

        return "+" + departure.getDelayMinutes();
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

    public int getTextScroll() {
        return textScroll;
    }
}