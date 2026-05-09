package de.tiger.abfahrtstafel;

public class Departure {

    private final String time;
    private final String line;
    private final String destination;
    private final String via;
    private final String platform;
    private final long delayMinutes;

    public Departure(String time,
                     String line,
                     String destination,
                     String via,
                     String platform,
                     long delayMinutes) {
        this.time = time;
        this.line = line;
        this.destination = destination;
        this.via = via;
        this.platform = platform;
        this.delayMinutes = delayMinutes;
    }

    public long getDelayMinutes() {
        return delayMinutes;
    }

    public String getTime() {
        return time;
    }

    public String getLine() {
        return line;
    }

    public String getDestination() {
        return destination;
    }

    public String getVia() {
        return via;
    }

    public String getPlatform() {
        return platform;
    }
}