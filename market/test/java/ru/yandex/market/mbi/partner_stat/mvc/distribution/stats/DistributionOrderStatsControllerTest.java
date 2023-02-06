package ru.yandex.market.mbi.partner_stat.mvc.distribution.stats;

import java.time.LocalDate;

import com.google.gson.JsonParser;
import org.dbunit.database.DatabaseConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.common.util.db.SortingOrder;
import ru.yandex.market.common.test.db.DbUnitDataBaseConfig;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.mbi.partner_stat.config.ClickHouseTestConfig;
import ru.yandex.market.mbi.partner_stat.mvc.distribution.model.SortingField;
import ru.yandex.market.mbi.partner_stat.repository.ClickhouseFunctionalTest;

/**
 * Тесты для {@link DistributionOrderStatsController}
 */
@DbUnitDataBaseConfig(
        @DbUnitDataBaseConfig.Entry(
                name = DatabaseConfig.FEATURE_ALLOW_EMPTY_FIELDS,
                value = "true"))
class DistributionOrderStatsControllerTest extends ClickhouseFunctionalTest {

    @DisplayName("Проверка получения краткого списка заказов ручки старого формата")
    @Test
    @DbUnitDataSet(
            dataSource = ClickHouseTestConfig.DATA_SOURCE,
            before = "DistributionOrderStatsController/distribution_order_stats_total.csv")
    void testGetOrderStatsTotal() {
        final ResponseEntity<String> responseEntity = FunctionalTestHelper.get(getDistributionOrderStatsUrl(true));
        JsonTestUtil.assertEquals(responseEntity, getClass(), "DistributionOrderStatsController/testGetOrderStatsTotal.json");
    }

    @DisplayName("Проверка получения краткого списка заказов ручки нового формата")
    @Test
    @DbUnitDataSet(
            dataSource = ClickHouseTestConfig.DATA_SOURCE,
            before = "DistributionOrderStatsController/distribution_order_stats_total.csv")
    void testGetOrderStatsV2Total() {
        final ResponseEntity<String> responseEntity = FunctionalTestHelper.get(getDistributionOrderStatsV2Url(true));
        JsonTestUtil.assertEquals(responseEntity, getClass(), "DistributionOrderStatsController/testGetOrderStatsV2Total.json");
    }

    @DisplayName("Проверка получения подробного списка заказов ручки нового формата")
    @Test
    @DbUnitDataSet(
            dataSource = ClickHouseTestConfig.DATA_SOURCE,
            before = "DistributionOrderStatsController/distribution_order_stats_detailed.csv")
    void testGetOrderStatsDetailed() {
        final ResponseEntity<String> responseEntity = FunctionalTestHelper.get(getDistributionOrderStatsUrl(false));
        JsonTestUtil.assertEquals(responseEntity, getClass(), "DistributionOrderStatsController/testGetOrderStatsDetailed" +
                ".json");
    }

    @DisplayName("Проверка получения подробного списка заказов ручки нового формата")
    @Test
    @DbUnitDataSet(
            dataSource = ClickHouseTestConfig.DATA_SOURCE,
            before = "DistributionOrderStatsController/distribution_order_stats_detailed.csv")
    void testGetOrderStatsV2Detailed() {
        final ResponseEntity<String> responseEntity = FunctionalTestHelper.get(getDistributionOrderStatsV2Url(false));
        JsonTestUtil.assertEquals(responseEntity, getClass(), "DistributionOrderStatsController/testGetOrderStatsV2Detailed" +
                ".json");
    }

    @Test
    @DisplayName("Проверка получения бигдесимала как 0, а не как 0Е-8 ручки нового формата")
    @DbUnitDataSet(
            dataSource = ClickHouseTestConfig.DATA_SOURCE,
            before = "DistributionOrderStatsController/distribution_order_stats_detailed.csv")
    void testGetOrderStatsBigDecimalLikeAZero() {
        final ResponseEntity<String> responseEntity = FunctionalTestHelper.get(getDistributionOrderStatsV2Url(false));
        JsonParser jsonParser = new JsonParser();
        String body = responseEntity.getBody();
        Assertions.assertNotNull(body);
        String tariffRateAsString = jsonParser.parse(responseEntity.getBody()).getAsJsonObject()
                .get("result").getAsJsonObject()
                .get("orders").getAsJsonArray()
                .get(0).getAsJsonObject()
                .get("items").getAsJsonArray()
                .get(0).getAsJsonObject()
                .get("tariffRate").getAsString();
        Assertions.assertEquals("0", tariffRateAsString);
    }

    @DisplayName("Проверка отсутствия элементов в clid ручки нового формата")
    @Test
    void testGetV2OrdersEmptyClid() {
        final var exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.get(getDistributionOrderStatsUriBuilder().toUriString())
        );
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        JsonTestUtil.assertResponseErrorMessage(exception, getClass(),
                "DistributionOrderStatsController/testGetOrdersEmptyClid.expected.json");
    }

    @DisplayName("Проверка отсутствия элементов в clid ручки нового формата")
    @Test
    void testGetOrderStatsV2EmptyClid() {
        final var exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.get(getDistributionOrderStatsV2UriBuilder().toUriString())
        );
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        JsonTestUtil.assertResponseErrorMessage(exception, getClass(),
                "DistributionOrderStatsController/testGetOrdersV2EmptyClid.expected.json");
    }

    @DisplayName("Проверка нескольких значений в clid ручки нового формата")
    @Test
    void testGetOrderStatsMultipleClid() {
        final ResponseEntity<String> responseEntity =
                FunctionalTestHelper.get(getDistributionOrderStatsUriBuilder(1L, 2L, 3L).toUriString());
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    }

    @DisplayName("Проверка нескольких значений в clid ручки нового формата")
    @Test
    void testGetOrderStatsV2MultiplyClid() {
        final ResponseEntity<String> responseEntity =
                FunctionalTestHelper.get(getDistributionOrderStatsV2UriBuilder(1L, 2L, 3L).toUriString());
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    }

    @DisplayName("Проверка временнЫх параметров ручки нового формата")
    @Test
    void testGetOrderStatsDateTime() {
        final var url = getDistributionOrderStatsUriBuilder(1L)
                .queryParam("dateStart", "2020-02-13T00:00:00")
                .queryParam("dateEnd", "2020-02-14T00:00:00")
                .queryParam("updatedStart", "2020-02-13T00:00:00")
                .queryParam("updatedEnd", "2020-02-14T00:00:00")
                .toUriString();

        final ResponseEntity<String> responseEntity = FunctionalTestHelper.get(url);
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    }

    @DisplayName("Проверка временнЫх параметров ручки нового формата")
    @Test
    void testGetOrderStatsV2DateTime() {
        final var url = getDistributionOrderStatsV2UriBuilder(1L)
                .queryParam("dateStart", "2020-02-13T00:00:00")
                .queryParam("dateEnd", "2020-02-14T00:00:00")
                .queryParam("updatedStart", "2020-02-13T00:00:00")
                .queryParam("updatedEnd", "2020-02-14T00:00:00")
                .toUriString();

        final ResponseEntity<String> responseEntity = FunctionalTestHelper.get(url);
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    }

    @DisplayName("Проверка параметров для сортировки ручки нового формата")
    @MethodSource("testGetOrderSortingArgs")
    @ParameterizedTest
    void testGetOrderStatsorting(String sortingFieldName) {
        final var url = getDistributionOrderStatsUriBuilder(1L)
                .queryParam("sortingField", sortingFieldName)
                .queryParam("sortingOrder", "DESC")
                .toUriString();

        final ResponseEntity<String> responseEntity = FunctionalTestHelper.get(url);
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    }

    @DisplayName("Проверка параметров для сортировки ручки нового формата")
    @MethodSource("testGetOrderSortingArgs")
    @ParameterizedTest
    void testGetOrderStatsV2Sorting(String sortingFieldName) {
        final var url = getDistributionOrderStatsV2UriBuilder(1L)
                .queryParam("sortingField", sortingFieldName)
                .queryParam("sortingOrder", "DESC")
                .toUriString();

        final ResponseEntity<String> responseEntity = FunctionalTestHelper.get(url);
        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    }

    @DisplayName("Проверка невалидного параметра сортировки ручки нового формата")
    @Test
    void testGetOrderStatsortingInvalidName() {
        final var url = getDistributionOrderStatsUriBuilder(1L)
                .queryParam("sortingField", "take_rate")
                .toUriString();

        final var exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.get(url)
        );

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @DisplayName("Проверка невалидного параметра сортировки ручки нового формата")
    @Test
    void testGetOrderStatsV2SortingInvalidName() {
        final var url = getDistributionOrderStatsV2UriBuilder(1L)
                .queryParam("sortingField", "take_rate")
                .toUriString();

        final var exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.get(url)
        );

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @DisplayName("Проверка получения информации о балансе из таблиц нового формата")
    @Test
    @DbUnitDataSet(
            dataSource = ClickHouseTestConfig.DATA_SOURCE,
            before = "DistributionOrderStatsController/distribution_order_stats_balance.csv")
    void testV2GetBalanceInfo() {
        final ResponseEntity<String> responseEntity = FunctionalTestHelper.get(getDistributionOrderStatsCommonUrl("balance", 1L));
        JsonTestUtil.assertEquals(responseEntity, getClass(), "DistributionOrderStatsController/testGetBalanceInfo.json");
    }

    @DisplayName("Проверка списка популярных заказов")
    @Test
    @DbUnitDataSet(
            dataSource = ClickHouseTestConfig.DATA_SOURCE,
            before = "DistributionOrderStatsController/distribution_top_v2.csv")
    void testV2GetTopOrders() {
        String v2CommonUrl = getDistributionOrderStatsCommonUrl("top/orders", 1L);
        final ResponseEntity<String> responseEntity = FunctionalTestHelper.get(v2CommonUrl);
        JsonTestUtil.assertEquals(responseEntity, getClass(), "DistributionOrderStatsController/testGetTopOrders.json");
    }

    @DisplayName("Проверка получения статистики для графиков")
    @Test
    @DbUnitDataSet(
            dataSource = ClickHouseTestConfig.DATA_SOURCE,
            before = "DistributionOrderStatsController/distribution_order_stats_graph.csv")
    void testV2GetGraphsInfo() {
        final var url = getDistributionOrderStatsCommonUriBuilder("graphs", 1L)
                .queryParam("dateStart", LocalDate.of(2020, 3, 14))
                .queryParam("dateEnd", LocalDate.of(2020, 3, 16))
                .toUriString();
        final ResponseEntity<String> responseEntity = FunctionalTestHelper.get(url);
        JsonTestUtil.assertEquals(responseEntity, getClass(), "DistributionOrderStatsController/testGetGraphsInfo.json");
    }

    @DisplayName("Проверка получения корректной статистики")
    @Test
    @DbUnitDataSet(
            dataSource = ClickHouseTestConfig.DATA_SOURCE,
            before = "DistributionOrderStatsController/distribution_stats.csv")
    void testGetStatsClicks() {
        final String url = UriComponentsBuilder.fromHttpUrl(baseUrl()).path("/distribution/stats/clicks")
                .queryParam("clid", 1L)
                .queryParam("vids", "1A")
                .queryParam("dateStart", LocalDate.of(2020, 1, 2))
                .queryParam("dateEnd", LocalDate.of(2020, 1, 3))
                .toUriString();
        final ResponseEntity<String> responseEntity = FunctionalTestHelper.get(url);
        JsonTestUtil.assertEquals(responseEntity, getClass(), "DistributionOrderStatsController/testGetStatsClicks.json");
    }

    @DisplayName("Проверка получения заказа по номеру из таблицы нового формата 2")
    @Test
    @DbUnitDataSet(
            dataSource = ClickHouseTestConfig.DATA_SOURCE,
            before = "DistributionOrderStatsController/distribution_order_stats_by_id_promocode.csv")
    void testGetOrderStatsByIdWithPromocode() {
        String url = getDistributionOrderStatsCommonUriBuilder("orderbyid", 123445L)
                .queryParam("orderId", 1001L)
                .queryParam("total", true).toUriString();
        final ResponseEntity<String> responseEntity = FunctionalTestHelper.get(url);
        JsonTestUtil.assertEquals(
                responseEntity,
                getClass(),
                "DistributionOrderStatsController/testGetOrderByIdWithPromocode.json"
        );
    }

    @DisplayName("Проверка получения заказа по номеру из таблицы нового формата")
    @Test
    @DbUnitDataSet(
            dataSource = ClickHouseTestConfig.DATA_SOURCE,
            before = "DistributionOrderStatsController/distribution_order_stats_by_id.csv")
    void testGetOrderStatsById() {
        String url = getDistributionOrderStatsCommonUriBuilder("orderbyid", 11111L)
                .queryParam("orderId", 55555L)
                .queryParam("total", true).toUriString();
        final ResponseEntity<String> responseEntity = FunctionalTestHelper.get(url);
        JsonTestUtil.assertEquals(responseEntity, getClass(), "DistributionOrderStatsController/testGetOrderStatsById.json");
    }

    @DisplayName("Проверка получения заказа по номеру из таблицы нового формата на урле v2")
    @Test
    @DbUnitDataSet(
            dataSource = ClickHouseTestConfig.DATA_SOURCE,
            before = "DistributionOrderStatsController/distribution_order_stats_by_id.csv")
    void testGetOrderStatsByIdV2Path() {
        String url = getV2UriBuilder("orderbyid", 11111L)
                .queryParam("orderId", 55555L)
                .queryParam("total", true).toUriString();
        final ResponseEntity<String> responseEntity = FunctionalTestHelper.get(url);
        JsonTestUtil.assertEquals(responseEntity, getClass(), "DistributionOrderStatsController/testGetOrderStatsById.json");
    }

    private String getDistributionOrdersV2Url(boolean total) {
        return getDistributionOrderStatsCommonUriBuilder("orders/v2", 1L)
                .queryParam("count", 10)
                .queryParam("total", total)
                .queryParam("sortingField", SortingField.ORDER_ID)
                .queryParam("sortingOrder", SortingOrder.ASC)
                .toUriString();
    }

    private String getDistributionOrderStatsV2Url(boolean total) {
        return getDistributionOrderStatsCommonUriBuilder("orders/v2", 1L)
                .queryParam("count", 10)
                .queryParam("total", total)
                .queryParam("sortingField", SortingField.ORDER_ID)
                .queryParam("sortingOrder", SortingOrder.ASC)
                .toUriString();
    }

    private String getDistributionOrderStatsUrl(boolean total) {
        return getDistributionOrderStatsUriBuilder(1L)
                .queryParam("total", total)
                .queryParam("sortingField", SortingField.ORDER_ID)
                .queryParam("sortingOrder", SortingOrder.ASC)
                .toUriString();
    }

    private UriComponentsBuilder getDistributionOrderStatsUriBuilder(Long... clids) {
        return getDistributionOrderStatsCommonUriBuilder("orders", clids)
                .queryParam("count", 10);
    }

    private UriComponentsBuilder getDistributionOrderStatsV2UriBuilder(Long... clids) {
        return getDistributionOrderStatsCommonUriBuilder("orders/v2", clids)
                .queryParam("count", 10);
    }

    private String getDistributionOrderStatsCommonUrl(String methodPath, Long... clids) {
        return getDistributionOrderStatsCommonUriBuilder(methodPath, clids)
                .toUriString();
    }

    private UriComponentsBuilder getDistributionOrderStatsCommonUriBuilder(String methodPath, Long... clids) {
        return UriComponentsBuilder.fromHttpUrl(baseUrl())
                .path("/distribution/" + methodPath)
                .queryParam("clids", (Object[]) clids);
    }

    private UriComponentsBuilder getV2UriBuilder(String methodPath, Long... clids) {
        return UriComponentsBuilder.fromHttpUrl(baseUrl())
                .path("/v2/distribution/" + methodPath)
                .queryParam("clids", (Object[]) clids);
    }
}
