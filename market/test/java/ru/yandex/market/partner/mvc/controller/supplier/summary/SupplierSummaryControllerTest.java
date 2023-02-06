package ru.yandex.market.partner.mvc.controller.supplier.summary;

import java.util.TimeZone;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

/**
 * Тесты для ручек сводки {@link SupplierSummaryController}.
 */
@DbUnitDataSet(before = "SupplierSummaryControllerTest.before.csv")
class SupplierSummaryControllerTest extends FunctionalTest {

    private static final int MILLIS_IN_HOUR = 3_600_000;

    @DisplayName("Для не существующего поставщика")
    @Test
    void testSupplierSummaryNotFound() {
        HttpClientErrorException response = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.get(getUrl(), 10101L)
        );
        Assertions.assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @DisplayName("Сводка состояния склада для случая, когда у поставщика все заполнено")
    @Test
    @DbUnitDataSet(before = "supplierSummary.csv")
    void testStockStorageSummary() {
        ResponseEntity<String> response = FunctionalTestHelper.get(getUrl() + "/stock-storage", 10102L);
        String expected =
                "{\n" +
                        "  \"total\": {\n" +
                        "    \"offerCount\": 38,\n" +
                        "    \"skuCount\": 4\n" +
                        "  },\n" +
                        "  \"available\": {\n" +
                        "    \"offerCount\": 10,\n" +
                        "    \"skuCount\": 1\n" +
                        "  },\n" +
                        "  \"defect\": {\n" +
                        "    \"offerCount\": 5,\n" +
                        "    \"skuCount\": 1\n" +
                        "  },\n" +
                        "  \"expired\": {\n" +
                        "    \"offerCount\": 3,\n" +
                        "    \"skuCount\": 1\n" +
                        "  },\n" +
                        "  \"paidStorage\": {\n" +
                        "    \"offerCount\":20,\n" +
                        "    \"skuCount\":2\n" +
                        "  },\n" +
                        "  \"debtAmount\": {\n" +
                        "   \"currentPeriodDebt\":80\n" +
                        "  }\n" +
                        "}";
        JsonTestUtil.assertEquals(response, expected);
    }

    @DisplayName("Состояние скрытий у поставщика")
    @Test
    @DbUnitDataSet(before = "supplierSummary.csv")
    void hiddenSummaryInfo() {
        ResponseEntity<String> response = FunctionalTestHelper.get(getUrl() + "/hiddens", 10102L);
        JsonTestUtil.assertEquals(response, "{ \"offerCount\": 0, \"skuCount\": 0 }");
    }

    @DisplayName("Сводка по ценам, когда неаплоадный фид")
    @Test
    void testFeedSummaryUrlFeed() {
        ResponseEntity<String> response = FunctionalTestHelper.get(getUrl() + "/feed", 10102L);
        String expected = "" +
                "{ \"offerCount\":34 }";
        JsonTestUtil.assertEquals(response, expected);
    }

    @DisplayName("Сводка по ценам, когда у поставщика все заполнено")
    @Test
    @DbUnitDataSet(before = "supplierSummary.csv")
    void testFeedSummary() {
        ResponseEntity<String> response = FunctionalTestHelper.get(getUrl() + "/feed", 10102L);
        String expected = "{\n" +
                "    \"offerCount\": 34,\n" +
                "    \"upload\": {\n" +
                "      \"fileName\": \"file.name\",\n" +
                "      \"uploadDateTime\": \"" + getRegionFormattedDateTime() + "\"\n" +
                "    }\n" +
                "}";
        JsonTestUtil.assertEquals(response, expected);
    }

    @DisplayName("Сводка по ассортименту, когда у поставщика все заполнено")
    @Test
    @DbUnitDataSet(before = "supplierSummary.csv")
    void testAssortmentSummary() {
        ResponseEntity<String> response = FunctionalTestHelper.get(getUrl() + "/assortment", 10102L);
        String expected = "{\n" +
                "    \"upload\": {\n" +
                "      \"fileName\": \"file.name\",\n" +
                "      \"uploadDateTime\": \"" + getRegionFormattedDateTime() + "\"\n" +
                "    },\n" +
                "    \"offers\": {\n" +
                "      \"total\": 1,\n" +
                "      \"approved\": 1,\n" +
                "      \"onModeration\": 1\n" +
                "    }\n" +
                "}";
        JsonTestUtil.assertEquals(response, expected);
    }

    @DisplayName("Рейтинг поставщика")
    @Test
    void testRatingSummary() {
        ResponseEntity<String> response = FunctionalTestHelper.get(getUrl() + "/rating", 10102L);
        String expected = "{\"ratingValue\": 5.0}";
        JsonTestUtil.assertEquals(response, expected);
    }

    private String getUrl() {
        return baseUrl + "/suppliers/{campaignId}/summary";
    }

    /**
     * Дата с оффсетом в зависимости от timezone под которой прогоняется тест.
     */
    private String getRegionFormattedDateTime() {
        int zoneOffset = TimeZone.getDefault().getRawOffset() / MILLIS_IN_HOUR;
        return "2018-01-07T00:00:00+" + (zoneOffset > 9 ? String.valueOf(zoneOffset) : ("0" + zoneOffset)) + ":00";
    }

}
