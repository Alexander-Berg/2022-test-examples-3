package ru.yandex.market.vendors.analytics.platform.controller.billing.management.change;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.vendors.analytics.core.model.billing.TariffType;
import ru.yandex.market.vendors.analytics.platform.controller.billing.BalanceFunctionalTest;

import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author antipov93.
 */
@DbUnitDataSet(before = "../../Balance.common.before.csv")
class VendorPlacementControllerChangeTariffTest extends BalanceFunctionalTest {

    @Test
    @DisplayName("Апгрейд тарифа в середине месяца")
    @DbUnitDataSet(
            before = "../../OfferClock.2020-04-16.before.csv",
            after = "VendorPlacementControllerChangeTariffTest.changeTariffToFullMedium.after.csv"
    )
    void changeTariffToFullMedium() {
        long uid = 11;
        mockBalance(1001, 4_210_000);
        mockChangeDynamicCost(1001, uid, 3_805_000);
        executeChangeTariffRequest(1, 90796, TariffType.FULL, uid);
    }

    @Test
    @DisplayName("Апгрейд тарифа в начале месяца")
    @DbUnitDataSet(
            before = "../../OfferClock.2020-05-01.before.csv",
            after = "VendorPlacementControllerChangeTariffTest.changeTariffToFullStart.after.csv"
    )
    void changeTariffToFullStart() {
        long uid = 11;
        mockBalance(1001, 10_000_000);
        mockChangeDynamicCost(1001, uid, 3_885_000);
        executeChangeTariffRequest(1, 6290268, TariffType.FULL, uid);
    }

    @Test
    @DisplayName("Нельзя изменить неактивный тариф")
    @DbUnitDataSet(before = "../../OfferClock.2020-04-16.before.csv", after = "../../Balance.common.before.csv")
    void cantChangeNoActiveTariffToFullMedium() {
        long uid = 11;
        var exception = assertThrows(HttpClientErrorException.class,
                () -> executeChangeTariffRequest(1, 7286125, TariffType.FULL, uid));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        String expected = "{  \n" +
                "   \"code\":\"HAS_NO_ACTIVE_TARIFF\",\n" +
                "   \"message\":\"${json-unit.ignore}\"\n" +
                "}";
        assertJsonEquals(expected, exception.getResponseBodyAsString());
    }

    @Test
    @DisplayName("Тариф уже полный")
    @DbUnitDataSet(before = "../../OfferClock.2020-04-16.before.csv", after = "../../Balance.common.before.csv")
    void tariffAlreadyFull() {
        long uid = 11;
        var exception = assertThrows(HttpClientErrorException.class,
                () -> executeChangeTariffRequest(1, 91491, TariffType.FULL, uid));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        String expected = "{  \n" +
                "   \"code\":\"TARIFF_TYPES_EQUAL\",\n" +
                "   \"message\":\"${json-unit.ignore}\"\n" +
                "}";
        assertJsonEquals(expected, exception.getResponseBodyAsString());
    }

    @Test
    @DisplayName("Успешный даунгрейд тарифа")
    @DbUnitDataSet(
            before = "../../OfferClock.2020-04-16.before.csv",
            after = "VendorPlacementControllerDisableTest.successfullyDowngradeCategory.after.csv"
    )
    void successfullyDowngradeCategory() {
        executeChangeTariffRequest(1, 91491, TariffType.BASIC, 11);
    }

    @Test
    @DisplayName("Тариф уже базовый")
    @DbUnitDataSet(before = "../../OfferClock.2020-04-16.before.csv", after = "../../Balance.common.before.csv")
    void tariffAlreadyBasic() {
        long uid = 11;
        var exception = assertThrows(HttpClientErrorException.class,
                () -> executeChangeTariffRequest(1, 90796, TariffType.BASIC, uid));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        String expected = "{  \n" +
                "   \"code\":\"TARIFF_TYPES_EQUAL\",\n" +
                "   \"message\":\"${json-unit.ignore}\"\n" +
                "}";
        assertJsonEquals(expected, exception.getResponseBodyAsString());
    }

    @Test
    @DisplayName("Тариф уже сменён на базовый со следующего месяца")
    @DbUnitDataSet(before = "../../OfferClock.2020-04-16.before.csv", after = "../../Balance.common.before.csv")
    void tariffAlreadyChangeToBasic() {
        long uid = 11;
        var exception = assertThrows(HttpClientErrorException.class,
                () -> executeChangeTariffRequest(1, 6290268, TariffType.BASIC, uid));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        String expected = "{  \n" +
                "   \"code\":\"TARIFF_TYPE_ALREADY_CHANGED\",\n" +
                "   \"message\":\"${json-unit.ignore}\"\n" +
                "}";
        assertJsonEquals(expected, exception.getResponseBodyAsString());
    }

    @Test
    @DisplayName("Запрещён переход на базовый тариф")
    @DbUnitDataSet(before = "../../OfferClock.2020-04-16.before.csv", after = "../../Balance.common.before.csv")
    void forbiddenChangeToBasic() {
        long uid = 11;
        var exception = assertThrows(HttpClientErrorException.class,
                () -> executeChangeTariffRequest(1, 91013, TariffType.BASIC, uid));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        String expected = "{  \n" +
                "   \"code\":\"FORBIDDEN_TO_SWITCH_TO_BASE_TARIFF\",\n" +
                "   \"message\":\"${json-unit.ignore}\"\n" +
                "}";
        assertJsonEquals(expected, exception.getResponseBodyAsString());
    }

    @Test
    @DbUnitDataSet(before = "../../OfferClock.2020-04-16.before.csv", after = "../../Balance.common.before.csv")
    @DisplayName("Последний месяц ещё не был обиллен, изменение тарифа недоступно")
    void lastMonthNotBilled() {
        long uid = 11;
        var exception = assertThrows(HttpClientErrorException.class,
                () -> executeChangeTariffRequest(2, 6290268, TariffType.BASIC, uid));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        String expected = "{  \n" +
                "   \"code\":\"BILLING_HAS_NOT_BEEN_PROCESSED_YET\",\n" +
                "   \"message\":\"${json-unit.ignore}\"\n" +
                "}";
        assertJsonEquals(expected, exception.getResponseBodyAsString());
    }


    private void executeChangeTariffRequest(long vendorId, long categoryId, TariffType tariffType, long uid) {
        var url = UriComponentsBuilder.fromHttpUrl(baseUrl())
                .pathSegment("vendors", "{vendorId}", "categories", "{categoryId}", "type", "{tariffType}")
                .queryParam("uid", uid)
                .buildAndExpand(vendorId, categoryId, tariffType)
                .toUriString();
        FunctionalTestHelper.put(url, null);
    }
}