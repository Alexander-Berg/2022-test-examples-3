package ru.yandex.market.api.internal.report.parsers.json;

import org.junit.Test;
import ru.yandex.market.api.domain.v2.OfferFieldV2;
import ru.yandex.market.api.domain.v2.OfferPriceV2;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.util.ResourceHelpers;

import java.util.Collections;

import static org.junit.Assert.assertEquals;

/**
 * Created by fettsery on 21.05.19.
 */
public class OfferPriceV2JsonParserTest extends UnitTestBase {

    @Test
    public void testParsePrice() {
        OfferPriceV2 price = new OfferPriceV2JsonParser(Collections.singleton(OfferFieldV2.DISCOUNT))
            .parse(ResourceHelpers.getResource("offer-price.json"));
        assertEquals("975", price.getValue());

        assertEquals("1500", price.getBase());
        assertEquals("35", price.getDiscount());

        assertEquals("1300", price.getOldBase());
        assertEquals("13", price.getOldDiscount());
    }

    @Test
    public void testParseAbsolutePrice() {
        OfferPriceV2 price = new OfferPriceV2JsonParser(Collections.singleton(OfferFieldV2.DISCOUNT))
            .parse(ResourceHelpers.getResource("offer-price-with-absolute.json"));

        assertEquals("50", price.getDiscountAbsolute());
        assertEquals("55", price.getOldDiscountAbsolute());
    }
}
