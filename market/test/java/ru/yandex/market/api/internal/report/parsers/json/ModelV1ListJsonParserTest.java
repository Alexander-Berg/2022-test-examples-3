package ru.yandex.market.api.internal.report.parsers.json;

import org.junit.Test;

import ru.yandex.market.api.common.currency.CurrencyService;
import ru.yandex.market.api.common.url.CommonMarketUrls;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.internal.report.parsers.json.filters.v1.FilterV1Factory;
import ru.yandex.market.api.model.AbstractModelV1;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;
import ru.yandex.market.api.util.ResourceHelpers;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * Created by apershukov on 14.02.17.
 */
@WithContext
public class ModelV1ListJsonParserTest extends BaseTest {
    @Inject
    private CommonMarketUrls commonMarketUrls;

    @Test
    public void testParseAdultModel() {
        CurrencyService currencyService = mock(CurrencyService.class);
        ModelV1ListJsonParser parser = new ModelV1ListJsonParser(
            1,
            Collections.emptyList(),
            currencyService,
            new FilterV1Factory(),
            commonMarketUrls
        );

        List<AbstractModelV1> models = parser.parse(ResourceHelpers.getResource("adult-model.json"));

        assertEquals(1, models.size());
        AbstractModelV1 model = models.get(0);
        assertEquals(1, (int) model.getAdult());
    }
}
