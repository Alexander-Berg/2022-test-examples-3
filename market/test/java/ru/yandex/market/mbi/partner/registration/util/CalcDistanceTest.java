package ru.yandex.market.mbi.partner.registration.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.mbi.partner.registration.util.DistanceCalculator.distance;

public class CalcDistanceTest {
    @Test
    public void calcDistance() {
        assertEquals(distance(60.045655, 30.448406, 60.05194, 30.364081), 4733.215888261437);
    }
}
