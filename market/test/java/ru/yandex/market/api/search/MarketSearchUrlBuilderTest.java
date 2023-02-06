package ru.yandex.market.api.search;

import javax.inject.Inject;

import com.google.common.collect.ImmutableMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.api.common.url.MarketUrls;
import ru.yandex.market.api.common.client.rules.BlueRule;
import ru.yandex.market.api.common.url.params.UrlParamsFactoryImpl;
import ru.yandex.market.api.domain.v2.redirect.parameters.SearchQuery;
import ru.yandex.market.api.geo.GeoRegionService;
import ru.yandex.market.api.geo.domain.GeoRegion;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.internal.common.UrlSchema;
import ru.yandex.market.api.internal.filters.Filters;
import ru.yandex.market.api.internal.report.ReportSort;
import ru.yandex.market.api.internal.report.SearchForm;
import ru.yandex.market.api.internal.report.SortOrder;
import ru.yandex.market.api.internal.report.ReportSortType;
import ru.yandex.market.api.server.context.Context;
import ru.yandex.market.api.server.version.RegionVersion;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by apershukov on 27.10.16.
 */
@WithContext
public class MarketSearchUrlBuilderTest extends BaseTest {

    @Inject
    private MarketSearchUrlBuilder builder;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        context.setUrlSchema(UrlSchema.HTTP);
    }

    @Test
    public void testBuildSearchUrl() {
        SearchForm form = SearchForm.builder()
            .setQuery(SearchQuery.text("query"))
            .setDeliveryInclude(false)
            .setOnStock(true)
            .setCategories(new IntArrayList(new int[]{91529}))
            .build();

        String url = builder.build(form);

        assertTrue(url.startsWith("http://market.yandex.ru/search"));
        assertTrue(url.contains("hid=91529"));
        assertTrue(url.contains("text=query"));
        assertTrue(url.contains("free-delivery=0"));
        assertTrue(url.contains("onstock=1"));
    }

    @Test
    public void testBuildUrlWithShopIds() {
        SearchForm form = SearchForm.builder()
            .setQuery(SearchQuery.text("query"))
            .setShopIds(new LongArrayList(new long[]{98015, 114020}))
            .build();

        String url = builder.build(form);

        assertTrue(url.contains("fesh=98015%2C114020"));
    }

    @Test
    public void testBuildUrlWithBothPriceBorders() {
        SearchForm form = SearchForm.builder()
            .setQuery(SearchQuery.text("query"))
            .setMinPrice(300)
            .setMaxPrice(10_000)
            .build();

        String url = builder.build(form);

        assertTrue(url.contains("pricefrom=300"));
        assertTrue(url.contains("priceto=10000"));
    }

    @Test
    public void testBuildUrlWithMaxPriceOnly() {
        SearchForm form = SearchForm.builder()
            .setQuery(SearchQuery.text("query"))
            .setMaxPrice(10_000)
            .build();

        String url = builder.build(form);

        assertFalse(url.contains("pricefrom"));
        assertTrue(url.contains("priceto=10000"));
    }

    @Test
    public void testBuildUrlWithWarranty() {
        SearchForm form = SearchForm.builder()
            .setQuery(SearchQuery.text("query"))
            .setWarranty(true)
            .build();

        String url = builder.build(form);

        assertTrue(url.contains("manufacturer_warranty=1"));
    }

    @Test
    public void testBuildUrlWithShopRating() {
        SearchForm form = SearchForm.builder()
            .setQuery(SearchQuery.text("query"))
            .setFilterParams(ImmutableMap.of(Filters.SHOP_RATING_FILTER_CODE, "4"))
            .build();

        String url = builder.build(form);

        assertTrue(url.contains("qrfrom=4"));
    }

    /**
     * Тестирование того что параметры фильтров имеют приоритет над параметрами запроса
     */
    @Test
    public void testFilterParametersHavePriority() {
        SearchForm form = SearchForm.builder()
            .setQuery(SearchQuery.text("query"))
            .setShopIds(new LongArrayList(new long[]{98015}))
            .setFilterParams(ImmutableMap.of(Filters.SHOP_FILTER_CODE, "114020"))
            .build();

        String url = builder.build(form);

        assertTrue(url.contains("fesh=114020"));
    }

    @Test
    public void testBuildUrlWithVendorField() {
        SearchForm form = SearchForm.builder()
            .setQuery(SearchQuery.text("query"))
            .setFilterParams(ImmutableMap.of(Filters.VENDOR_FILTER_CODE, "111"))
            .build();

        String url = builder.build(form);

        assertTrue(url.contains("glfilter=7893318%3A111"));
    }

    @Test
    public void testBuildUrlWithMultipleHids() {
        SearchForm form = SearchForm.builder()
            .setQuery(SearchQuery.text("query"))
            .setCategories(new IntArrayList(new int[]{111, 222}))
            .build();

        String url = builder.build(form);

        assertTrue(url.contains("hid=111%2C222"));
    }

    @Test
    public void testBuildUrlWithSort() {
        SearchForm form = SearchForm.builder()
            .setQuery(SearchQuery.text("query"))
            .setSort(new ReportSort(ReportSortType.PRICE, SortOrder.ASC))
            .build();

        String url = builder.build(form);

        assertTrue(url.contains("how=aprice"));
    }

    @Test
    public void testSearchByBarkode() {
        SearchForm form = SearchForm.builder()
            .setQuery(new SearchQuery("123123", SearchType.BARCODE, null))
            .build();

        String url = builder.build(form);

        assertTrue(url.contains("text=barcode%3A%22123123%22"));
    }
}
