package ru.yandex.market.vendors.analytics.platform.controller.billing.management.add;

import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendors.analytics.core.model.billing.TariffType;
import ru.yandex.market.vendors.analytics.core.utils.DateUtils;

import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * @author antipov93.
 */
@DbUnitDataSet(before = "../../without_part_month_discount.csv")
public class VendorPlacementControllerAddTest extends AbstractAddTest {

    @Test
    @DisplayName("Удачное подключение категорий в начале месяца")
    @DbUnitDataSet(before = "../../OfferClock.2020-05-01.before.csv")
    @DbUnitDataSet(after = "VendorPlacementControllerAddTest.successfullyAddCategoriesStart.after.csv")
    void successfullyAddCategoriesStart() {
        when(clock.instant()).thenReturn(DateUtils.toInstant(LocalDate.of(2020, 4, 16)));

        long uid = 11;
        mockBalance(1001, 10_000_000);
        mockChangeDynamicCost(1001, uid, 3957000);
        var body = addCategoriesRequest(Map.of(7286125L, TariffType.FULL, 90555L, TariffType.BASIC));
        String response = executeAddCategoriesRequest(1, uid, body);
        String expected = loadFromFile("VendorPlacementControllerAddTest.successfullyAddCategoriesStart.response.json");
        assertJsonEquals(expected, response);
    }

    @Test
    @DisplayName("Удачное подключение категорий в середине месяца")
    @DbUnitDataSet(before = "../../OfferClock.2020-04-16.before.csv")
    @DbUnitDataSet(before = "VendorPlacementControllerAddTest.successfullyAddCategoriesMedium.before.csv")
    @DbUnitDataSet(after = "VendorPlacementControllerAddTest.successfullyAddCategoriesMedium.after.csv")
    void successfullyAddCategoriesMedium() {
        when(clock.instant()).thenReturn(DateUtils.toInstant(LocalDate.of(2020, 4, 16)));

        long uid = 11;
        mockBalance(1001, 10_000_000);
        mockChangeDynamicCost(1001, uid, 3972000);
        var body = addCategoriesRequest(Map.of(7286125L, TariffType.FULL, 90555L, TariffType.BASIC));
        var response = executeAddCategoriesRequest(1, uid, body);
        var expected = loadFromFile("VendorPlacementControllerAddTest.successfullyAddCategoriesMedium.response.json");
        assertJsonEquals(expected, response);
    }


    @Test
    @DisplayName("Не получилось подключить категории из-за нехватки средств")
    @DbUnitDataSet(before = "../../OfferClock.2020-04-10.before.csv")
    void addAlreadyExistCategory() {
        long uid = 11;
        var body = addCategoriesRequest(Map.of(91491L, TariffType.FULL));
        var exception = assertThrows(HttpClientErrorException.class, () -> executeAddCategoriesRequest(1, uid, body));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        String expected = "{  \n" +
                "   \"code\":\"CAN_NOT_ADD_ALREADY_ENABLED_CATEGORY\",\n" +
                "   \"message\":\"${json-unit.ignore}\"\n" +
                "}";
        assertJsonEquals(expected, exception.getResponseBodyAsString());
    }

    @Test
    @DisplayName("Попытка купить неизвестную категорию")
    @DbUnitDataSet(before = "../../OfferClock.2020-04-10.before.csv")
    void unknownCategoryPrice() {
        var body = addCategoriesRequest(Map.of(0L, TariffType.FULL));
        var exception = assertThrows(HttpClientErrorException.class, () -> executeAddCategoriesRequest(1, 11, body));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        String expected = "{\n" +
                "   \"code\":\"ENTITY_NOT_FOUND\",\n" +
                "   \"message\":\"${json-unit.ignore}\",\n" +
                "   \"entityId\": 0,\n" +
                "   \"entityType\":\"CATEGORY_PRICE\"\n" +
                "}";
        assertJsonEquals(expected, exception.getResponseBodyAsString());
    }

    @Test
    @DisplayName("Последний месяц ещё не был обиллен, добавление категорий недоступно")
    @DbUnitDataSet(before = "../../OfferClock.2020-04-10.before.csv")
    void lastMonthNotBilled() {
        var body = addCategoriesRequest(Map.of(91491L, TariffType.FULL));
        var exception = assertThrows(HttpClientErrorException.class, () -> executeAddCategoriesRequest(2, 11, body));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        String expected = "{\n" +
                "   \"code\":\"BILLING_HAS_NOT_BEEN_PROCESSED_YET\",\n" +
                "   \"message\":\"${json-unit.ignore}\"\n" +
                "}";
        assertJsonEquals(expected, exception.getResponseBodyAsString());
    }
}
