package dev.quickview;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 数据模型的基本行为：分组空值归一化、坐标读写、toString 只返回名称（搜索键不依赖它）。
 */
class ViewpointTest {

    @Test
    void groupDefaultsToEmptyStringNotUndefined() {
        Viewpoint vp = new Viewpoint("Home", "minecraft:overworld", 1, 2, 3, 10, 20);
        assertEquals("", vp.getGroup());
    }

    @Test
    void setNullGroupNormalisesToEmpty() {
        Viewpoint vp = new Viewpoint("Home", "minecraft:overworld", 1, 2, 3, 10, 20);
        vp.setGroup(null);
        assertEquals("", vp.getGroup());
        vp.setGroup("base");
        assertEquals("base", vp.getGroup());
        vp.setGroup(null);
        assertEquals("", vp.getGroup());
    }

    @Test
    void storesAllFields() {
        Viewpoint vp = new Viewpoint("Home", "minecraft:the_nether", 1.5, -2.5, 3.5, 45.0f, -30.0f);
        assertEquals("Home", vp.getName());
        assertEquals("minecraft:the_nether", vp.getDimension());
        assertEquals(1.5, vp.getX());
        assertEquals(-2.5, vp.getY());
        assertEquals(3.5, vp.getZ());
        assertEquals(45.0f, vp.getYaw());
        assertEquals(-30.0f, vp.getPitch());
        assertTrue(vp.getTimestamp() > 0, "timestamp should be set on construction");
    }

    @Test
    void toStringReturnsNameAndHandlesNull() {
        Viewpoint vp = new Viewpoint("Home", "minecraft:overworld", 0, 0, 0, 0, 0);
        assertEquals("Home", vp.toString());
        vp.setName(null);
        assertEquals("", vp.toString());
    }
}
