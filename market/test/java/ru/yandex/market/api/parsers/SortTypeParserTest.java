package ru.yandex.market.api.parsers;

import org.junit.Test;
import ru.yandex.market.api.MockRequestBuilder;
import ru.yandex.market.api.common.url.UrlControllerHelper;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.internal.report.SortOrder;
import ru.yandex.market.api.model.UniversalModelSort;

import javax.servlet.http.HttpServletRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.api.model.UniversalModelSort.SortField;
import static ru.yandex.market.api.util.ObjectUtil.allNonNull;

/**
 * @author dimkarp93
 */
public class SortTypeParserTest extends UnitTestBase {
    @Test
    public void howAsc_sortPrice_parse_priceAscSort() {
        HttpServletRequest request = MockRequestBuilder.start()
            .param("how", "ASC")
            .param("sort", "PRICE")
            .build();

        UniversalModelSort actual = new UrlControllerHelper.UrlSortTypeParser().get(request).getValue();
        assertNull(actual);
    }

    @Test
    public void howQuality_sortPrice_parse_qualityDescSort() {
        HttpServletRequest request = MockRequestBuilder.start()
            .param("how", "ASC")
            .param("how", "quality")
            .param("how", "DESC")
            .param("sort", "PRICE")
            .build();

        UniversalModelSort actual = new UrlControllerHelper.UrlSortTypeParser().get(request).getValue();
        assertSort(new UniversalModelSort(SortField.QUALITY, SortOrder.DESC), actual);
    }


    private void assertSort(UniversalModelSort expected, UniversalModelSort actual) {
        assertTrue(allNonNull(expected, actual));
        assertEquals(expected.getSort(), actual.getSort());
        assertEquals(expected.getHow(), actual.getHow());
    }
}
