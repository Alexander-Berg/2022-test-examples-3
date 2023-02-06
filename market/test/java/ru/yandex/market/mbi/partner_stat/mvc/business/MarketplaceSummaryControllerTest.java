package ru.yandex.market.mbi.partner_stat.mvc.business;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.mbi.partner_stat.repository.ClickhouseFunctionalTest;
import ru.yandex.market.mbi.partner_stat.repository.summary.MarketplaceSummaryClickhouseDao;

/**
 * Тесты для {@link MarketplaceSummaryController}
 */
@DbUnitDataSet(dataSource = "clickHouseDataSource", before = "marketplace/before.csv")
public class MarketplaceSummaryControllerTest extends ClickhouseFunctionalTest {

    @SpyBean
    private MarketplaceSummaryClickhouseDao dao;

    @DisplayName("Проверка получения сводки по выручке для бизнеса")
    @ParameterizedTest
    @MethodSource("revenueSummaryArgs")
    void testGetRevenueSummary(long businessId, String expectedFilepath) {
        var response = FunctionalTestHelper.get(getMarketplaceSummaryUrl(businessId, "/revenue"));
        JsonTestUtil.assertEquals(response, getClass(), "marketplace/" + expectedFilepath);
    }

    @DisplayName("Проверка получения сводки по заказам для бизнеса")
    @ParameterizedTest
    @MethodSource("ordersSummaryArgs")
    void testGetOrderSummary(long businessId, String expectedFilepath) {
        var response = FunctionalTestHelper.get(getMarketplaceSummaryUrl(businessId, "/orders"));
        JsonTestUtil.assertEquals(response, getClass(), "marketplace/" + expectedFilepath);
    }

    @DisplayName("Проверка получения данных по заказам и выручке")
    @ParameterizedTest
    @MethodSource("timeSeriesArgs")
    void testGetSummaryTimeSeries(long businessId, String expectedFilepath) {
        var response = FunctionalTestHelper.get(
                getMarketplaceSummaryUrl(businessId, "/series"));
        JsonTestUtil.assertEquals(response, getClass(), "marketplace/" + expectedFilepath);
    }

    private static Stream<Arguments> revenueSummaryArgs() {
        return Stream.of(
                //нет данных о продажах по бизнесу
                Arguments.of(6, "summary.revenue.1.json"),
                //у бизнеса нет продаж за прошлый период
                Arguments.of(2, "summary.revenue.2.json"),
                //у партнера нет продаж за текущий период
                Arguments.of(3, "summary.revenue.3.json"),
                //рост выручки
                Arguments.of(4, "summary.revenue.4.json"),
                //падение выручки
                Arguments.of(5, "summary.revenue.5.json")
        );
    }

    private static Stream<Arguments> ordersSummaryArgs() {
        return Stream.of(
                //нет данных о продажах по партнеру
                Arguments.of(6, "summary.orders.1.json"),
                //у партнера нет продаж за прошлый период
                Arguments.of(2, "summary.orders.2.json"),
                //у партнера нет продаж за текущий период
                Arguments.of(3, "summary.orders.3.json"),
                //рост заказов
                Arguments.of(4, "summary.orders.4.json"),
                //падение заказов
                Arguments.of(5, "summary.orders.5.json")
        );
    }

    private static Stream<Arguments> timeSeriesArgs() {
        return Stream.of(
                Arguments.of(1, "summary.timeseries.1.json")
        );
    }

    private String getMarketplaceSummaryUrl(long businessId, String postfix) {
        return baseUrl() + String.format("businesses/%s/summary/%s",
                businessId, postfix);
    }
}
