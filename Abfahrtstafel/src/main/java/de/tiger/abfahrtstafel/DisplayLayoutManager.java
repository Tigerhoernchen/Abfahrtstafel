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
            String description = layoutSection.getString("description", "");

            DisplayDefaults defaults = loadDefaults(
                    layoutSection.getConfigurationSection("defaults")
            );

            List<DisplayElement> elements = new ArrayList<>();
            List<DisplaySection> sections = new ArrayList<>();

            // Alte Struktur weiterhin unterstützen
            if (layoutSection.isList("elements")) {
                elements = parseElements(layoutSection.getMapList("elements"), defaults);
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
                        sectionElements = parseElements(elementMaps, defaults);
                    }

                    sections.add(new DisplaySection(when, sectionElements));
                }
            }

            DisplayLayout layout = new DisplayLayout(
                    layoutName,
                    displayType,
                    widthBlocks,
                    heightBlocks,
                    background,
                    description,
                    defaults,
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

    private List<DisplayElement> parseElements(List<Map<?, ?>> rawElements, DisplayDefaults defaults) {
        List<DisplayElement> elements = new ArrayList<>();

        for (Map<?, ?> rawElement : rawElements) {
            String type = getString(rawElement, "type", "text");
            String value = getString(rawElement, "value", "");
            int x = getInt(rawElement, "x", 0);
            int y = getInt(rawElement, "y", 0);
            String width = getString(rawElement, "width", "fill");
            int height = getInt(rawElement, "height", 0);
            String align = getString(rawElement, "align", "left");
            String font = getString(rawElement, "font", defaults.getFont());
            String scroll = getString(rawElement, "scroll", defaults.getScroll());
            int fontSize = getInt(rawElement, "fontSize", defaults.getFontSize());
            String color = getString(rawElement, "color", defaults.getColor());
            String fontStyle = getString(rawElement, "fontStyle", defaults.getFontStyle());
            String padding = getString(rawElement, "padding", defaults.getPadding());
            String scrollSeparator = getString(rawElement, "scrollSeparator", defaults.getScrollSeparator());
            String elementBackground = getString(rawElement, "background", "");
            String zebra = getString(rawElement, "zebra", "");
            int thickness = getInt(rawElement, "thickness", 1);
            String rowHeight = getString(rawElement, "rowHeight", "20");
            int minRowHeight = getInt(rawElement, "minRowHeight", 20);
            int maxRows = getInt(rawElement, "maxRows", 10);
            String showWhen = getString(rawElement, "showWhen", "");
            String source = getString(rawElement, "source", "");
            String sortBy = getString(rawElement, "sortBy", "");
            int limit = getInt(rawElement, "limit", 0);
            boolean blink = Boolean.parseBoolean(getString(rawElement, "blink", "false"));
            int blinkTicks = getInt(rawElement, "blinkTicks", 20);

            List<DisplayColumn> columns = new ArrayList<>();

            Object rawColumnsObject = rawElement.get("columns");

            if (rawColumnsObject instanceof List<?> rawColumns) {
                for (Object rawColumnObject : rawColumns) {
                    if (!(rawColumnObject instanceof Map<?, ?> rawColumn)) {
                        continue;
                    }

                    String columnValue = getString(rawColumn, "value", "");
                    String columnHeader = getString(rawColumn, "header", "");
                    List<DisplayColumn> columnVariants = new ArrayList<>();
                    int columnX = getInt(rawColumn, "x", 0);
                    String columnWidth = getString(rawColumn, "width", "50");
                    String columnAlign = getString(rawColumn, "align", "left");
                    String columnColor = getString(rawColumn, "color", color);
                    int columnFontSize = getInt(rawColumn, "fontSize", fontSize);
                    String columnScroll = getString(rawColumn, "scroll", scroll);
                    String columnFont = getString(rawColumn, "font", font);
                    String columnFontStyle = getString(rawColumn, "fontStyle", fontStyle);
                    String columnScrollSeparator = getString(rawColumn, "scrollSeparator", scrollSeparator);
                    String columnBackground = getString(rawColumn, "background", "");
                    String columnPadding = getString(rawColumn, "padding", "0");
                    String columnShowWhen = getString(rawColumn, "showWhen", "");
                    boolean columnBlink = Boolean.parseBoolean(getString(rawColumn, "blink", "false"));
                    int columnBlinkTicks = getInt(rawColumn, "blinkTicks", 20);

                    Object rawVariantsObject = rawColumn.get("variants");

                    if (rawVariantsObject instanceof List<?> rawVariants) {
                        for (Object rawVariantObject : rawVariants) {
                            if (!(rawVariantObject instanceof Map<?, ?> rawVariant)) {
                                continue;
                            }

                            String variantValue = getString(rawVariant, "value", "");
                            String variantAlign = getString(rawVariant, "align", columnAlign);
                            String variantColor = getString(rawVariant, "color", columnColor);
                            int variantFontSize = getInt(rawVariant, "fontSize", columnFontSize);
                            String variantScroll = getString(rawVariant, "scroll", columnScroll);
                            String variantFont = getString(rawVariant, "font", columnFont);
                            String variantFontStyle = getString(rawVariant, "fontStyle", columnFontStyle);
                            String variantScrollSeparator = getString(rawVariant, "scrollSeparator", columnScrollSeparator);
                            String variantBackground = getString(rawVariant, "background", columnBackground);
                            String variantPadding = getString(rawVariant, "padding", columnPadding);
                            String variantShowWhen = getString(rawVariant, "showWhen", "");
                            boolean variantBlink = Boolean.parseBoolean(
                                    getString(rawVariant, "blink", "false")
                            );
                            int variantBlinkTicks = getInt(rawVariant, "blinkTicks", columnBlinkTicks);

                            columnVariants.add(new DisplayColumn(
                                    variantValue,
                                    columnX,
                                    columnWidth,
                                    variantAlign,
                                    variantColor,
                                    variantFontSize,
                                    variantScroll,
                                    variantFont,
                                    variantFontStyle,
                                    variantScrollSeparator,
                                    variantBackground,
                                    variantPadding,
                                    variantShowWhen,
                                    variantBlink,
                                    variantBlinkTicks,
                                    "",
                                    java.util.Collections.emptyList()
                            ));
                        }
                    }


                    columns.add(new DisplayColumn(
                            columnValue,
                            columnX,
                            columnWidth,
                            columnAlign,
                            columnColor,
                            columnFontSize,
                            columnScroll,
                            columnFont,
                            columnFontStyle,
                            columnScrollSeparator,
                            columnBackground,
                            columnPadding,
                            columnShowWhen,
                            columnBlink,
                            columnBlinkTicks,
                            columnHeader,
                            columnVariants
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
                    zebra,
                    thickness,
                    rowHeight,
                    minRowHeight,
                    maxRows,
                    columns,
                    showWhen,
                    source,
                    sortBy,
                    limit,
                    font,
                    fontStyle,
                    padding,
                    scrollSeparator,
                    blink,
                    blinkTicks
            ));
        }

        return elements;
    }

    private DisplayDefaults loadDefaults(ConfigurationSection section) {
        DisplayDefaults fallback = DisplayDefaults.defaultValues();

        if (section == null) {
            return fallback;
        }

        return new DisplayDefaults(
                section.getString("font", fallback.getFont()),
                section.getInt("fontSize", fallback.getFontSize()),
                section.getString("color", fallback.getColor()),
                section.getString("scroll", fallback.getScroll()),
                section.getString("fontStyle", fallback.getFontStyle()),
                section.getString("padding", fallback.getPadding()),
                section.getString("scrollSeparator", fallback.getScrollSeparator())
        );
    }
}