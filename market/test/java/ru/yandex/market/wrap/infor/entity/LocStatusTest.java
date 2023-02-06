package ru.yandex.market.wrap.infor.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocStatusTest {

    @Test
    void parseDamageType() {
        LocStatus expected = LocStatus.HOLD;
        LocStatus actual = LocStatus.of("HOLD");

        assertEquals(expected, actual);
    }

    @Test
    void parseStageType() {
        LocStatus expected = LocStatus.OK;
        LocStatus actual = LocStatus.of("OK");

        assertEquals(expected, actual);
    }

    @Test
    void parseTypeAsUnknown() {
        LocStatus expected = LocStatus.UNKNOWN;
        LocStatus actual = LocStatus.of("abcde");

        assertEquals(expected, actual);

        actual = LocStatus.of("unknown");
        assertEquals(expected, actual);

        actual = LocStatus.of("sstage");
        assertEquals(expected, actual);
    }


}
