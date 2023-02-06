package ru.yandex.market.api.internal.report.parsers.json;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import ru.yandex.market.api.common.currency.CurrencyService;
import ru.yandex.market.api.model.ChildModel;
import ru.yandex.market.api.model.ChildModels;
import ru.yandex.market.api.model.Prices;
import ru.yandex.market.api.util.ResourceHelpers;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;

/**
 * Created by apershukov on 07.04.17.
 */
@RunWith(MockitoJUnitRunner.class)
public class ChildModelsJsonParserTest {

    @Mock
    private CurrencyService currencyService;

    @Test
    public void testParseAllModifications() {
        ChildModelsJsonParser parser = new ChildModelsJsonParser(true, currencyService);
        ChildModels models = parser.parse(ResourceHelpers.getResource("modifications.json"));

        assertNotNull(models);
        assertEquals(2, models.getModels().size());

        ChildModel model = models.getModels().get(0);

        assertEquals(14124642, model.getId());
        assertEquals("ASUS X540LJ", model.getName());
        assertEquals(61, model.getOffersCount());

        Prices prices = model.getPrices();
        assertNotNull(prices);
        assertEquals("31890", prices.getMin());
        assertEquals("43790", prices.getMax());
        assertEquals("35320", prices.getAvg());

        assertEquals(14159255, models.getModels().get(1).getId());

        verify(currencyService, Mockito.times(2)).doPriceConversions(any(), any(), any());
    }

    @Test
    public void testParseModificationsWithOffersOnly() {
        ChildModelsJsonParser parser = new ChildModelsJsonParser(false, currencyService);
        ChildModels models = parser.parse(ResourceHelpers.getResource("modifications.json"));

        assertEquals(1, models.getModels().size());
        assertEquals(14124642, models.getModels().get(0).getId());
    }

    @Test
    public void shouldNotOverflowOnBigOfferCount() {
        ChildModelsJsonParser parser = new ChildModelsJsonParser(false, currencyService);
        List<ChildModel> models = parser.parse(
            ResourceHelpers.getResource(
                "modifications-with-big-offer-count.json"
            )
        ).getModels();
        assertEquals(47244640286L, models.get(0).getOffersCount());
    }
}
