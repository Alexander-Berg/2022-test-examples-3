package ru.yandex.market.ff.client;

import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.gateway.common.model.common.NonconformityType;

class NonconformityTypeTest {

    @Test
    public void testAllEnumsAreMapped() {
        Assertions.assertAll(
                Arrays.stream(NonconformityType.values()).map(cType ->
                        () -> Assertions.assertNotEquals(ru.yandex.market.ff.client.enums.NonconformityType.DEFAULT,
                                ru.yandex.market.ff.client.enums.NonconformityType.findByValue(cType.getName()),
                                () -> String.format("FFWF has no corresponding enum for the one from LGW: %s",
                                        cType.getName())
                        )));
    }
}
