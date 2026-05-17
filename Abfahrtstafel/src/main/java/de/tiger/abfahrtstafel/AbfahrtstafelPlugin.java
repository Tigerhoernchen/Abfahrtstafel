package de.tiger.abfahrtstafel;

import com.bergerkiller.bukkit.common.map.MapDisplayProperties;
import com.bergerkiller.bukkit.tc.signactions.SignAction;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.GlowItemFrame;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class AbfahrtstafelPlugin extends JavaPlugin {

    private static AbfahrtstafelPlugin instance;

    private ScheduleManager scheduleManager;
    private WarningManager warningManager;
    private RuntimeStateManager runtimeStateManager;
    private DisplayLayoutManager displayLayoutManager;
    private SoundManager soundManager;
    private SoundCategory announcementSoundCategory = SoundCategory.MASTER;
    private StationAliasManager stationAliasManager;

    private final SignActionAbfahrt signActionAbfahrt = new SignActionAbfahrt();
    private final SignActionAnkunft signActionAnkunft = new SignActionAnkunft();

    @Override
    public void onLoad() {
        SignAction.register(signActionAbfahrt);
        SignAction.register(signActionAnkunft);
    }

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        reloadConfig();
        loadAnnouncementSoundCategory();

        runtimeStateManager = new RuntimeStateManager();

        stationAliasManager = new StationAliasManager(this);
        stationAliasManager.load();

        displayLayoutManager = new DisplayLayoutManager(this);
        displayLayoutManager.load();

        scheduleManager = new ScheduleManager(this);
        scheduleManager.load();

        warningManager = new WarningManager(this);

        soundManager = new SoundManager(this);
        soundManager.load();

        getLogger().info("[Abfahrtstafel] Plugin gestartet!");
        getLogger().info("[Abfahrtstafel] BKCommonLib und TrainCarts wurden gefunden!");
    }

    @Override
    public void onDisable() {
        SignAction.unregister(signActionAbfahrt);
        SignAction.unregister(signActionAnkunft);
        getLogger().info("Abfahrtstafel Plugin gestoppt!");
    }

    public static AbfahrtstafelPlugin getInstance() {
        return instance;
    }

    public ScheduleManager getScheduleManager() {
        return scheduleManager;
    }

    public WarningManager getWarningManager() {
        return warningManager;
    }

    public RuntimeStateManager getRuntimeStateManager() {
        return runtimeStateManager;
    }

    public DisplayLayoutManager getDisplayLayoutManager() {
        return displayLayoutManager;
    }

    public SoundManager getSoundManager() {
        return soundManager;
    }

    public SoundCategory getAnnouncementSoundCategory() {
        return announcementSoundCategory;
    }

    public boolean isDebugSignActions() {
        return getConfig().getBoolean("debugSignActions", false);
    }

    public int getTextScrollSpeedTicks() {
        return getConfig().getInt("textScrollSpeedTicks", 1);
    }

    public int getDepartureTimeoutMinutes() {
        return getConfig().getInt("departureTimeoutMinutes", 5);
    }

    public int getStationLookAheadMinutes() {
        return getConfig().getInt("stationLookAheadMinutes", 120);
    }

    public int getPlatformLookAheadMinutes() {
        return getConfig().getInt("platformLookAheadMinutes", 180);
    }

    public int getStationDisplayMaxEntries() {
        return getConfig().getInt("stationDisplayMaxEntries", 12);
    }

    public int getDisplayUpdateTicks() {
        return getConfig().getInt("displayUpdateTicks", 10);
    }

    public int getArrivalTriggerLookAheadMinutes() {
        return getConfig().getInt("arrivalTriggerLookAheadMinutes", 5);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("abfahrtstafel")) {
            return false;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!checkPermission(sender, "abfahrtstafel.reload")) {
                return true;
            }

            reloadConfig();
            loadAnnouncementSoundCategory();
            stationAliasManager.load();
            scheduleManager.load();
            warningManager.load();
            displayLayoutManager.load();
            soundManager.load();

            sender.sendMessage(ChatColor.GREEN + "Dateien wurden neu geladen.");
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("layouts")) {
            if (!checkPermission(sender, "abfahrtstafel.admin")) {
                return true;
            }

            sender.sendMessage(ChatColor.YELLOW + "Verfügbare DisplayLayouts:");

            for (DisplayLayout layout : displayLayoutManager.getLayouts()) {
                String description = layout.getDescription();

                if (description == null || description.isBlank()) {
                    description = "Keine Beschreibung";
                }

                sender.sendMessage(ChatColor.GRAY + "- "
                        + ChatColor.AQUA + layout.getName()
                        + ChatColor.GRAY + " [" + layout.getDisplayType()
                        + ", " + layout.getWidthBlocks() + "x" + layout.getHeightBlocks() + "] "
                        + ChatColor.WHITE + description);
            }

            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("clearstate")) {
            if (!checkPermission(sender, "abfahrtstafel.admin")) {
                return true;
            }

            runtimeStateManager.clear();
            sender.sendMessage(ChatColor.GREEN + "Runtime-State wurde geleert.");
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("place")) {
            if (!checkPermission(sender, "abfahrtstafel.admin")) {
                return true;
            }

            if (!(sender instanceof Player player)) {
                sender.sendMessage("Dieser Befehl kann nur im Spiel benutzt werden.");
                return true;
            }

            return handlePlaceLayoutCommand(player, sender, args);
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("remove")) {
            if (!checkPermission(sender, "abfahrtstafel.admin")) {
                return true;
            }

            if (!(sender instanceof Player player)) {
                sender.sendMessage("Dieser Befehl kann nur im Spiel benutzt werden.");
                return true;
            }

            ItemFrame frame = getTargetItemFrame(player, 8);

            if (frame == null) {
                sender.sendMessage(ChatColor.RED + "Bitte schaue einen Item-Frame an.");
                return true;
            }

            int removed = removeConnectedFrames(frame);
            sender.sendMessage(ChatColor.GREEN + "Anzeige entfernt: " + removed + " Rahmen.");
            return true;
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("trigger")) {
            if (!checkPermission(sender, "abfahrtstafel.admin")) {
                return true;
            }

            String stationAndRail = joinArgs(args, 1);

            if (!stationAndRail.contains(":")) {
                sender.sendMessage(ChatColor.RED + "Nutzung: /abfahrtstafel trigger <Station>:<Gleis>");
                return true;
            }

            String[] parts = stationAndRail.split(":", 2);
            String station = parts[0].trim();
            String railGroup = parts[1].trim();
            station = stationAliasManager.resolve(station);

            boolean success = scheduleManager.processNextDeparture(station, railGroup);

            if (success) {
                sender.sendMessage(ChatColor.GREEN + "Naechste Abfahrt fuer "
                        + station + ":" + railGroup + " wurde abgearbeitet.");
            } else {
                sender.sendMessage(ChatColor.RED + "Keine passende Abfahrt fuer "
                        + station + ":" + railGroup + " gefunden.");
            }

            return true;
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("warn")) {
            if (!checkPermission(sender, "abfahrtstafel.warn")) {
                return true;
            }

            return handleWarnCommand(sender, args);
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("soundbox")) {
            if (!checkPermission(sender, "abfahrtstafel.admin")) {
                return true;
            }

            if (args[1].equalsIgnoreCase("list")) {
                sender.sendMessage(ChatColor.YELLOW + "Soundboxen:");

                for (SoundBox box : soundManager.getSoundBoxes()) {
                    sender.sendMessage(ChatColor.GRAY + "#" + box.getId()
                            + ChatColor.AQUA + " " + box.getStation() + ":" + box.getRailGroup()
                            + ChatColor.GRAY + " " + box.getWorld()
                            + " " + String.format("%.1f %.1f %.1f", box.getX(), box.getY(), box.getZ())
                            + " r=" + box.getRadius()
                            + " v=" + box.getVolume()
                            + " p=" + box.getPitch());
                }

                return true;
            }

            if (args[1].equalsIgnoreCase("create")) {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Dieser Befehl kann nur im Spiel benutzt werden.");
                    return true;
                }

                if (args.length < 4) {
                    sender.sendMessage(ChatColor.RED + "Nutzung: /abfahrtstafel soundbox create <Gleis> <Bahnhof>");
                    return true;
                }

                String railGroup = args[2];
                String station = stationAliasManager.resolve(joinArgs(args, 3));

                SoundBox box = soundManager.createSoundBox(station, railGroup, player.getLocation());

                if (box == null) {
                    sender.sendMessage(ChatColor.RED + "Soundbox konnte nicht erstellt werden.");
                    return true;
                }

                sender.sendMessage(ChatColor.GREEN + "Soundbox erstellt: #" + box.getId()
                        + " für " + station + ":" + railGroup);
                return true;
            }

            if (args[1].equalsIgnoreCase("remove")) {
                if (args.length != 3) {
                    sender.sendMessage(ChatColor.RED + "Nutzung: /abfahrtstafel soundbox remove <id>");
                    return true;
                }

                int id;

                try {
                    id = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Ungültige ID.");
                    return true;
                }

                if (soundManager.removeSoundBox(id)) {
                    sender.sendMessage(ChatColor.GREEN + "Soundbox #" + id + " entfernt.");
                } else {
                    sender.sendMessage(ChatColor.RED + "Soundbox #" + id + " nicht gefunden.");
                }

                return true;
            }

            sender.sendMessage(ChatColor.YELLOW + "/abfahrtstafel soundbox create <Gleis> <Bahnhof>");
            sender.sendMessage(ChatColor.YELLOW + "/abfahrtstafel soundbox list");
            sender.sendMessage(ChatColor.YELLOW + "/abfahrtstafel soundbox remove <id>");
            return true;
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("debug")) {
            if (!checkPermission(sender, "abfahrtstafel.admin")) {
                return true;
            }

            String debugTarget = args[1].toLowerCase();

            if (debugTarget.equals("signactions")) {
                if (args.length != 3) {
                    sender.sendMessage(ChatColor.YELLOW + "Nutzung: /abfahrtstafel debug signactions <true|false>");
                    return true;
                }

                boolean value = Boolean.parseBoolean(args[2]);
                getConfig().set("debugSignActions", value);
                saveConfig();

                sender.sendMessage(ChatColor.GREEN
                        + "Debug für SignActions "
                        + (value ? "aktiviert" : "deaktiviert") + ".");
                return true;
            }

            if (debugTarget.equals("trigger")) {
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.YELLOW + "Nutzung: /abfahrtstafel debug trigger <Station>:<Gleis>");
                    return true;
                }

                String stationAndRail = joinArgs(args, 2);

                if (!stationAndRail.contains(":")) {
                    sender.sendMessage(ChatColor.RED + "Nutzung: /abfahrtstafel debug trigger <Station>:<Gleis>");
                    return true;
                }

                String[] parts = stationAndRail.split(":", 2);
                String station = parts[0].trim();
                String railGroup = parts[1].trim();
                station = stationAliasManager.resolve(station);

                boolean success = scheduleManager.processNextDeparture(station, railGroup);

                if (success) {
                    sender.sendMessage(ChatColor.GREEN + "Debug-Trigger ausgeführt für "
                            + station + ":" + railGroup + ".");
                } else {
                    sender.sendMessage(ChatColor.RED + "Debug-Trigger: Keine passende Abfahrt gefunden für "
                            + station + ":" + railGroup + ".");
                }

                return true;
            }

            if (debugTarget.equals("clearstate")) {
                runtimeStateManager.clear();
                sender.sendMessage(ChatColor.GREEN + "Debug: Runtime-State wurde geleert.");
                return true;
            }

            if (debugTarget.equals("fonts")) {
                String[] fonts = java.awt.GraphicsEnvironment
                        .getLocalGraphicsEnvironment()
                        .getAvailableFontFamilyNames();

                sender.sendMessage(ChatColor.GREEN + "Verfügbare Schriftarten:");

                for (String font : fonts) {
                    sender.sendMessage(ChatColor.GRAY + "- " + font);
                }

                sender.sendMessage(ChatColor.YELLOW
                        + "Anzahl: " + fonts.length);

                return true;
            }

            if (debugTarget.equals("soundbox")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage("Dieser Befehl kann nur im Spiel benutzt werden.");
                    return true;
                }

                if (args.length < 4) {
                    sender.sendMessage(ChatColor.YELLOW
                            + "Nutzung: /abfahrtstafel debug soundbox <Gleis> <Bahnhof>");
                    return true;
                }

                String railGroup = args[2];
                String station = stationAliasManager.resolve(joinArgs(args, 3));

                soundManager.playPlatformSound(
                        station,
                        railGroup,
                        "minecraft:block.note_block.pling"
                );

                sender.sendMessage(ChatColor.GREEN
                        + "Testsound für " + station + ":" + railGroup + " abgespielt.");

                return true;
            }

            sender.sendMessage(ChatColor.YELLOW
                    + "Nutzung: /abfahrtstafel debug <signactions|soundbox|trigger|clearstate>");
            return true;
        }

        sendHelp(sender);
        return true;
    }

    private boolean handlePlaceLayoutCommand(Player player, CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Nutzung: /abfahrtstafel place <DisplayName> <normal|glow> <Station>");
            sender.sendMessage(ChatColor.RED + "Oder: /abfahrtstafel place <DisplayName> <normal|glow> <Gleis> <Station>");
            return true;
        }

        String layoutName = args[1];
        String frameType = args[2].toLowerCase();

        boolean glow;

        if (frameType.equals("normal")) {
            glow = false;
        } else if (frameType.equals("glow")) {
            glow = true;
        } else {
            sender.sendMessage(ChatColor.RED + "Rahmentyp muss normal oder glow sein.");
            return true;
        }

        DisplayLayout layout = displayLayoutManager.getLayout(layoutName);

        if (layout == null) {
            sender.sendMessage(ChatColor.RED + "DisplayLayout nicht gefunden: " + layoutName);
            return true;
        }

        String displayType = layout.getDisplayType();

        String station;
        String railGroup = null;

        if (displayType.equalsIgnoreCase("platform")) {
            if (args.length < 5) {
                sender.sendMessage(ChatColor.RED + "Nutzung: /abfahrtstafel place "
                        + layoutName + " <normal|glow> <Gleis> <Station>");
                return true;
            }

            railGroup = args[3];
            station = stationAliasManager.resolve(joinArgs(args, 4));
        } else if (displayType.equalsIgnoreCase("station")) {
            if (args.length < 4) {
                sender.sendMessage(ChatColor.RED + "Nutzung: /abfahrtstafel place "
                        + layoutName + " <normal|glow> <Station>");
                return true;
            }

            station = stationAliasManager.resolve(joinArgs(args, 3));
        } else {
            sender.sendMessage(ChatColor.RED + "Unbekannter displayType im Layout: " + displayType);
            return true;
        }

        ItemStack item = createDisplayItem(displayType, station, railGroup, layoutName);

        int placed = placeLayoutFrames(player, item, glow, layout.getWidthBlocks(), layout.getHeightBlocks());

        if (placed <= 0) {
            sender.sendMessage(ChatColor.RED + "Display konnte nicht platziert werden.");
            return true;
        }

        if (railGroup == null) {
            sender.sendMessage(ChatColor.GREEN + "Display platziert: " + layoutName
                    + " für " + station + " (" + placed + " Rahmen)");
        } else {
            sender.sendMessage(ChatColor.GREEN + "Display platziert: " + layoutName
                    + " für " + station + ":" + railGroup + " (" + placed + " Rahmen)");
        }

        return true;
    }

    private ItemStack createDisplayItem(String displayType, String station, String railGroup, String layoutName) {
        MapDisplayProperties properties = MapDisplayProperties.createNew(DepartureDisplay.class);
        properties.set("displayType", displayType);
        properties.set("station", station);
        properties.set("layout", layoutName);

        if (railGroup != null) {
            properties.set("railGroup", railGroup);
        }

        return properties.getMapItem();
    }

    private int placeLayoutFrames(Player player,
                                  ItemStack item,
                                  boolean glow,
                                  int widthBlocks,
                                  int heightBlocks) {

        Block targetBlock = player.getTargetBlockExact(10);

        if (targetBlock == null) {
            player.sendMessage(ChatColor.RED + "Bitte schaue einen Block an.");
            return 0;
        }

        World world = targetBlock.getWorld();
        BlockFace facing = getOppositeCardinalFacing(player);

        Location base = targetBlock.getLocation().add(
                facing.getModX(),
                facing.getModY(),
                facing.getModZ()
        );

        BlockFace right = getRightFace(facing);

        List<Location> frameLocations = new ArrayList<>();

        for (int y = 0; y < heightBlocks; y++) {
            for (int x = 0; x < widthBlocks; x++) {
                Location location = base.clone()
                        .add(
                                -right.getModX() * x,
                                -y,
                                -right.getModZ() * x
                        );

                frameLocations.add(location);
            }
        }

        for (Location location : frameLocations) {
            if (hasItemFrameAt(location)) {
                player.sendMessage(ChatColor.RED + "In der Displayfläche befindet sich bereits ein ItemFrame.");
                return 0;
            }

            Block block = world.getBlockAt(location);

            if (!block.getType().isAir()) {
                player.sendMessage(ChatColor.RED + "Die Displayfläche ist nicht frei.");
                return 0;
            }
        }

        int placed = 0;

        for (Location location : frameLocations) {
            Location spawnLocation = location.clone().add(0.5, 0.5, 0.5);

            ItemFrame frame;

            if (glow) {
                frame = (GlowItemFrame) world.spawnEntity(spawnLocation, EntityType.GLOW_ITEM_FRAME);
            } else {
                frame = (ItemFrame) world.spawnEntity(spawnLocation, EntityType.ITEM_FRAME);
            }

            frame.setFacingDirection(facing, true);
            frame.setItem(item.clone(), false);
            frame.setFixed(true);
            frame.setVisible(true);

            placed++;
        }

        return placed;
    }

    private BlockFace getRightFace(BlockFace facing) {
        return switch (facing) {
            case NORTH -> BlockFace.EAST;
            case SOUTH -> BlockFace.WEST;
            case EAST -> BlockFace.SOUTH;
            case WEST -> BlockFace.NORTH;
            default -> BlockFace.EAST;
        };
    }

    private BlockFace getOppositeCardinalFacing(Player player) {
        BlockFace facing = player.getFacing();

        return switch (facing) {
            case NORTH -> BlockFace.SOUTH;
            case SOUTH -> BlockFace.NORTH;
            case EAST -> BlockFace.WEST;
            case WEST -> BlockFace.EAST;
            default -> BlockFace.SOUTH;
        };
    }

    private boolean hasItemFrameAt(Location location) {
        World world = location.getWorld();

        if (world == null) {
            return false;
        }

        int blockX = location.getBlockX();
        int blockY = location.getBlockY();
        int blockZ = location.getBlockZ();

        for (ItemFrame frame : world.getEntitiesByClass(ItemFrame.class)) {
            Location frameLocation = frame.getLocation();

            if (frameLocation.getBlockX() == blockX
                    && frameLocation.getBlockY() == blockY
                    && frameLocation.getBlockZ() == blockZ) {
                return true;
            }
        }

        return false;
    }

    private ItemFrame getTargetItemFrame(Player player, double maxDistance) {
        Location eye = player.getEyeLocation();
        World world = player.getWorld();

        ItemFrame closestFrame = null;
        double closestDistance = maxDistance;

        for (ItemFrame frame : world.getEntitiesByClass(ItemFrame.class)) {
            Location frameLocation = frame.getLocation();

            if (frameLocation.distance(eye) > maxDistance) {
                continue;
            }

            double dot = player.getEyeLocation().getDirection().normalize()
                    .dot(frameLocation.clone().add(0, 0.25, 0)
                            .subtract(eye)
                            .toVector()
                            .normalize());

            if (dot < 0.98) {
                continue;
            }

            double distance = frameLocation.distance(eye);

            if (distance < closestDistance) {
                closestDistance = distance;
                closestFrame = frame;
            }
        }

        return closestFrame;
    }

    private int removeConnectedFrames(ItemFrame startFrame) {
        World world = startFrame.getWorld();
        BlockFace facing = startFrame.getFacing();
        Location start = startFrame.getLocation();

        int removed = 0;

        List<ItemFrame> toRemove = new ArrayList<>();
        List<ItemFrame> open = new ArrayList<>();

        open.add(startFrame);

        while (!open.isEmpty()) {
            ItemFrame current = open.remove(0);

            if (toRemove.contains(current)) {
                continue;
            }

            toRemove.add(current);

            Location currentLocation = current.getLocation();

            for (ItemFrame other : world.getEntitiesByClass(ItemFrame.class)) {
                if (toRemove.contains(other) || open.contains(other)) {
                    continue;
                }

                if (other.getFacing() != facing) {
                    continue;
                }

                Location otherLocation = other.getLocation();

                if (!isSameFramePlane(facing, start, otherLocation)) {
                    continue;
                }

                if (isDirectNeighborFrame(facing, currentLocation, otherLocation)) {
                    open.add(other);
                }
            }
        }

        for (ItemFrame frame : toRemove) {
            frame.remove();
            removed++;
        }

        return removed;
    }

    private boolean isSameFramePlane(BlockFace facing, Location start, Location other) {
        if (facing == BlockFace.NORTH || facing == BlockFace.SOUTH) {
            return start.getBlockZ() == other.getBlockZ();
        }

        if (facing == BlockFace.EAST || facing == BlockFace.WEST) {
            return start.getBlockX() == other.getBlockX();
        }

        return false;
    }

    private boolean isDirectNeighborFrame(BlockFace facing, Location a, Location b) {
        int dx = Math.abs(a.getBlockX() - b.getBlockX());
        int dy = Math.abs(a.getBlockY() - b.getBlockY());
        int dz = Math.abs(a.getBlockZ() - b.getBlockZ());

        if (facing == BlockFace.NORTH || facing == BlockFace.SOUTH) {
            return dz == 0 && (
                    (dx == 1 && dy == 0) ||
                            (dx == 0 && dy == 1)
            );
        }

        if (facing == BlockFace.EAST || facing == BlockFace.WEST) {
            return dx == 0 && (
                    (dz == 1 && dy == 0) ||
                            (dz == 0 && dy == 1)
            );
        }

        return false;
    }

    private boolean handleWarnCommand(CommandSender sender, String[] args) {
        if (args.length == 2 && args[1].equalsIgnoreCase("list")) {
            sender.sendMessage(ChatColor.YELLOW + "Warnmeldungen:");

            for (WarnMessage warning : warningManager.getWarnings()) {
                sender.sendMessage(ChatColor.GRAY + "#" + warning.getId()
                        + " active=" + warning.isActive()
                        + " groups=" + String.join(", ", warning.getGroups())
                        + " message=" + warning.getMessage());
            }

            return true;
        }

        if (args.length == 3 && args[1].equalsIgnoreCase("enable")) {
            int id = Integer.parseInt(args[2]);

            if (warningManager.setActive(id, true)) {
                sender.sendMessage(ChatColor.GREEN + "Warnmeldung #" + id + " aktiviert.");
            } else {
                sender.sendMessage(ChatColor.RED + "Warnmeldung #" + id + " nicht gefunden.");
            }

            return true;
        }

        if (args.length == 3 && args[1].equalsIgnoreCase("disable")) {
            int id = Integer.parseInt(args[2]);

            if (warningManager.setActive(id, false)) {
                sender.sendMessage(ChatColor.GREEN + "Warnmeldung #" + id + " deaktiviert.");
            } else {
                sender.sendMessage(ChatColor.RED + "Warnmeldung #" + id + " nicht gefunden.");
            }

            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "/abfahrtstafel warn list");
        sender.sendMessage(ChatColor.YELLOW + "/abfahrtstafel warn enable <id>");
        sender.sendMessage(ChatColor.YELLOW + "/abfahrtstafel warn disable <id>");
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "/abfahrtstafel place <DisplayName> <normal|glow> <Station>");
        sender.sendMessage(ChatColor.YELLOW + "/abfahrtstafel place <DisplayName> <normal|glow> <Gleis> <Station>");
        sender.sendMessage(ChatColor.YELLOW + "/abfahrtstafel remove");
        sender.sendMessage(ChatColor.YELLOW + "/abfahrtstafel layouts");
        sender.sendMessage(ChatColor.YELLOW + "/abfahrtstafel debug <signactions|soundbox|fonts|clearstate|trigger>");
        sender.sendMessage(ChatColor.YELLOW + "/abfahrtstafel debug soundbox <Gleis> <Bahnhof>");
        sender.sendMessage(ChatColor.YELLOW + "/abfahrtstafel reload");
        sender.sendMessage(ChatColor.YELLOW + "/abfahrtstafel soundbox create <Gleis> <Bahnhof>");
        sender.sendMessage(ChatColor.YELLOW + "/abfahrtstafel soundbox list");
        sender.sendMessage(ChatColor.YELLOW + "/abfahrtstafel soundbox remove <id>");
        sender.sendMessage(ChatColor.YELLOW + "/abfahrtstafel warn list");
        sender.sendMessage(ChatColor.YELLOW + "/abfahrtstafel warn enable <id>");
        sender.sendMessage(ChatColor.YELLOW + "/abfahrtstafel warn disable <id>");
    }

    private void loadAnnouncementSoundCategory() {
        String categoryName = getConfig().getString("announcementSoundCategory", "MASTER");

        try {
            announcementSoundCategory = SoundCategory.valueOf(categoryName.toUpperCase());
        } catch (IllegalArgumentException e) {
            announcementSoundCategory = SoundCategory.MASTER;
            getLogger().warning("Ungültige announcementSoundCategory: " + categoryName + ". Verwende MASTER.");
        }
    }

    private boolean checkPermission(CommandSender sender, String permission) {
        if (sender.hasPermission("abfahrtstafel.admin")
                || sender.hasPermission(permission)) {
            return true;
        }

        sender.sendMessage(ChatColor.RED + "Keine Berechtigung.");
        return false;
    }

    private String joinArgs(String[] args, int startIndex) {
        StringBuilder builder = new StringBuilder();

        for (int i = startIndex; i < args.length; i++) {
            if (i > startIndex) {
                builder.append(" ");
            }

            builder.append(args[i]);
        }

        return builder.toString();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!command.getName().equalsIgnoreCase("abfahrtstafel")) {
            return completions;
        }

        if (args.length == 1) {
            completions.add("debug");
            completions.add("layouts");
            completions.add("place");
            completions.add("reload");
            completions.add("soundbox");
            completions.add("warn");
            completions.add("remove");
            return filterCompletions(completions, args[0]);
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("place")) {
                completions.addAll(displayLayoutManager.getLayoutNames());
                return filterCompletions(completions, args[1]);
            }

            if (args[0].equalsIgnoreCase("warn")) {
                completions.add("list");
                completions.add("enable");
                completions.add("disable");
                return filterCompletions(completions, args[1]);
            }

            if (args[0].equalsIgnoreCase("soundbox")) {
                completions.add("create");
                completions.add("list");
                completions.add("remove");
                return filterCompletions(completions, args[1]);
            }

            if (args[0].equalsIgnoreCase("debug")) {
                completions.add("signactions");
                completions.add("soundbox");
                completions.add("trigger");
                completions.add("fonts");
                completions.add("clearstate");
                return filterCompletions(completions, args[1]);
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("place")) {
            completions.add("normal");
            completions.add("glow");
            return filterCompletions(completions, args[2]);
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("place")) {
            DisplayLayout layout = displayLayoutManager.getLayout(args[1]);

            if (layout != null && layout.getDisplayType().equalsIgnoreCase("platform")) {
                completions.addAll(scheduleManager.getRailGroupNames());
                return filterCompletions(completions, args[3]);
            }

            if (layout != null && layout.getDisplayType().equalsIgnoreCase("station")) {
                String currentStationInput = joinArgs(args, 3);
                completions.addAll(scheduleManager.getStationNames());
                return filterCompletions(completions, currentStationInput);
            }
        }

        if (args.length >= 5 && args[0].equalsIgnoreCase("place")) {
            DisplayLayout layout = displayLayoutManager.getLayout(args[1]);

            if (layout != null && layout.getDisplayType().equalsIgnoreCase("platform")) {
                String currentStationInput = joinArgs(args, 4);
                completions.addAll(scheduleManager.getStationNames());
                return filterCompletions(completions, currentStationInput);
            }
        }

        if (args.length == 3
                && args[0].equalsIgnoreCase("warn")
                && (args[1].equalsIgnoreCase("enable") || args[1].equalsIgnoreCase("disable"))) {

            for (WarnMessage warning : warningManager.getWarnings()) {
                completions.add(String.valueOf(warning.getId()));
            }

            return filterCompletions(completions, args[2]);
        }

        if (args.length == 3
                && args[0].equalsIgnoreCase("debug")
                && args[1].equalsIgnoreCase("signactions")) {

            completions.add("true");
            completions.add("false");
            return filterCompletions(completions, args[2]);
        }

        if (args.length == 3
                && args[0].equalsIgnoreCase("debug")
                && args[1].equalsIgnoreCase("trigger")) {

            completions.addAll(buildStationRailCompletions());
            return filterCompletions(completions, args[2]);
        }

        if (args.length == 3
                && args[0].equalsIgnoreCase("debug")
                && args[1].equalsIgnoreCase("soundbox")) {

            completions.addAll(scheduleManager.getRailGroupNames());
            return filterCompletions(completions, args[2]);
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("debug")) {
                completions.add("signactions");
                completions.add("soundbox");
                completions.add("trigger");
                completions.add("clearstate");
                completions.add("fonts");
                return filterCompletions(completions, args[1]);
            }
        }

        if (args.length >= 4
                && args[0].equalsIgnoreCase("debug")
                && args[1].equalsIgnoreCase("soundbox")) {

            String currentStationInput = joinArgs(args, 3);
            completions.addAll(scheduleManager.getStationNames());
            return filterCompletions(completions, currentStationInput);
        }

        if (args.length == 3
                && args[0].equalsIgnoreCase("soundbox")
                && args[1].equalsIgnoreCase("create")) {

            completions.addAll(scheduleManager.getRailGroupNames());
            return filterCompletions(completions, args[2]);
        }

        if (args.length >= 4
                && args[0].equalsIgnoreCase("soundbox")
                && args[1].equalsIgnoreCase("create")) {

            String currentStationInput = joinArgs(args, 3);
            completions.addAll(scheduleManager.getStationNames());
            return filterCompletions(completions, currentStationInput);
        }

        return completions;
    }

    private List<String> buildStationRailCompletions() {
        List<String> result = new ArrayList<>();

        if (scheduleManager == null) {
            return result;
        }

        for (String station : scheduleManager.getStationNames()) {
            for (String railGroup : scheduleManager.getRailGroupNames()) {
                result.add(station + ":" + railGroup);
            }
        }

        return result;
    }

    private List<String> filterCompletions(List<String> completions, String input) {
        List<String> result = new ArrayList<>();

        for (String completion : completions) {
            if (completion.toLowerCase().startsWith(input.toLowerCase())) {
                result.add(completion);
            }
        }

        return result;
    }

    public StationAliasManager getStationAliasManager() {
        return stationAliasManager;
    }
}