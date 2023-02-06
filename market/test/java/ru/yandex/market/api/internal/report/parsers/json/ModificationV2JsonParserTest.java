package ru.yandex.market.api.internal.report.parsers.json;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.api.common.currency.CurrencyService;
import ru.yandex.market.api.domain.v2.ModelPriceV2;
import ru.yandex.market.api.domain.v2.Modification;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.util.ResourceHelpers;

import java.util.Collections;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;

/**
 * Created by apershukov on 07.04.17.
 */
public class ModificationV2JsonParserTest extends UnitTestBase {

    private CurrencyService currencyService;
    private ModificationV2JsonParser parser;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        currencyService = mock(CurrencyService.class);
        parser = new ModificationV2JsonParser(Collections.emptyList(), currencyService);
    }

    @Test
    public void testParseModification() {
        Modification modification = parser.parse(ResourceHelpers.getResource("modification.json"));

        assertNotNull(modification);
        assertEquals(14124642, modification.getId());
        assertEquals("ASUS X540LJ", modification.getName());
        assertEquals("ноутбук, 1.9 кг", modification.getDescription());
        assertEquals(61, modification.getOfferCount());
        assertEquals(59, modification.getShopCount());
        assertEquals("90.4", modification.getPopularity());

        ModelPriceV2 price = (ModelPriceV2) modification.getPrice();
        assertNotNull(price);
        assertEquals("31890", price.getMin());
        assertEquals("43790", price.getMax());
        assertEquals("35320", price.getAvg());

        verify(currencyService, only()).doPriceConversions(any(), any(), any());
    }

    @Test
    public void testParseProductsOnly() {
        assertNull(parser.parse(ResourceHelpers.getResource("offer.json")));
    }

    @Test
    public void shouldNotOverflowOnBigOfferCount() {
        Modification modification = parser.parse(
            ResourceHelpers.getResource("modification-with-big-offer-count.json")
        );
        assertEquals(47244640286L, modification.getOfferCount());
    }

}
