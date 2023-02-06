package ru.yandex.market.fulfillment.stockstorage.service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.fulfillment.stockstorage.util.KorobyteChecker;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ItemReference;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Korobyte;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.UnitId;

import static org.junit.Assert.assertEquals;

public class KorobyteCheckerTest {
    private static final UnitId UNIT_ID = new UnitId("sku", 1L, "1");

    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"all fields defined", true, itemReference(1, 2, 3, BigDecimal.ONE)},
                {"korobyte is null", false, new ItemReference(UNIT_ID, null, 123, Collections.emptySet(), null)},
                {"width is zero", true, itemReference(0, 2, 3, BigDecimal.ONE)},
                {"width is null", false, itemReference(null, 2, 3, BigDecimal.ONE)},
                {"width is negative", false, itemReference(-1, 2, 3, BigDecimal.ONE)},
                {"height is zero", true, itemReference(1, 0, 3, BigDecimal.ONE)},
                {"height is null", false, itemReference(1, null, 3, BigDecimal.ONE)},
                {"height is negative", false, itemReference(1, -2, 3, BigDecimal.ONE)},
                {"length is zero", true, itemReference(1, 2, 0, BigDecimal.ONE)},
                {"length is null", false, itemReference(1, 2, null, BigDecimal.ONE)},
                {"length is negative", false, itemReference(1, 2, -3, BigDecimal.ONE)},
                {"weightGross is zero", true, itemReference(1, 2, 3, BigDecimal.ZERO)},
                {"weightGross is null", false, itemReference(1, 2, 3, null)},
                {"weightGross is negative", false, itemReference(1, 2, 3, BigDecimal.ONE.negate())}
        });
    }

    private static ItemReference itemReference(Integer width, Integer height, Integer length, BigDecimal weightGross) {
        Korobyte korobyte = new Korobyte(width, height, length, weightGross, new BigDecimal(0.0), new BigDecimal(0.0));
        return new ItemReference(UNIT_ID, korobyte, 123, Collections.emptySet(), null);
    }

    @ParameterizedTest
    @MethodSource("data")
    public void korobyteDefined(String message,
                                boolean defined,
                                ItemReference itemReference) {
        assertEquals("State: " + message, defined, KorobyteChecker.isKorobyteDefined(itemReference));
    }
}
