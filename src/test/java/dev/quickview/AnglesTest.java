package dev.quickview;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 最短角度差的回归测试。返回值应始终落在 [-180, 180)，转身时走最近的一侧。
 */
class AnglesTest {

    private static final float EPS = 1.0e-4f;

    @Test
    void simpleDeltas() {
        assertEquals(0.0f, Angles.shortestDelta(0.0f, 0.0f), EPS);
        assertEquals(90.0f, Angles.shortestDelta(0.0f, 90.0f), EPS);
        assertEquals(-90.0f, Angles.shortestDelta(90.0f, 0.0f), EPS);
    }

    @Test
    void wrapsAroundTheShortWay() {
        assertEquals(20.0f, Angles.shortestDelta(170.0f, -170.0f), EPS);  // 不是 -340
        assertEquals(-20.0f, Angles.shortestDelta(-170.0f, 170.0f), EPS); // 不是 +340
        assertEquals(20.0f, Angles.shortestDelta(350.0f, 10.0f), EPS);
        assertEquals(-20.0f, Angles.shortestDelta(10.0f, 350.0f), EPS);
    }

    @Test
    void halfTurnFoldsToNegative() {
        assertEquals(-180.0f, Angles.shortestDelta(0.0f, 180.0f), EPS);
        assertEquals(-180.0f, Angles.shortestDelta(0.0f, -180.0f), EPS);
    }

    @Test
    void hugeAnglesStillNormalise() {
        assertEquals(0.0f, Angles.shortestDelta(0.0f, 3600.0f), EPS);
        assertEquals(90.0f, Angles.shortestDelta(0.0f, 810.0f), EPS);
    }

    @Test
    void alwaysWithinHalfTurnRange() {
        for (float from = -720.0f; from <= 720.0f; from += 37.0f) {
            for (float to = -720.0f; to <= 720.0f; to += 53.0f) {
                float d = Angles.shortestDelta(from, to);
                assertTrue(d >= -180.0f && d < 180.0f, "out of range: " + d + " (" + from + " -> " + to + ")");
            }
        }
    }
}
