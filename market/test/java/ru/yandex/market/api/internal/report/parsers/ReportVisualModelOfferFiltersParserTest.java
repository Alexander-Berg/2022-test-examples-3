package ru.yandex.market.api.internal.report.parsers;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.api.category.FilterSubType;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.internal.report.data.ReportVisualModelOfferFilters;
import ru.yandex.market.api.util.ResourceHelpers;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
public class ReportVisualModelOfferFiltersParserTest extends UnitTestBase {

    @Test
    public void shouldParsePartialXml() {

        ReportVisualModelOfferFilters filters = new ReportVisualModelOfferFiltersParser().parse(
            ResourceHelpers.getResource("visual-model-offers-part.xml")
        );

        Assert.assertNotNull(filters);

        Assert.assertEquals("A4kTt2HjsmaEj-uEgxOhHQ", filters.getOfferId());

        Assert.assertNotNull(filters.getFilters());
        Assert.assertEquals(3, filters.getFilters().size());

        Assert.assertEquals(FilterSubType.COLOR, filters.getFilters().get(0).getSubType());
        Assert.assertEquals(FilterSubType.SIZE, filters.getFilters().get(1).getSubType());
        Assert.assertEquals(FilterSubType.SIZE, filters.getFilters().get(2).getSubType());
    }
}
