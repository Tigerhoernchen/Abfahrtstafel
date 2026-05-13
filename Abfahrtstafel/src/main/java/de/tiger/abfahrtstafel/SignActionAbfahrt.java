package de.tiger.abfahrtstafel;

import com.bergerkiller.bukkit.tc.events.SignActionEvent;
import com.bergerkiller.bukkit.tc.events.SignChangeActionEvent;
import com.bergerkiller.bukkit.tc.signactions.SignAction;
import com.bergerkiller.bukkit.tc.signactions.SignActionType;
import com.bergerkiller.bukkit.tc.utils.SignBuildOptions;

public class SignActionAbfahrt extends SignAction {

    @Override
    public boolean match(SignActionEvent info) {
        return info.isType("abfahrt");
    }

    @Override
    public void execute(SignActionEvent info) {
        if (!info.isTrainSign()) {
            return;
        }

        if (!info.isAction(SignActionType.GROUP_ENTER)) {
            return;
        }

        String stationAndRail = info.getLine(2);

        if (stationAndRail == null || !stationAndRail.contains(":")) {
            return;
        }

        String[] parts = stationAndRail.split(":", 2);
        String station = parts[0].trim();
        String railGroup = parts[1].trim();

        boolean success = AbfahrtstafelPlugin
                .getInstance()
                .getScheduleManager()
                .processNextDeparture(station, railGroup);

        if (AbfahrtstafelPlugin.getInstance().isDebugSignActions()) {
            if (success) {
                AbfahrtstafelPlugin.getInstance().getLogger().info(
                        "TrainCarts-Trigger: Abfahrt abgearbeitet für "
                                + station + ":" + railGroup
                );
            } else {
                AbfahrtstafelPlugin.getInstance().getLogger().info(
                        "TrainCarts-Trigger: Keine passende Abfahrt gefunden für "
                                + station + ":" + railGroup
                );
            }
        }
    }

    @Override
    public boolean build(SignChangeActionEvent event) {
        return SignBuildOptions.create()
                .setName("Abfahrtstafel Trigger")
                .setDescription("arbeitet die nächste Abfahrt für Station:Gleis ab")
                .handle(event.getPlayer());
    }
}