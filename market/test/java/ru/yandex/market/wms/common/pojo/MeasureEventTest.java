package ru.yandex.market.wms.common.pojo;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MeasureEventTest {

    private final MeasureEvent measureEvent = new MeasureEvent(
            "clerk",
            "test_warehouse",
            "123",
            "storer1",
            "abc",
            new Dimensions.DimensionsBuilder()
                    .length(BigDecimal.valueOf(1))
                    .width(BigDecimal.valueOf(2))
                    .height(BigDecimal.valueOf(3))
                    .weight(BigDecimal.valueOf(2.5))
                    .build(),
            new Dimensions.DimensionsBuilder()
                    .length(BigDecimal.valueOf(4))
                    .width(BigDecimal.valueOf(5))
                    .height(BigDecimal.valueOf(6))
                    .weight(BigDecimal.valueOf(6.5))
                    .build(),
            60
    );

    @Test
    void asFlatMap() {
        Map<String, String> expected = new HashMap<>();
        expected.put("timestamp", Long.toString(measureEvent.getTimestamp()));
        expected.put("userName", "clerk");
        expected.put("warehouseId", "test_warehouse");
        expected.put("sku", "123");
        expected.put("storer", "storer1");
        expected.put("manufacturerSku", "abc");
        expected.put("length", "1");
        expected.put("width", "2");
        expected.put("height", "3");
        expected.put("weight", "2.5");
        expected.put("cube", null);
        expected.put("previous_length", "4");
        expected.put("previous_width", "5");
        expected.put("previous_height", "6");
        expected.put("previous_weight", "6.5");
        expected.put("previous_cube", null);
        expected.put("durations", "60");

        Map<String, String> actual = measureEvent.asFlatMap();
        assertThat(actual, aMapWithSize(17));
        assertEquals(expected, actual);
    }
}
