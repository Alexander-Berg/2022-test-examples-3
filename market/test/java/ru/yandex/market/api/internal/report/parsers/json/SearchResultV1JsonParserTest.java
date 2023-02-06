package ru.yandex.market.api.internal.report.parsers.json;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.api.category.searchCategory.SearchCategories;
import ru.yandex.market.api.domain.PageInfo;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.internal.report.ReportRequestContext;
import ru.yandex.market.api.internal.report.parsers.ReportParserFactory;
import ru.yandex.market.api.search.PageSearchResult;
import ru.yandex.market.api.search.SearchItem;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;
import ru.yandex.market.api.util.ResourceHelpers;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;

/**
 * Created by apershukov on 06.03.17.
 */
@WithContext
public class SearchResultV1JsonParserTest extends BaseTest {

    @Inject
    private ReportParserFactory reportParserFactory;

    private SearchResultV1JsonParser parser;

    @Before
    public void setUp() {
        parser = reportParserFactory.createPageSearchResultParser(
                Collections.emptyList(),
                new ReportRequestContext(),
                new PageInfo(1, 5),
                true
        );
    }

    @Test
    public void testParseMixedSearchResult() {
        PageSearchResult result = parser.parse(ResourceHelpers.getResource("mixed-search-results.json"));

        assertNotNull(result);

        List<SearchItem> items = result.getResults();
        assertEquals(5, items.size());

        assertNotNull(items.get(0).getModel());
        assertNull(items.get(0).getOffer());
        assertEquals(13485515, items.get(0).getModel().getId());

        assertNotNull(items.get(1).getModel());
        assertNull(items.get(1).getOffer());
        assertEquals(13485518, items.get(1).getModel().getId());

        assertNotNull(items.get(2).getModel());
        assertNull(items.get(2).getOffer());
        assertEquals(13953515, items.get(2).getModel().getId());

        assertNotNull(items.get(3).getOffer());
        assertNull(items.get(3).getModel());
        assertEquals("5Wpx3oygkYj-r8nkFfngmQ", items.get(3).getOffer().getWareMd5());

        assertNotNull(items.get(4).getModel());
        assertNull(items.get(4).getOffer());
        assertEquals(13584121, items.get(4).getModel().getId());

        assertEquals(1, result.getPage());
        assertEquals(5, result.getCount());
        assertEquals(80934, result.getTotal());

        assertEquals(2, (int) result.getRegionDelimiterPosition());

        assertEquals("390", items.get(3).getOffer().getDelivery().getPrice().getValue());
    }

    @Test
    public void testParseSearchCategories() {
        PageSearchResult result = parser.parse(ResourceHelpers.getResource("mixed-search-results.json"));

        SearchCategories categories = result.getCategories();
        assertNotNull(categories);

        assertEquals(6, categories.size());

        assertEquals("Мобильные телефоны", categories.get(0).getName());
        assertEquals("Мобильные телефоны", categories.get(0).getUniq_name());
        assertEquals("91491", categories.get(0).getId());
        assertEquals("80934", categories.get(0).getCount());

        assertEquals("Чехлы для планшетов", categories.get(1).getName());
        assertEquals("Чехлы для планшетов", categories.get(1).getUniq_name());
        assertEquals("2662954", categories.get(1).getId());
        assertEquals("297", categories.get(1).getCount());

        assertEquals("Кабели, разъемы, переходники", categories.get(2).getName());
        assertEquals("Компьютерные кабели, разъемы, переходники", categories.get(2).getUniq_name());
        assertEquals("91074", categories.get(2).getId());
        assertEquals("24", categories.get(2).getCount());

        assertEquals("Инструменты", categories.get(3).getName());
        assertEquals("Компьютерные инструменты", categories.get(3).getUniq_name());
        assertEquals("91073", categories.get(3).getId());
        assertEquals("14", categories.get(3).getCount());

        assertEquals("USB Flash drive", categories.get(4).getName());
        assertEquals("USB Flash drive", categories.get(4).getUniq_name());
        assertEquals("288003", categories.get(4).getId());
        assertEquals("27", categories.get(4).getCount());

        assertEquals("Рули, джойстики, геймпады", categories.get(5).getName());
        assertEquals("Рули, джойстики, геймпады", categories.get(5).getUniq_name());
        assertEquals("91117", categories.get(5).getId());
        assertEquals("13", categories.get(5).getCount());
    }
}
