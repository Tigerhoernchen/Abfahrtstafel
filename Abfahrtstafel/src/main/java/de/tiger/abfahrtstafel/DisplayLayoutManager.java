package de.tiger.abfahrtstafel;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Collections;

public class DisplayLayoutManager {

    private final AbfahrtstafelPlugin plugin;
    private final Map<String, DisplayLayout> layouts = new HashMap<>();

    public DisplayLayoutManager(AbfahrtstafelPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        layouts.clear();

        File file = new File(plugin.getDataFolder(), "DisplayLayouts.yml");

        if (!file.exists()) {
            plugin.saveResource("DisplayLayouts.yml", false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("layouts");

        if (section == null) {
            plugin.getLogger().warning("DisplayLayouts.yml enthält keine layouts-Sektion.");
            return;
        }

        for (String layoutName : section.getKeys(false)) {
            ConfigurationSection layoutSection = section.getConfigurationSection(layoutName);

            if (layoutSection == null) {
                continue;
            }

            String displayType = layoutSection.getString("displayType", "platform");
            int widthBlocks = layoutSection.getInt("widthBlocks", 1);
            int heightBlocks = layoutSection.getInt("heightBlocks", 1);
            String background = layoutSection.getString("background", "#2B2D8D");

            List<DisplayElement> elements = new ArrayList<>();
            List<DisplaySection> sections = new ArrayList<>();

            // Alte Struktur weiterhin unterstützen
            if (layoutSection.isList("elements")) {
                elements = parseElements(layoutSection.getMapList("elements"));
            }

            // Neue sections-Struktur
            if (layoutSection.isList("sections")) {
                for (Map<?, ?> rawSection : layoutSection.getMapList("sections")) {
                    String when = getString(rawSection, "when", "");

                    List<DisplayElement> sectionElements = new ArrayList<>();

                    Object rawElementsObject = rawSection.get("elements");
                    if (rawElementsObject instanceof List<?> rawElements) {
                        @SuppressWarnings("unchecked")
                        List<Map<?, ?>> elementMaps = (List<Map<?, ?>>) rawElements;
                        sectionElements = parseElements(elementMaps);
                    }

                    sections.add(new DisplaySection(when, sectionElements));
                }
            }


            for (Map<?, ?> rawElement : layoutSection.getMapList("elements")) {
                String type = getString(rawElement, "type", "text");
                String value = getString(rawElement, "value", "");
                int x = getInt(rawElement, "x", 0);
                int y = getInt(rawElement, "y", 0);
                String width = getString(rawElement, "width", "fill");
                int height = getInt(rawElement, "height", 0);
                String align = getString(rawElement, "align", "left");
                String scroll = getString(rawElement, "scroll", "none");
                int fontSize = getInt(rawElement, "fontSize", 14);
                String color = getString(rawElement, "color", "#FFFFFF");
                String elementBackground = getString(rawElement, "background", "");
                int thickness = getInt(rawElement, "thickness", 1);
                int rowHeight = getInt(rawElement, "rowHeight", 20);
                int maxRows = getInt(rawElement, "maxRows", 10);
                String showWhen = getString(rawElement, "showWhen", "always");
                String source = getString(rawElement, "source", "platform");
                List<DisplayColumn> columns = new ArrayList<>();

                Object rawColumnsObject = rawElement.get("columns");

                if (rawColumnsObject instanceof List<?> rawColumns) {
                    for (Object rawColumnObject : rawColumns) {
                        if (!(rawColumnObject instanceof Map<?, ?> rawColumn)) {
                            continue;
                        }

                        String columnValue = getString(rawColumn, "value", "");
                        int columnX = getInt(rawColumn, "x", 0);
                        String columnWidth = getString(rawColumn, "width", "50");
                        String columnAlign = getString(rawColumn, "align", "left");
                        String columnColor = getString(rawColumn, "color", "#FFFFFF");
                        int columnFontSize = getInt(rawColumn, "fontSize", fontSize);
                        String columnScroll = getString(rawColumn, "scroll", "none");

                        columns.add(new DisplayColumn(
                                columnValue,
                                columnX,
                                columnWidth,
                                columnAlign,
                                columnColor,
                                columnFontSize,
                                columnScroll
                        ));
                    }
                }

                elements.add(new DisplayElement(
                        type,
                        value,
                        x,
                        y,
                        width,
                        height,
                        align,
                        scroll,
                        fontSize,
                        color,
                        elementBackground,
                        thickness,
                        rowHeight,
                        maxRows,
                        columns,
                        showWhen,
                        source
                ));
            }

            DisplayLayout layout = new DisplayLayout(
                    layoutName,
                    displayType,
                    widthBlocks,
                    heightBlocks,
                    background,
                    elements,
                    sections
            );

            layouts.put(layoutName.toLowerCase(), layout);
        }

        plugin.getLogger().info("DisplayLayouts geladen: " + layouts.size() + " Layouts");
    }

    public DisplayLayout getLayout(String name) {
        if (name == null) {
            return null;
        }

        return layouts.get(name.toLowerCase());
    }

    public boolean hasLayout(String name) {
        return getLayout(name) != null;
    }

    public Collection<DisplayLayout> getLayouts() {
        return layouts.values();
    }

    public Set<String> getLayoutNames() {
        return layouts.keySet();
    }

    private String getString(Map<?, ?> map, String key, String defaultValue) {
        Object value = map.get(key);

        if (value == null) {
            return defaultValue;
        }

        return String.valueOf(value);
    }

    private int getInt(Map<?, ?> map, String key, int defaultValue) {
        Object value = map.get(key);

        if (value == null) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private List<DisplayElement> parseElements(List<Map<?, ?>> rawElements) {
        List<DisplayElement> elements = new ArrayList<>();

        for (Map<?, ?> rawElement : rawElements) {
            String type = getString(rawElement, "type", "text");
            String value = getString(rawElement, "value", "");
            int x = getInt(rawElement, "x", 0);
            int y = getInt(rawElement, "y", 0);
            String width = getString(rawElement, "width", "fill");
            int height = getInt(rawElement, "height", 0);
            String align = getString(rawElement, "align", "left");
            String scroll = getString(rawElement, "scroll", "none");
            int fontSize = getInt(rawElement, "fontSize", 14);
            String color = getString(rawElement, "color", "#FFFFFF");
            String elementBackground = getString(rawElement, "background", "");
            int thickness = getInt(rawElement, "thickness", 1);
            int rowHeight = getInt(rawElement, "rowHeight", 20);
            int maxRows = getInt(rawElement, "maxRows", 10);
            String showWhen = getString(rawElement, "showWhen", "");
            String source = getString(rawElement, "source", "");

            List<DisplayColumn> columns = new ArrayList<>();

            Object rawColumnsObject = rawElement.get("columns");

            if (rawColumnsObject instanceof List<?> rawColumns) {
                for (Object rawColumnObject : rawColumns) {
                    if (!(rawColumnObject instanceof Map<?, ?> rawColumn)) {
                        continue;
                    }

                    String columnValue = getString(rawColumn, "value", "");
                    int columnX = getInt(rawColumn, "x", 0);
                    String columnWidth = getString(rawColumn, "width", "50");
                    String columnAlign = getString(rawColumn, "align", "left");
                    String columnColor = getString(rawColumn, "color", "#FFFFFF");
                    int columnFontSize = getInt(rawColumn, "fontSize", fontSize);
                    String columnScroll = getString(rawColumn, "scroll", "none");

                    columns.add(new DisplayColumn(
                            columnValue,
                            columnX,
                            columnWidth,
                            columnAlign,
                            columnColor,
                            columnFontSize,
                            columnScroll
                    ));
                }
            }

            elements.add(new DisplayElement(
                    type,
                    value,
                    x,
                    y,
                    width,
                    height,
                    align,
                    scroll,
                    fontSize,
                    color,
                    elementBackground,
                    thickness,
                    rowHeight,
                    maxRows,
                    columns,
                    showWhen,
                    source
            ));
        }

        return elements;
    }
}