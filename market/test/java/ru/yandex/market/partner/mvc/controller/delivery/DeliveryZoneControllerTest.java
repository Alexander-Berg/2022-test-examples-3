package ru.yandex.market.partner.mvc.controller.delivery;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;
import ru.yandex.misc.io.http.UriBuilder;

import static ru.yandex.market.common.test.util.JsonTestUtil.assertEquals;

/**
 * Тесты для {@link DeliveryZoneController}.
 *
 * @author Vladislav Bauer
 */
class DeliveryZoneControllerTest extends FunctionalTest {

    private static final long DATASOURCE_ID = 1001L;
    private static final long DISTANCE_FROM = 15000L;
    private static final long DISTANCE_TO = 25000L;
    private static final long USER_ID = 123456;


    @Test
    @DisplayName("Данные отсутсвуют")
    void testEmptyData() {
        final ResponseEntity<String> response = getDeliveryZoneRegions(null, null);
        assertEquals(response, "[]");
    }

    @Test
    @DbUnitDataSet(before = "DeliveryZoneControllerTest.before.csv")
    @DisplayName("Не установлены фильтры расстояния от/до, нужно вернуть все регионы зоны доставки")
    void testWithoutDistanceFilters() {
        final ResponseEntity<String> response = getDeliveryZoneRegions(null, null);
        assertEquals(response, "[" +
                "{\"id\":10715,\"name\":\"Апрелевка\",\"parentId\":98597}," +
                "{\"id\":10716,\"name\":\"Балашиха\",\"parentId\":116705}," +
                "{\"id\":117174,\"name\":\"Анискино\",\"parentId\":121009}" +
                "]");
    }

    @Test
    @DbUnitDataSet(before = "DeliveryZoneControllerTest.before.csv")
    @DisplayName("Установлены оба фильтра расстояния от/до")
    void testBothDistanceFilters() {
        final ResponseEntity<String> response = getDeliveryZoneRegions(DISTANCE_FROM, DISTANCE_TO);
        assertEquals(response, "[" +
                "{\"id\":117174,\"name\":\"Анискино\",\"parentId\":121009}" +
                "]");
    }

    @Test
    @DbUnitDataSet(before = "DeliveryZoneControllerTest.before.csv")
    @DisplayName("Установлен фильтр расстояния 'от'")
    void testFromDistanceFilter() {
        final ResponseEntity<String> response = getDeliveryZoneRegions(DISTANCE_FROM, null);
        assertEquals(response, "[" +
                "{\"id\":10715,\"name\":\"Апрелевка\",\"parentId\":98597}," +
                "{\"id\":117174,\"name\":\"Анискино\",\"parentId\":121009}" +
                "]");
    }

    @Test
    @DbUnitDataSet(before = "DeliveryZoneControllerTest.before.csv")
    @DisplayName("Установлен фильтр расстояния 'до'")
    void testToDistanceFilter() {
        final ResponseEntity<String> response = getDeliveryZoneRegions(null, DISTANCE_TO);
        assertEquals(response, "[" +
                "{\"id\":10716,\"name\":\"Балашиха\",\"parentId\":116705}," +
                "{\"id\":117174,\"name\":\"Анискино\",\"parentId\":121009}" +
                "]");
    }

    private ResponseEntity<String> getDeliveryZoneRegions(final Long from, final Long to) {
        final UriBuilder uriBuilder = UriBuilder.cons(baseUrl + "/delivery-zone/regions")
                .addParam("_user_id", USER_ID)
                .addParam("datasource_id", DATASOURCE_ID);

        if (from != null) {
            uriBuilder.addParam("from", from);
        }
        if (to != null) {
            uriBuilder.addParam("to", to);
        }

        return FunctionalTestHelper.get(uriBuilder.toUrl());
    }

}
