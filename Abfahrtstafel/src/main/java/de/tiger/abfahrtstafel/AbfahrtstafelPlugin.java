package de.tiger.abfahrtstafel;

import com.bergerkiller.bukkit.common.map.MapDisplayProperties;
import com.bergerkiller.bukkit.tc.signactions.SignAction;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class AbfahrtstafelPlugin extends JavaPlugin {

    private static AbfahrtstafelPlugin instance;

    private ScheduleManager scheduleManager;
    private WarningManager warningManager;
    private RuntimeStateManager runtimeStateManager;

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

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("abfahrtstafel")) {
            return false;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            reloadConfig();
            scheduleManager.load();
            warningManager.load();
            sender.sendMessage(ChatColor.GREEN + "Dateien wurden neu geladen.");
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("clearstate")) {
            runtimeStateManager.clear();
            sender.sendMessage(ChatColor.GREEN + "Runtime-State wurde geleert.");
            return true;
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("trigger")) {
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
                sender.sendMessage(ChatColor.GREEN + "Naechste Abfahrt fuer " + station + ":" + railGroup + " wurde abgearbeitet.");
            } else {
                sender.sendMessage(ChatColor.RED + "Keine passende Abfahrt fuer " + station + ":" + railGroup + " gefunden.");
            }

            return true;
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("warn")) {
            return handleWarnCommand(sender, args);
        }

        if (args.length >= 3 && args[0].equalsIgnoreCase("give")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Dieser Befehl kann nur im Spiel benutzt werden.");
                return true;
            }

            String displayType = args[1].toLowerCase();

            if (displayType.equals("station")) {
                String station = joinArgs(args, 2);

                MapDisplayProperties properties = MapDisplayProperties.createNew(DepartureDisplay.class);
                properties.set("displayType", "station");
                properties.set("station", station);
                properties.setDisplayName(ChatColor.AQUA + "Abfahrtstafel: " + station);

                ItemStack item = properties.getMapItem();
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

                MapDisplayProperties properties = MapDisplayProperties.createNew(DepartureDisplay.class);
                properties.set("displayType", "platform");
                properties.set("station", station);
                properties.set("railGroup", railGroup);
                properties.setDisplayName(ChatColor.AQUA + "Gleisanzeige: " + station + ":" + railGroup);

                ItemStack item = properties.getMapItem();
                player.getInventory().addItem(item);

                player.sendMessage(ChatColor.GREEN + "Kleine Gleisanzeige erhalten für: " + station + ":" + railGroup);
                return true;
            }
        }

        sendHelp(sender);
        return true;
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
        sender.sendMessage(ChatColor.YELLOW + "/abfahrtstafel trigger <Station>:<Gleis>");
        sender.sendMessage(ChatColor.YELLOW + "/abfahrtstafel clearstate");
        sender.sendMessage(ChatColor.YELLOW + "/abfahrtstafel reload");
        sender.sendMessage(ChatColor.YELLOW + "/abfahrtstafel warn list");
        sender.sendMessage(ChatColor.YELLOW + "/abfahrtstafel warn enable <id>");
        sender.sendMessage(ChatColor.YELLOW + "/abfahrtstafel warn disable <id>");
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

    public int getStationLookAheadMinutes() {
        return getConfig().getInt("stationLookAheadMinutes", 120);
    }

    public int getPlatformLookAheadMinutes() {
        return getConfig().getInt("platformLookAheadMinutes", 180);
    }
}