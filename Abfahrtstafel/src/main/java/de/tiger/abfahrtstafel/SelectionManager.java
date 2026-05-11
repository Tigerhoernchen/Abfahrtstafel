package de.tiger.abfahrtstafel;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SelectionManager {

    private final Map<UUID, Location> pos1 = new HashMap<>();
    private final Map<UUID, Location> pos2 = new HashMap<>();

    public void setPos1(Player player, Location location) {
        pos1.put(player.getUniqueId(), location);
    }

    public void setPos2(Player player, Location location) {
        pos2.put(player.getUniqueId(), location);
    }

    public Location getPos1(Player player) {
        return pos1.get(player.getUniqueId());
    }

    public Location getPos2(Player player) {
        return pos2.get(player.getUniqueId());
    }

    public boolean hasSelection(Player player) {
        return getPos1(player) != null && getPos2(player) != null;
    }

    public void clear(Player player) {
        pos1.remove(player.getUniqueId());
        pos2.remove(player.getUniqueId());
    }
}