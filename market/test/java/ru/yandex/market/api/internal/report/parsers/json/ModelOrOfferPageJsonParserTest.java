package ru.yandex.market.api.internal.report.parsers.json;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.api.domain.ModelOrOfferV2;
import ru.yandex.market.api.domain.PageInfo;
import ru.yandex.market.api.domain.PagedResultWithOptions;
import ru.yandex.market.api.domain.v2.option.AvailableReportSort;
import ru.yandex.market.api.domain.v2.option.AvailableReportSort.Option;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.internal.report.ReportSortType;
import ru.yandex.market.api.internal.report.SortOrder;
import ru.yandex.market.api.util.ResourceHelpers;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

/**
 *
 * Created by apershukov on 16.09.16.
 */
public class ModelOrOfferPageJsonParserTest extends UnitTestBase {

    private ModelOrOfferPageJsonParser parser;

    @Before
    public void setUp() {
        parser = new ModelOrOfferPageJsonParser(
            null,
            null,
            null,
            PageInfo.DEFAULT,
            false
        );
    }

    @Test
    public void testParseAvailibleSorts() {
        PagedResultWithOptions<ModelOrOfferV2> result = parser.parse(ResourceHelpers.getResource("search-result.json"));

        List<AvailableReportSort> sorts = result.getSorts();
        assertEquals(3, sorts.size());

        assertSort(sorts.get(0), "по умолчанию", null, Collections.emptyList());
        assertSort(sorts.get(1), "по цене", ReportSortType.PRICE, asList(new Option("aprice", SortOrder.ASC, null),
            new Option("dprice", SortOrder.DESC, null)));
        assertSort(sorts.get(2), "по размеру скидки", ReportSortType.DISCOUNT,
            Collections.singletonList(new Option("discount_p", SortOrder.DESC, null)));
    }

    private void assertSort(AvailableReportSort sort,
                            String expectedText,
                            ReportSortType expectedField,
                            List<Option> options) {
        assertEquals(expectedText, sort.getText());
        assertEquals(expectedField, sort.getField());
        assertEquals(options, sort.getOptions());
    }
}
