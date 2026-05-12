package de.tiger.abfahrtstafel;

import com.bergerkiller.bukkit.common.map.MapDisplayProperties;
import com.bergerkiller.bukkit.tc.signactions.SignAction;
import org.bukkit.ChatColor;
import org.bukkit.Location;
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
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

public class AbfahrtstafelPlugin extends JavaPlugin {

    private static AbfahrtstafelPlugin instance;

    private ScheduleManager scheduleManager;
    private WarningManager warningManager;
    private RuntimeStateManager runtimeStateManager;
    private SelectionManager selectionManager;

    private final SignActionAbfahrt signActionAbfahrt = new SignActionAbfahrt();

    @Override
    public void onLoad() {
        SignAction.register(signActionAbfahrt);
    }

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        reloadConfig();

        runtimeStateManager = new RuntimeStateManager();
        selectionManager = new SelectionManager();

        scheduleManager = new ScheduleManager(this);
        scheduleManager.load();

        warningManager = new WarningManager(this);
        warningManager.load();

        getLogger().info("Abfahrtstafel Plugin gestartet!");
        getLogger().info("BKCommonLib und TrainCarts wurden gefunden!");
    }

    @Override
    public void onDisable() {
        SignAction.unregister(signActionAbfahrt);
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

    public int getWarningScrollSpeedTicks() {
        return getConfig().getInt("warningScrollSpeedTicks", 5);
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
            scheduleManager.load();
            warningManager.load();
            sender.sendMessage(ChatColor.GREEN + "Dateien wurden neu geladen.");
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

        if (args.length == 1 && args[0].equalsIgnoreCase("pos1")) {
            if (!checkPermission(sender, "abfahrtstafel.admin")) {
                return true;
            }

            if (!(sender instanceof Player player)) {
                sender.sendMessage("Dieser Befehl kann nur im Spiel benutzt werden.");
                return true;
            }

            Block block = player.getTargetBlockExact(10);

            if (block == null) {
                sender.sendMessage(ChatColor.RED + "Bitte schaue einen Block an.");
                return true;
            }

            Location frameLocation = block.getLocation()
                    .add(getOppositeCardinalFacing(player).getModX(),
                            getOppositeCardinalFacing(player).getModY(),
                            getOppositeCardinalFacing(player).getModZ());

            selectionManager.setPos1(player, frameLocation);
            sender.sendMessage(ChatColor.GREEN + "Position 1 gesetzt: "
                    + formatLocation(frameLocation));
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("pos2")) {
            if (!checkPermission(sender, "abfahrtstafel.admin")) {
                return true;
            }

            if (!(sender instanceof Player player)) {
                sender.sendMessage("Dieser Befehl kann nur im Spiel benutzt werden.");
                return true;
            }

            Block block = player.getTargetBlockExact(10);

            if (block == null) {
                sender.sendMessage(ChatColor.RED + "Bitte schaue einen Block an.");
                return true;
            }

            Location frameLocation = block.getLocation()
                    .add(getOppositeCardinalFacing(player).getModX(),
                            getOppositeCardinalFacing(player).getModY(),
                            getOppositeCardinalFacing(player).getModZ());

            selectionManager.setPos2(player, frameLocation);
            sender.sendMessage(ChatColor.GREEN + "Position 2 gesetzt: "
                    + formatLocation(frameLocation));
            return true;
        }

        if (args.length >= 4 && args[0].equalsIgnoreCase("place")) {
            if (!checkPermission(sender, "abfahrtstafel.admin")) {
                return true;
            }

            if (!(sender instanceof Player player)) {
                sender.sendMessage("Dieser Befehl kann nur im Spiel benutzt werden.");
                return true;
            }

            if (!selectionManager.hasSelection(player)) {
                sender.sendMessage(ChatColor.RED + "Bitte zuerst /abfahrtstafel pos1 und /abfahrtstafel pos2 setzen.");
                return true;
            }

            String displayType = args[1].toLowerCase();
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

            if (displayType.equals("station")) {
                String station = joinArgs(args, 3);

                ItemStack item = createDisplayItem("station", station, null);
                int placed = placeFrames(player, item, glow);

                sender.sendMessage(ChatColor.GREEN + "Bahnhofsanzeige platziert: "
                        + station + " (" + placed + " Rahmen)");
                return true;
            }

            if (displayType.equals("platform")) {
                if (args.length < 5) {
                    sender.sendMessage(ChatColor.RED + "Nutzung: /abfahrtstafel place platform <normal|glow> <Gleis> <Stations Name>");
                    return true;
                }

                String railGroup = args[3];
                String station = joinArgs(args, 4);

                ItemStack item = createDisplayItem("platform", station, railGroup);
                int placed = placeFrames(player, item, glow);

                sender.sendMessage(ChatColor.GREEN + "Gleisanzeige platziert: "
                        + station + ":" + railGroup + " (" + placed + " Rahmen)");
                return true;
            }

            sender.sendMessage(ChatColor.RED + "Nutzung:");
            sender.sendMessage(ChatColor.YELLOW + "/abfahrtstafel place station <normal|glow> <Stations Name>");
            sender.sendMessage(ChatColor.YELLOW + "/abfahrtstafel place platform <normal|glow> <Gleis> <Stations Name>");
            return true;
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

        if (args.length >= 3 && args[0].equalsIgnoreCase("give")) {
            if (!checkPermission(sender, "abfahrtstafel.admin")) {
                return true;
            }

            if (!(sender instanceof Player player)) {
                sender.sendMessage("Dieser Befehl kann nur im Spiel benutzt werden.");
                return true;
            }

            String displayType = args[1].toLowerCase();

            if (displayType.equals("station")) {
                String station = joinArgs(args, 2);

                ItemStack item = createDisplayItem("station", station, null);
                player.getInventory().addItem(item);

                player.sendMessage(ChatColor.GREEN + "Große Bahnhofsanzeige erhalten für Station: " + station);
                return true;
            }

            if (displayType.equals("platform")) {
                if (args.length < 4) {
                    sender.sendMessage(ChatColor.RED + "Nutzung: /abfahrtstafel give platform <Gleis> <Stations Name>");
                    return true;
                }

                String railGroup = args[2];
                String station = joinArgs(args, 3);

                ItemStack item = createDisplayItem("platform", station, railGroup);
                player.getInventory().addItem(item);

                player.sendMessage(ChatColor.GREEN + "Kleine Gleisanzeige erhalten für: " + station + ":" + railGroup);
                return true;
            }
        }

        sendHelp(sender);
        return true;
    }

    private ItemStack createDisplayItem(String displayType, String station, String railGroup) {
        MapDisplayProperties properties = MapDisplayProperties.createNew(DepartureDisplay.class);
        properties.set("displayType", displayType);
        properties.set("station", station);

        if (railGroup != null) {
            properties.set("railGroup", railGroup);
        }

        return properties.getMapItem();
    }

    private int placeFrames(Player player, ItemStack item, boolean glow) {
        Location pos1 = selectionManager.getPos1(player);
        Location pos2 = selectionManager.getPos2(player);

        if (pos1 == null || pos2 == null || pos1.getWorld() == null || pos2.getWorld() == null) {
            return 0;
        }

        if (!pos1.getWorld().equals(pos2.getWorld())) {
            player.sendMessage(ChatColor.RED + "Positionen müssen in derselben Welt liegen.");
            return 0;
        }

        World world = pos1.getWorld();

        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        boolean flatX = minX == maxX;
        boolean flatY = minY == maxY;
        boolean flatZ = minZ == maxZ;

        if (!flatX && !flatZ) {
            player.sendMessage(ChatColor.RED + "Die Auswahl muss eine flache Wandfläche oder eine Linie an einer Wand sein.");
            return 0;
        }

        BlockFace facing = getOppositeCardinalFacing(player);

        // Bereits vorhandene ItemFrames in dieser Fläche -> Platzieren abbrechen
        if (hasAnyItemFrameInArea(world, minX, maxX, minY, maxY, minZ, maxZ)) {
            player.sendMessage(ChatColor.RED + "In der ausgewählten Fläche befindet sich bereits ein Display. Platzieren abgebrochen.");
            return 0;
        }

        int placed = 0;
        int skipped = 0;

        for (int y = maxY; y >= minY; y--) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Location location = new Location(world, x + 0.5, y + 0.5, z + 0.5);

                    // Nur wenn dort noch kein ItemFrame hängt, störende Blöcke entfernen
                    Block block = world.getBlockAt(x, y, z);

                    if (!block.getType().isAir()) {
                        block.setType(Material.AIR);
                    }

                    ItemFrame frame;

                    if (glow) {
                        frame = (GlowItemFrame) world.spawnEntity(location, EntityType.GLOW_ITEM_FRAME);
                    } else {
                        frame = (ItemFrame) world.spawnEntity(location, EntityType.ITEM_FRAME);
                    }

                    frame.setFacingDirection(facing, true);
                    frame.setItem(item.clone(), false);
                    frame.setFixed(true);
                    frame.setVisible(true);

                    placed++;
                }
            }
        }

        if (skipped > 0) {
            player.sendMessage(ChatColor.YELLOW + "Übersprungen wegen vorhandener Rahmen: " + skipped);
        }

        return placed;
    }

    private boolean hasAnyItemFrameInArea(World world, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        for (ItemFrame frame : world.getEntitiesByClass(ItemFrame.class)) {
            Location location = frame.getLocation();

            int x = location.getBlockX();
            int y = location.getBlockY();
            int z = location.getBlockZ();

            if (x >= minX && x <= maxX
                    && y >= minY && y <= maxY
                    && z >= minZ && z <= maxZ) {
                return true;
            }
        }

        return false;
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

    private String formatLocation(Location location) {
        return location.getWorld().getName()
                + " "
                + location.getBlockX()
                + " "
                + location.getBlockY()
                + " "
                + location.getBlockZ();
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
        sender.sendMessage(ChatColor.YELLOW + "/abfahrtstafel give station <Stations Name>");
        sender.sendMessage(ChatColor.YELLOW + "/abfahrtstafel give platform <Gleis> <Stations Name>");
        sender.sendMessage(ChatColor.YELLOW + "/abfahrtstafel place station <normal|glow> <Stations Name>");
        sender.sendMessage(ChatColor.YELLOW + "/abfahrtstafel place platform <normal|glow> <Gleis> <Stations Name>");
        sender.sendMessage(ChatColor.YELLOW + "/abfahrtstafel pos1");
        sender.sendMessage(ChatColor.YELLOW + "/abfahrtstafel pos2");
        sender.sendMessage(ChatColor.YELLOW + "/abfahrtstafel remove");
        sender.sendMessage(ChatColor.YELLOW + "/abfahrtstafel trigger <Station>:<Gleis>");
        sender.sendMessage(ChatColor.YELLOW + "/abfahrtstafel clearstate");
        sender.sendMessage(ChatColor.YELLOW + "/abfahrtstafel reload");
        sender.sendMessage(ChatColor.YELLOW + "/abfahrtstafel warn list");
        sender.sendMessage(ChatColor.YELLOW + "/abfahrtstafel warn enable <id>");
        sender.sendMessage(ChatColor.YELLOW + "/abfahrtstafel warn disable <id>");
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
            completions.add("give");
            completions.add("place");
            completions.add("pos1");
            completions.add("pos2");
            completions.add("remove");
            completions.add("reload");
            completions.add("clearstate");
            completions.add("trigger");
            completions.add("warn");
            return filterCompletions(completions, args[0]);
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("place")) {
                completions.add("station");
                completions.add("platform");
                return filterCompletions(completions, args[1]);
            }

            if (args[0].equalsIgnoreCase("warn")) {
                completions.add("list");
                completions.add("enable");
                completions.add("disable");
                return filterCompletions(completions, args[1]);
            }

            if (args[0].equalsIgnoreCase("trigger")) {
                completions.addAll(buildStationRailCompletions());
                return filterCompletions(completions, args[1]);
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("place")) {
            completions.add("normal");
            completions.add("glow");
            return filterCompletions(completions, args[2]);
        }

        if (args.length >= 3
                && args[0].equalsIgnoreCase("give")
                && args[1].equalsIgnoreCase("station")) {

            String currentStationInput = joinArgs(args, 2);
            completions.addAll(scheduleManager.getStationNames());
            return filterCompletions(completions, currentStationInput);
        }

        if (args.length == 3
                && args[0].equalsIgnoreCase("give")
                && args[1].equalsIgnoreCase("platform")) {

            completions.addAll(scheduleManager.getRailGroupNames());
            return filterCompletions(completions, args[2]);
        }

        if (args.length >= 4
                && args[0].equalsIgnoreCase("give")
                && args[1].equalsIgnoreCase("platform")) {

            String currentStationInput = joinArgs(args, 3);
            completions.addAll(scheduleManager.getStationNames());
            return filterCompletions(completions, currentStationInput);
        }

        if (args.length >= 4
                && args[0].equalsIgnoreCase("place")
                && args[1].equalsIgnoreCase("station")) {

            String currentStationInput = joinArgs(args, 3);
            completions.addAll(scheduleManager.getStationNames());
            return filterCompletions(completions, currentStationInput);
        }

        if (args.length == 4
                && args[0].equalsIgnoreCase("place")
                && args[1].equalsIgnoreCase("platform")) {

            completions.addAll(scheduleManager.getRailGroupNames());
            return filterCompletions(completions, args[3]);
        }

        if (args.length >= 5
                && args[0].equalsIgnoreCase("place")
                && args[1].equalsIgnoreCase("platform")) {

            String currentStationInput = joinArgs(args, 4);
            completions.addAll(scheduleManager.getStationNames());
            return filterCompletions(completions, currentStationInput);
        }

        if (args.length == 3
                && args[0].equalsIgnoreCase("warn")
                && (args[1].equalsIgnoreCase("enable") || args[1].equalsIgnoreCase("disable"))) {

            for (WarnMessage warning : warningManager.getWarnings()) {
                completions.add(String.valueOf(warning.getId()));
            }

            return filterCompletions(completions, args[2]);
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
}