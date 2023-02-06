package ru.yandex.market.core.backa.persist.addr;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author Victor Nazarov &lt;sviperll@yandex-team.ru&gt;
 */
class CoordinatesTest {

    private static final String[] SAMPLES = new String[]{
            "1.2,3.4", "1.2 3.4", "1.2   3.4", "1.2, 3.4", "1.2 , 3.4", "1.2   ,   3.4"
    };

    private static final String[] NULL_VALUES = new String[]{
            null, "", " ", ",", "1,", ",2"
    };


    @Test
    void testCoordinates1() {
        for (final String s : SAMPLES) {
            Coordinates coords = Coordinates.valueOf(s);
            assertEquals(1.2, coords.getLon(), 0.001);
            assertEquals(3.4, coords.getLat(), 0.001);
        }
    }

    @Test
    void testIsValid() {
        for (final String value : SAMPLES) {
            assertThat(Coordinates.isValid(value), equalTo(true));
        }

        for (final String value : NULL_VALUES) {
            assertThat(Coordinates.isValid(value), equalTo(false));
        }
    }

}
