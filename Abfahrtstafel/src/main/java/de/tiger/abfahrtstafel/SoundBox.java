package de.tiger.abfahrtstafel;

public class SoundBox {

    private final int id;
    private final String station;
    private final String railGroup;
    private final String world;
    private final double x;
    private final double y;
    private final double z;
    private final float radius;
    private final float volume;
    private final float pitch;

    public SoundBox(int id,
                    String station,
                    String railGroup,
                    String world,
                    double x,
                    double y,
                    double z,
                    float radius,
                    float volume,
                    float pitch) {
        this.id = id;
        this.station = station;
        this.railGroup = railGroup;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.radius = radius;
        this.volume = volume;
        this.pitch = pitch;
    }

    public int getId() {
        return id;
    }

    public String getStation() {
        return station;
    }

    public String getRailGroup() {
        return railGroup;
    }

    public String getWorld() {
        return world;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public float getRadius() {
        return radius;
    }

    public float getVolume() {
        return volume;
    }

    public float getPitch() {
        return pitch;
    }
}