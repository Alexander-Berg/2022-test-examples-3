package ru.yandex.market.mbi.partner_stat.mvc.business.shops;

import java.util.stream.Stream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.mbi.partner_stat.repository.ClickhouseFunctionalTest;
import ru.yandex.market.mbi.partner_stat.repository.summary.ShopSummaryClickhouseDao;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

/**
 * Тесты для {@link ShopSummaryController}
 */
@DbUnitDataSet(dataSource = "clickHouseDataSource", before = "summary/ShopSummaryControllerTest.before.csv")
public class ShopSummaryControllerTest extends ClickhouseFunctionalTest {

    @SpyBean
    private ShopSummaryClickhouseDao dao;

    @DisplayName("Проверка получения сводки по выручке для партнера")
    @ParameterizedTest
    @MethodSource("revenueSummaryArgs")
    void testGetRevenueSummary(long businessId, long partnerId, String expectedFilepath) {
        var response = FunctionalTestHelper.get(getPartnerSummaryUrl(businessId, partnerId, "/revenue"));
        JsonTestUtil.assertEquals(response, getClass(), "summary/" + expectedFilepath);
    }

    @DisplayName("Проверка получения сводки по количеству заказов для партнера")
    @ParameterizedTest
    @MethodSource("ordersSummaryArgs")
    void testGetOrdersSummary(long businessId, long partnerId, String expectedFilepath) {
        var response = FunctionalTestHelper.get(getPartnerSummaryUrl(businessId, partnerId, "/orders"));
        JsonTestUtil.assertEquals(response, getClass(), "summary/" + expectedFilepath);
    }

    @DisplayName("Проверка получения данных по заказам и выручке")
    @ParameterizedTest
    @MethodSource("timeSeriesArgs")
    void testGetSummaryTimeSeries(long businessId, long partnerId, String expectedFilepath) {
        var response = FunctionalTestHelper.get(getPartnerSummaryUrl(businessId, partnerId, "/series"));
        JsonTestUtil.assertEquals(response, getClass(), "summary/" + expectedFilepath);
    }

    @Test
    @Disabled
    @DisplayName("Проверка кэширования данных")
    void testCache() {
        FunctionalTestHelper.get(getPartnerSummaryUrl(1, 308, "/orders"));
        FunctionalTestHelper.get(getPartnerSummaryUrl(1, 308, "/orders"));
        //проверяем, что поход в БД было ровно один
        Mockito.verify(dao, Mockito.times(1)).getOrderCountSummary(eq(1L), eq(308L), any());
        //проверяем, что другие методы dao для данного партнера не вызывались
        Mockito.verifyZeroInteractions(dao);
    }

    private static Stream<Arguments> revenueSummaryArgs() {
        return Stream.of(
                //нет данных о продажах по партнеру
                Arguments.of(1, 666L, "summary.revenue.1.json"),
                //у партнера нет продаж за прошлый период
                Arguments.of(1, 101L, "summary.revenue.2.json"),
                //у партнера нет продаж за текущий период
                Arguments.of(1, 102L, "summary.revenue.3.json"),
                //рост выручки
                Arguments.of(1, 103L, "summary.revenue.4.json"),
                //падение выручки
                Arguments.of(1, 104L, "summary.revenue.5.json")
        );
    }

    private static Stream<Arguments> ordersSummaryArgs() {
        return Stream.of(
                //нет данных о продажах по партнеру
                Arguments.of(1, 666L, "summary.orders.1.json"),
                //у партнера нет продаж за прошлый период
                Arguments.of(1, 101L, "summary.orders.2.json"),
                //у партнера нет продаж за текущий период
                Arguments.of(1, 102L, "summary.orders.3.json"),
                //рост заказов
                Arguments.of(1, 103L, "summary.orders.4.json"),
                //кол-во заказов не изменилось
                Arguments.of(1, 104L, "summary.orders.5.json")
        );
    }

    private static Stream<Arguments> timeSeriesArgs() {
        return Stream.of(
                Arguments.of(1, 101L, "summary.timeseries.1.json")
        );
    }

    private String getPartnerSummaryUrl(long businessId, long shopId, String postfix) {
        return baseUrl() + String.format("businesses/%s/shops/%s/summary/%s",
                businessId, shopId, postfix);
    }
}


