package dev.quickview;

public class Viewpoint {
    private String name;
    private String dimension;
    /** 分组名，空串表示未分组。旧存档没有这个字段，读出来是 null，getter 会归一化成空串。 */
    private String group = "";
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
    public String getGroup() { return group == null ? "" : group; }
    public void setGroup(String group) { this.group = group == null ? "" : group; }
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

    /**
     * 仅用于日志/调试，返回书签名称而非默认的对象哈希。
     * <p>
     * <b>列表搜索不要依赖这里</b>：搜索键由 {@code ViewpointListPanel} 的 searchKey 参数显式指定
     * （见 {@code ViewpointGUI} 里的 {@code Viewpoint::getName}）。曾把维度拼进来当作搜索键，
     * 结果维度名里的字母（如 {@code minecraft} 里的 a）会让单字母搜索误命中。
     */
    @Override
    public String toString() {
        return name == null ? "" : name;
    }
}
