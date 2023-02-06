package ru.yandex.market.mbi.partner_stat.mvc.campaign;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.mbi.partner_stat.FunctionalTest;
import ru.yandex.market.mbi.partner_stat.entity.statistics.SummaryEntity;
import ru.yandex.market.mbi.partner_stat.repository.common.model.TimeSeries;
import ru.yandex.market.mbi.partner_stat.repository.common.model.TimeSeriesPoint;
import ru.yandex.market.mbi.partner_stat.repository.stat.StatClickHouseDao;
import ru.yandex.market.mbi.partner_stat.repository.stat.StatDictionaryClickHouseDao;
import ru.yandex.market.mbi.partner_stat.service.stat.model.StatFilter;
import ru.yandex.market.mbi.partner_stat.service.stat.model.TimeSeriesType;
import ru.yandex.market.mbi.partner_stat.service.stat.model.detail.DetailPage;
import ru.yandex.market.mbi.partner_stat.service.stat.model.detail.DetailPageFilter;
import ru.yandex.market.mbi.partner_stat.service.stat.model.detail.DetailRow;
import ru.yandex.market.mbi.partner_stat.service.stat.model.detail.DetailTotal;
import ru.yandex.market.mbi.partner_stat.service.stat.model.detail.DetailTotalFilter;
import ru.yandex.market.mbi.partner_stat.service.stat.model.filter.Brand;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;

/**
 * Тесты для {@link CampaignStatController}
 */
class CampaignStatControllerTest extends FunctionalTest {

    private static final String FILTER = "" +
            "{\n" +
            "    \"businessId\" : 1,\n" +
            "    \"partnerId\" : 774,\n" +
            "    \"detalization\" : \"DAY\",\n" +
            "    \"dateFrom\" : \"2015-08-31T00:00:00+03:00\",\n" +
            "    \"dateTo\" : \"2022-09-11T00:00:00+03:00\",\n" +
            "    \"regionIds\" : [\n" +
            "        1,\n" +
            "        2\n" +
            "    ],\n" +
            "    \"brandIds\" : [\n" +
            "        1,\n" +
            "        2\n" +
            "    ],\n" +
            "    \"categoryIds\" : [\n" +
            "        3,\n" +
            "        4\n" +
            "    ],\n" +
            "    \"skus\" : [\n" +
            "          \"msku1\",\n" +
            "          \"msku1\"\n" +
            "    ]\n" +
            "}";
    private static final String BRANDS = "" +
            "{\n" +
            "    \"businessId\" : 1,\n" +
            "    \"partnerId\" : 774,\n" +
            "    \"categoryIds\" : [\n" +
            "        3,\n" +
            "        4\n" +
            "    ]\n" +
            "}";
    private static final String REGIONS = "" +
            "{\n" +
            "    \"businessId\" : 1,\n" +
            "    \"partnerId\" : 774\n" +
            "}";
    private static final String DETAIL_TOTAL = "" +
            "{" +
            "    \"filter\" : {\n" +
            "    \"businessId\" : 1,\n" +
            "    \"partnerId\" : 774,\n" +
            "    \"detalization\" : \"YEAR\",\n" +
            "    \"dateFrom\" : \"2018-03-14T19:28:09+03:00\",\n" +
            "    \"dateTo\" : \"2018-03-14T19:28:09+03:00\",\n" +
            "    \"regionIds\" : [\n" +
            "        1,\n" +
            "        2\n" +
            "    ],\n" +

            "    \"categoryIds\" : [\n" +
            "        3,\n" +
            "        4\n" +
            "    ],\n" +
            "    \"skus\" : [\n" +
            "          \"msku1\",\n" +
            "          \"msku1\"\n" +
            "    ]\n" +
            "   },\n" +
            "   \"grouping\" : \"BRANDS\"" +
            "}";
    private static final String DETAIL_PAGE = "" +
            "{" +
            "    \"filter\" : {\n" +
            "    \"businessId\" : 1,\n" +
            "    \"partnerId\" : 774,\n" +
            "    \"detalization\" : \"YEAR\",\n" +
            "    \"dateFrom\" : \"2018-03-14T19:28:09+03:00\",\n" +
            "    \"dateTo\" : \"2018-03-14T19:28:09+03:00\",\n" +
            "    \"regionIds\" : [\n" +
            "        1,\n" +
            "        2\n" +
            "    ],\n" +
            "    \"brandIds\" : [\n" +
            "        1,\n" +
            "        2\n" +
            "    ],\n" +
            "    \"categoryIds\" : [\n" +
            "        3,\n" +
            "        4\n" +
            "    ],\n" +
            "    \"skus\" : [\n" +
            "          \"msku1\",\n" +
            "          \"msku1\"\n" +
            "    ]\n" +
            "   }, " +
            "   \"settings\" : {" +
            "       \"offset\" : 1," +
            "       \"count\" : 2," +
            "       \"grouping\" : \"BRANDS\"," +
            "       \"searchString\" : \"text\"," +
            "       \"sort\" : {" +
            "         \"column\" : \"SHOWS\"," +
            "         \"direction\" : \"ASC\"" +
            "       }" +
            "   }\n" +
            "}";

    @Autowired
    private StatClickHouseDao statClickHouseDao;

    @Autowired
    private StatDictionaryClickHouseDao statDictionaryClickHouseDao;

    @BeforeEach
    public void init() {
        Mockito.when(statClickHouseDao.getDetailPage(Mockito.any(DetailPageFilter.class)))
                .thenReturn(new DetailPage(List.of(
                        DetailRow.builder()
                                .setTitle("msku1")
                                .setId("msku1")
                                .setShows(BigDecimal.valueOf(230))
                                .setCheckouts(BigDecimal.valueOf(8))
                                .setCheckoutsConversion(BigDecimal.valueOf(3.47))
                                .setItemsDelivered(BigDecimal.valueOf(12))
                                .setPrice(BigDecimal.valueOf(3999))
                                .setSales(BigDecimal.valueOf(47988))
                                .build()
                )));

        Mockito.when(statClickHouseDao.getTimeSeries(eq(TimeSeriesType.ITEMS_DELIVERED), Mockito.any(StatFilter.class)))
                .thenReturn(new TimeSeries(List.of(
                        new TimeSeriesPoint(BigDecimal.valueOf(1), 2L),
                        new TimeSeriesPoint(BigDecimal.valueOf(2), 3L))));

        Mockito.when(statClickHouseDao.getTimeSeries(eq(TimeSeriesType.SHOWS), Mockito.any(StatFilter.class)))
                .thenReturn(new TimeSeries(List.of(
                        new TimeSeriesPoint(BigDecimal.valueOf(4), 5L),
                        new TimeSeriesPoint(BigDecimal.valueOf(6), 7L))));

        Mockito.when(statClickHouseDao.getTimeSeries(eq(TimeSeriesType.GMV_DELIVERED), Mockito.any(StatFilter.class)))
                .thenReturn(new TimeSeries(List.of(
                        new TimeSeriesPoint(BigDecimal.valueOf(12), 13L),
                        new TimeSeriesPoint(BigDecimal.valueOf(14), 15L))));

        Mockito.when(statClickHouseDao.getTimeSeries(eq(TimeSeriesType.CHECKOUTS), Mockito.any(StatFilter.class)))
                .thenReturn(new TimeSeries(List.of(
                        new TimeSeriesPoint(BigDecimal.valueOf(16), 17L),
                        new TimeSeriesPoint(BigDecimal.valueOf(18), 19L))));

        Mockito.when(statClickHouseDao.getTimeSeriesSummary(eq(TimeSeriesType.ITEMS_DELIVERED), Mockito.any(StatFilter.class)))
                .thenReturn(new SummaryEntity(BigDecimal.valueOf(20), BigDecimal.valueOf(21)));

        Mockito.when(statClickHouseDao.getTimeSeriesSummary(eq(TimeSeriesType.SHOWS), Mockito.any(StatFilter.class)))
                .thenReturn(new SummaryEntity(BigDecimal.valueOf(22), BigDecimal.valueOf(23)));

        Mockito.when(statClickHouseDao.getTimeSeriesSummary(eq(TimeSeriesType.GMV_DELIVERED), Mockito.any(StatFilter.class)))
                .thenReturn(new SummaryEntity(BigDecimal.valueOf(26), BigDecimal.valueOf(27)));

        Mockito.when(statClickHouseDao.getTimeSeriesSummary(eq(TimeSeriesType.CHECKOUTS),
                Mockito.any(StatFilter.class)))
                .thenReturn(new SummaryEntity(BigDecimal.valueOf(28), BigDecimal.valueOf(29)));

        Mockito.when(statDictionaryClickHouseDao.getBrands(eq(1L), eq(774L), anyList()))
                .thenReturn(List.of(
                        new Brand(123, "Samsung"),
                        new Brand(456, ""),
                        new Brand(0, "ImaginaryBrand")
                ));

        Mockito.when(statClickHouseDao.getDetailTotal(Mockito.any(DetailTotalFilter.class)))
                .thenReturn(DetailTotal.Builder.newBuilder()
                        .totalCount(30)

                        .summaryShows(BigDecimal.valueOf(31))
                        .summaryCheckouts(BigDecimal.valueOf(32))
                        .summaryItemsDelivered(BigDecimal.valueOf(33))
                        .summaryOrders(BigDecimal.valueOf(77))
                        .summaryOrdersDelivered(BigDecimal.valueOf(77))
                        .summarySales(BigDecimal.valueOf(36))

                        .averageCheckoutsConversion(BigDecimal.valueOf(39))
                        .averageOrderConversion(BigDecimal.valueOf(41))
                        .averagePrice(BigDecimal.valueOf(42))

                        .build());
    }

    @DisplayName("Проверка получения графика продаж")
    @Test
    void testGetSalesPlot() {
        String url = getUrl(1L, "sales/plot");
        ResponseEntity<String> responseEntity = FunctionalTestHelper.post(url, new HttpEntity<>(FILTER, jsonHeaders()));
    }

    @DisplayName("Проверка получения мини-сводки графика продаж")
    @Test
    void testGetSalesSummary() {
        String url = getUrl(1L, "sales/summary");
        ResponseEntity<String> responseEntity = FunctionalTestHelper.post(url, new HttpEntity<>(FILTER, jsonHeaders()));
        JsonTestUtil.assertEquals(responseEntity, "{\"partnerId\":774,\"dateFrom\":\"2015-08-31T00:00:00" +
                "+03:00\",\"dateTo\":\"2022-09-11T00:00:00+03:00\",\"sales\":{\"total\":20,\"change\":21}," +
                "\"turnover\":{\"total\":26,\"change\":27}}");
    }

    @DisplayName("Проверка получения графика корзины")
    @Test
    void testGetCheckoutsPlot() {
        String url = getUrl(1L, "checkout/plot");
        ResponseEntity<String> responseEntity = FunctionalTestHelper.post(url, new HttpEntity<>(FILTER, jsonHeaders()));
    }

    @DisplayName("Проверка получения мини-сводки графика корзины")
    @Test
    void testGetCheckoutsSummary() {
        String url = getUrl(1L, "checkout/summary");
        ResponseEntity<String> responseEntity = FunctionalTestHelper.post(url, new HttpEntity<>(FILTER, jsonHeaders()));
        JsonTestUtil.assertEquals(responseEntity, "{\"partnerId\":774,\"dateFrom\":\"2015-08-31T00:00:00" +
                "+03:00\",\"dateTo\":\"2022-09-11T00:00:00+03:00\",\"checkout\":{\"total\":28,\"change\":29}}");
    }

    @DisplayName("Проверка получения графика показов")
    @Test
    void testGetShowsPlot() {
        String url = getUrl(1L, "shows/plot");
        ResponseEntity<String> responseEntity = FunctionalTestHelper.post(url, new HttpEntity<>(FILTER, jsonHeaders()));
    }

    @DisplayName("Проверка получения мини-сводки графика показов")
    @Test
    void testGetShowsSummary() {
        String url = getUrl(1L, "shows/summary");
        ResponseEntity<String> responseEntity = FunctionalTestHelper.post(url, new HttpEntity<>(FILTER, jsonHeaders()));
        JsonTestUtil.assertEquals(responseEntity, "{\"partnerId\":774,\"dateFrom\":\"2015-08-31T00:00:00+03" +
                ":00\",\"dateTo\":\"2022-09-11T00:00:00+03:00\",\"shows\":{\"total\":22,\"change\":23}}");
    }

    @DisplayName("Проверка получения итоговой информации по детальному отчету")
    @Test
    void testGetDetailTotal() {
        String url = getUrl(1L, "detail/total");
        ResponseEntity<String> responseEntity = FunctionalTestHelper.post(url, new HttpEntity<>(DETAIL_TOTAL,
                jsonHeaders()));
        JsonTestUtil.assertEquals(responseEntity, "{\"partnerId\":774,\"totalCount\":30" +
                ",\"summary\":{\"shows\":31," +
                "\"checkouts\":32,\"itemsDelivered\":33," +
                "\"sales\":36},\"average\":{" +
                "\"checkoutsConversion\":39}}");
    }

    @DisplayName("Проверка получения страницы таблицы детального отчета")
    @Test
    void testGetDetailPage() {
        String url = getUrl(1L, "detail/page");
        ResponseEntity<String> responseEntity = FunctionalTestHelper.post(url, new HttpEntity<>(DETAIL_PAGE,
                jsonHeaders()));
        JsonTestUtil.assertEquals(responseEntity, "{\"partnerId\":774,\"items\":[{\"title\":\"msku1\"," +
                "\"id\":\"msku1\",\"row\":{\"shows\":230,\"checkouts\":8,\"checkoutsConversion\":3.47," +
                "\"itemsDelivered\":12," +
                "\"price\":null,\"sales\":47988}}]}");
    }

    @DisplayName("Проверка получения брендов поставщика")
    @Test
    void testGetBrands() {
        String url = getUrl(1L, "filter/brands");
        ResponseEntity<String> responseEntity = FunctionalTestHelper.post(url,
                new HttpEntity<>(BRANDS, jsonHeaders()));
        JsonTestUtil.assertEquals(responseEntity, "{\"partnerId\":774,\"brands\":[{\"brandId\":123,\"brandName\":\"Samsung\"}]}");
    }

    @DisplayName("Проверка получения регионов поставщика")
    @Test
    void testGetRegions() {
        String url = getUrl(1L, "filter/regions");
        ResponseEntity<String> responseEntity = FunctionalTestHelper.post(url,
                new HttpEntity<>(REGIONS, jsonHeaders()));
        JsonTestUtil.assertEquals(responseEntity, "{\"partnerId\":774,\"regions\":[]}");
    }

    private String getUrl(final Long campaignId, final String url) {
        return baseUrl() + "/campaigns/" + campaignId + "/stat/" + url;
    }

    private static HttpHeaders jsonHeaders() {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        return httpHeaders;
    }
}
