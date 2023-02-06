package ru.yandex.market.api.internal.loyalty.parser;

import org.junit.Test;
import ru.yandex.market.api.domain.v2.DiscountType;
import ru.yandex.market.api.internal.loyalty.ThresholdResponse;
import ru.yandex.market.api.util.ResourceHelpers;

import java.math.BigDecimal;

import static org.junit.Assert.*;

/**
 * Created by fettsery on 01.02.19.
 */
public class ThresholdResponseParserTest {
    @Test
    public void shouldParseThresholdResponse() {
        ThresholdResponse result = new ThresholdResponseParser().parse(ResourceHelpers.getResource("threshold_response.json"));

        assertEquals(new BigDecimal("10.1"), result.getPriceLeftForFreeDelivery());
        assertEquals(new BigDecimal("1000.1"), result.getThreshold());
        assertEquals(DiscountType.YANDEX_PLUS, result.getPromo());
    }

}
