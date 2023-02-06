package ru.yandex.market.api.parsers;

import javax.servlet.http.HttpServletRequest;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.api.MockRequestBuilder;
import ru.yandex.market.api.category.FilterService;
import ru.yandex.market.api.common.Result;
import ru.yandex.market.api.error.ValidationError;
import ru.yandex.market.api.integration.UnitTestBase;

/**
 * @author dimkarp93
 */
public class FilterSortParserTest extends UnitTestBase {
    private final FilterService.FilterSortParser parser =
            new FilterService.FilterSortParser();

    @Test
    public void testParseName() {
        HttpServletRequest request = MockRequestBuilder.start()
                .param("sort", "name")
                .build();

        Result<FilterService.FilterSort, ValidationError> result = parser.get(request);

        Assert.assertTrue(result.isOk());
        Assert.assertThat(result.getValue(), Matchers.is(FilterService.FilterSort.NAME));
    }

    @Test
    public void testParseNone() {
        HttpServletRequest request = MockRequestBuilder.start()
                .param("sort", "none")
                .build();

        Result<FilterService.FilterSort, ValidationError> result = parser.get(request);

        Assert.assertTrue(result.isOk());
        Assert.assertThat(result.getValue(), Matchers.is(FilterService.FilterSort.NONE));
    }

    @Test
    public void testParseNull() {
        HttpServletRequest request = MockRequestBuilder.start()
                .build();

        Result<FilterService.FilterSort, ValidationError> result = parser.get(request);
        
        Assert.assertTrue(result.isOk());
        Assert.assertThat(result.getValue(), Matchers.is(FilterService.FilterSort.NONE));
    }
}
