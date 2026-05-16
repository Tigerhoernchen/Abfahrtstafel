package de.tiger.abfahrtstafel;

import com.bergerkiller.bukkit.tc.events.SignActionEvent;
import com.bergerkiller.bukkit.tc.events.SignChangeActionEvent;
import com.bergerkiller.bukkit.tc.signactions.SignAction;
import com.bergerkiller.bukkit.tc.signactions.SignActionType;
import com.bergerkiller.bukkit.tc.utils.SignBuildOptions;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class SignActionAnkunft extends SignAction {

    @Override
    public boolean match(SignActionEvent info) {
        return info.isType("ankunft");
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

        ScheduleManager.ArrivalCandidate candidate = AbfahrtstafelPlugin
                .getInstance()
                .getScheduleManager()
                .findNextArrivalCandidate(station, railGroup);

        if (candidate == null) {
            if (AbfahrtstafelPlugin.getInstance().isDebugSignActions()) {
                AbfahrtstafelPlugin.getInstance().getLogger().info(
                        "TrainCarts-Trigger: Keine passende Ankunft gefunden für "
                                + station + ":" + railGroup
                );
            }

            return;
        }

        if (!candidate.orderedRailGroup().isFinalStop()) {
            AbfahrtstafelPlugin
                    .getInstance()
                    .getRuntimeStateManager()
                    .setArrival(
                            candidate.station(),
                            candidate.railGroup(),
                            candidate.line(),
                            candidate.departureTime()
                    );
        }

        String platformSound = candidate
                .orderedRailGroup()
                .getArrivalPlatformSound();

        String trainSound = candidate
                .orderedRailGroup()
                .getArrivalTrainSound();

        AbfahrtstafelPlugin
                .getInstance()
                .getSoundManager()
                .playPlatformSound(
                        candidate.station(),
                        candidate.railGroup(),
                        platformSound
                );

        AbfahrtstafelPlugin
                .getInstance()
                .getSoundManager()
                .playTrainSound(
                        info.getGroup(),
                        trainSound
                );

        if (AbfahrtstafelPlugin.getInstance().isDebugSignActions()) {
            AbfahrtstafelPlugin.getInstance().getLogger().info(
                    "TrainCarts-Trigger: Ankunft gesetzt für "
                            + candidate.station()
                            + ":"
                            + candidate.railGroup()
                            + " "
                            + candidate.line()
                            + " "
                            + candidate.departureTime()
            );
        }
    }

    @Override
    public boolean build(SignChangeActionEvent event) {
        return SignBuildOptions.create()
                .setName("Abfahrtstafel Ankunft Trigger")
                .setDescription("setzt on_arrival für Station:Gleis")
                .handle(event.getPlayer());
    }

    private LocalDateTime toTodayOrTomorrow(LocalTime time) {
        LocalDate today = LocalDate.now();
        LocalDateTime dateTime = LocalDateTime.of(today, time);

        if (dateTime.isBefore(LocalDateTime.now().minusHours(12))) {
            dateTime = dateTime.plusDays(1);
        }

        return dateTime;
    }
}