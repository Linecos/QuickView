package dev.quickview;

public class Viewpoint {
    private String name;
    private String dimension;
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;
    private long timestamp;

    public Viewpoint() {
    }

    public Viewpoint(String name, String dimension, double x, double y, double z, float yaw, float pitch) {
        this.name = name;
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.timestamp = System.currentTimeMillis();
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDimension() { return dimension; }
    public void setDimension(String dimension) { this.dimension = dimension; }
    public double getX() { return x; }
    public void setX(double x) { this.x = x; }
    public double getY() { return y; }
    public void setY(double y) { this.y = y; }
    public double getZ() { return z; }
    public void setZ(double z) { this.z = z; }
    public float getYaw() { return yaw; }
    public void setYaw(float yaw) { this.yaw = yaw; }
    public float getPitch() { return pitch; }
    public void setPitch(float pitch) { this.pitch = pitch; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getFormattedCoords() {
        return String.format("X: %.1f  Y: %.1f  Z: %.1f", x, y, z);
    }

    public String getFormattedRotation() {
        return String.format("Yaw: %.1f  Pitch: %.1f", yaw, pitch);
    }
}
