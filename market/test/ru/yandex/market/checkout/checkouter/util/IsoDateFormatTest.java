package ru.yandex.market.checkout.checkouter.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class IsoDateFormatTest {

    @Test
    public void testCreateIsoDateFormat() {
        Assertions.assertEquals(
                "2018-03-30T20:55:07+03:00",
                IsoDateFormat.createIsoDateFormat().format(1522432507966L)
        );
    }
}
