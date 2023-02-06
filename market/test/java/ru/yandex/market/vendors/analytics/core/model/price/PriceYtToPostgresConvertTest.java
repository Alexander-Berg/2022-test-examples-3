package ru.yandex.market.vendors.analytics.core.model.price;


import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

/**
 * Тесты для методов конвертации {@link CategoryPriceSegments} в {@link PriceSegmentInfo}.
 *
 * @author ogonek
 */
public class PriceYtToPostgresConvertTest {

    private static CategoryPriceSegments categoryPriceSegments =
            new CategoryPriceSegments(1L,
                    100L,
                    200L,
                    300L,
                    400L,
                    500L,
                    600L,
                    700L
            );

    @Test
    void getLowerBoundTest() {
        Assertions.assertEquals(0, categoryPriceSegments.getLowerBound(1));
        Assertions.assertEquals(100, categoryPriceSegments.getLowerBound(2));
        Assertions.assertEquals(700, categoryPriceSegments.getLowerBound(8));
        assertThroesUnsupportedOperation(() -> categoryPriceSegments.getLowerBound(0));
        assertThroesUnsupportedOperation(() -> categoryPriceSegments.getLowerBound(9));
    }

    @Test
    void getHigherBoundTest() {
        Assertions.assertEquals(99, categoryPriceSegments.getModifiedHigherBound(1).longValue());
        Assertions.assertEquals(199, categoryPriceSegments.getModifiedHigherBound(2).longValue());
        Assertions.assertNull(categoryPriceSegments.getModifiedHigherBound(8));
        assertThroesUnsupportedOperation(() -> categoryPriceSegments.getModifiedHigherBound(0));
        assertThroesUnsupportedOperation(() -> categoryPriceSegments.getModifiedHigherBound(9));
    }

    @Test
    void toPriceSegmentsTest() {
        List<PriceSegmentInfo> expected = List.of(
                new PriceSegmentInfo(1, 1, 0, 99L),
                new PriceSegmentInfo(1, 2, 100, 199L),
                new PriceSegmentInfo(1, 3, 200, 299L),
                new PriceSegmentInfo(1, 4, 300, 399L),
                new PriceSegmentInfo(1, 5, 400, 499L),
                new PriceSegmentInfo(1, 6, 500, 599L),
                new PriceSegmentInfo(1, 7, 600, 699L),
                new PriceSegmentInfo(1, 8, 700, null)
        );
        Assertions.assertEquals(expected, categoryPriceSegments.toPriceSegments());
    }

    private void assertThroesUnsupportedOperation(Executable executable) {
        Assertions.assertThrows(
                IndexOutOfBoundsException.class,
                executable
        );
    }

}
