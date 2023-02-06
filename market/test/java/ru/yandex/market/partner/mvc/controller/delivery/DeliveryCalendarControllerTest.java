package ru.yandex.market.partner.mvc.controller.delivery;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.CheckouterShopApi;
import ru.yandex.market.checkout.checkouter.shop.ShopMetaData;
import ru.yandex.market.checkout.checkouter.shop.shipment.ShipmentDateCalculationRule;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DbUnitDataSet(before = "DeliveryCalendarControllerTest.before.csv")
public class DeliveryCalendarControllerTest extends FunctionalTest {

    private static final LocalDate LOCAL_DATE = LocalDate.parse("2020-12-20");

    @Autowired
    private CheckouterAPI checkouterClient;

    @Autowired
    private CheckouterShopApi checkouterShopApi;

    @Autowired
    private Clock clock;

    @BeforeEach
    public void setup() {
        when(checkouterClient.shops()).thenReturn(checkouterShopApi);
        when(checkouterShopApi.getShopData(anyLong())).thenReturn(ShopMetaData.DEFAULT);

        Clock fixedClock = Clock.fixed(LOCAL_DATE.atStartOfDay(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault());
        when(clock.instant()).thenReturn(fixedClock.instant());
        when(clock.getZone()).thenReturn(fixedClock.getZone());
    }

    @Test
    void getDeliveryCalendar() {
        ResponseEntity<String> response =
                FunctionalTestHelper.get(baseUrl +
                        "/deliveryCalendar" +
                        "?datasourceId=101" +
                        "&begin_date=2020-10-10" +
                        "&end_date=2020-12-10" +
                        "&delivery_service_id=505");

        JsonTestUtil.assertEquals(
                response,
                "[\n" +
                        "  {\n" +
                        "    \"id\": 101,\n" +
                        "    \"type\": \"SHOP_SERVICE_DELIVERY\",\n" +
                        "    \"ownerId\": -1,\n" +
                        "    \"properties\": {\n" +
                        "      \"availableOnHoliday\": true\n" +
                        "    },\n" +
                        "    \"period\": {\n" +
                        "      \"beginDate\": {\n" +
                        "        \"year\": 2020,\n" +
                        "        \"month\": \"OCTOBER\",\n" +
                        "        \"chronology\": {\n" +
                        "          \"calendarType\": \"iso8601\",\n" +
                        "          \"id\": \"ISO\"\n" +
                        "        },\n" +
                        "        \"dayOfWeek\": \"SATURDAY\",\n" +
                        "        \"era\": \"CE\",\n" +
                        "        \"dayOfYear\": 284,\n" +
                        "        \"leapYear\": true,\n" +
                        "        \"monthValue\": 10,\n" +
                        "        \"dayOfMonth\": 10\n" +
                        "      },\n" +
                        "      \"endDate\": {\n" +
                        "        \"year\": 2020,\n" +
                        "        \"month\": \"DECEMBER\",\n" +
                        "        \"chronology\": {\n" +
                        "          \"calendarType\": \"iso8601\",\n" +
                        "          \"id\": \"ISO\"\n" +
                        "        },\n" +
                        "        \"dayOfWeek\": \"THURSDAY\",\n" +
                        "        \"era\": \"CE\",\n" +
                        "        \"dayOfYear\": 345,\n" +
                        "        \"leapYear\": true,\n" +
                        "        \"monthValue\": 12,\n" +
                        "        \"dayOfMonth\": 10\n" +
                        "      },\n" +
                        "      \"dayCount\": 61\n" +
                        "    },\n" +
                        "    \"customDays\": [\n" +
                        "      {\n" +
                        "        \"date\": {\n" +
                        "          \"year\": 2020,\n" +
                        "          \"month\": \"NOVEMBER\",\n" +
                        "          \"chronology\": {\n" +
                        "            \"calendarType\": \"iso8601\",\n" +
                        "            \"id\": \"ISO\"\n" +
                        "          },\n" +
                        "          \"dayOfWeek\": \"WEDNESDAY\",\n" +
                        "          \"era\": \"CE\",\n" +
                        "          \"dayOfYear\": 309,\n" +
                        "          \"leapYear\": true,\n" +
                        "          \"monthValue\": 11,\n" +
                        "          \"dayOfMonth\": 4\n" +
                        "        },\n" +
                        "        \"type\": \"DELIVERY_HOLIDAY\"\n" +
                        "      },\n" +
                        "      {\n" +
                        "        \"date\": {\n" +
                        "          \"year\": 2020,\n" +
                        "          \"month\": \"NOVEMBER\",\n" +
                        "          \"chronology\": {\n" +
                        "            \"calendarType\": \"iso8601\",\n" +
                        "            \"id\": \"ISO\"\n" +
                        "          },\n" +
                        "          \"dayOfWeek\": \"TUESDAY\",\n" +
                        "          \"era\": \"CE\",\n" +
                        "          \"dayOfYear\": 315,\n" +
                        "          \"leapYear\": true,\n" +
                        "          \"monthValue\": 11,\n" +
                        "          \"dayOfMonth\": 10\n" +
                        "        },\n" +
                        "        \"type\": \"DELIVERY_HOLIDAY\"\n" +
                        "      }\n" +
                        "    ],\n" +
                        "    \"shopId\": -1,\n" +
                        "    \"weekHolidays\": [\n" +
                        "      \"SATURDAY\",\n" +
                        "      \"SUNDAY\"\n" +
                        "    ]\n" +
                        "  }\n" +
                        "]");
    }

    @Test
    @DbUnitDataSet(after = "DeliveryCalendarControllerTest.after.update.csv")
    void updateDeliveryCalendar() {
        ResponseEntity<String> response =
                FunctionalTestHelper.get(
                        baseUrl +
                                "/deliveryCalendarUpdate" +
                                "?datasource_id=101" +
                                "&begin_date=3020-10-10" +
                                "&end_date=3020-12-10" +
                                "&week_holidays=1,2" +
                                "&custom_days=3020-11-20_0,3020-11-10_1" +
                                "&delivery_service_id=48735");

        JSONAssert.assertEquals("[\"OK\"]",
                new JSONObject(response.getBody()).getJSONArray("result").toString(), JSONCompareMode.LENIENT);
    }

    @Test
    @DbUnitDataSet(before = "DeliveryCalendarControllerTest.before.updateDeliveryCalendarCheckRuleUpdated.csv")
    void updateDeliveryCalendarCheckRuleUpdated() {
        ResponseEntity<String> response =
                FunctionalTestHelper.get(
                        baseUrl +
                                "/deliveryCalendarUpdate" +
                                "?datasource_id=201" +
                                "&begin_date=3020-10-10" +
                                "&end_date=3020-12-10" +
                                "&week_holidays=1,2" +
                                "&custom_days=3020-11-20_0,3020-11-10_1");

        JSONAssert.assertEquals("[\"OK\"]",
                new JSONObject(response.getBody()).getJSONArray("result").toString(), JSONCompareMode.LENIENT);

        ArgumentCaptor<ShipmentDateCalculationRule> argument = ArgumentCaptor.forClass(ShipmentDateCalculationRule.class);

        verify(checkouterShopApi).saveShipmentDateCalculationRules(eq(201L), argument.capture());

        List<LocalDate> expectedHolidays = initHolidays();
        List<LocalDate> actualHolidays = argument.getValue().getHolidays();
        assertEquals(expectedHolidays.size(), actualHolidays.size());
        assertTrue(actualHolidays.containsAll(expectedHolidays));
    }

    private List<LocalDate> initHolidays() {
        return List.of(
//                Нерабочие дни недели (weeklyHolidays)
                LocalDate.parse("2020-12-21"),
                LocalDate.parse("2020-12-22"),
                LocalDate.parse("2020-12-28"),
                LocalDate.parse("2020-12-29"),
                LocalDate.parse("2021-01-04"),
                LocalDate.parse("2021-01-05"),
                LocalDate.parse("2021-01-11"),
                LocalDate.parse("2021-01-12"),
                LocalDate.parse("2021-01-18"),
                LocalDate.parse("2021-01-19"),
//                Праздничные дни России
                LocalDate.parse("2021-01-10"),
                LocalDate.parse("2021-01-09"),
//                Выходные дни службы доставки
                LocalDate.parse("2021-01-16"),
                LocalDate.parse("2021-01-17"));
    }

    @Test
    @DbUnitDataSet(after = "DeliveryCalendarControllerTest.after.update.csv")
    void postUpdateDeliveryCalendar() {
        String request =
                "{\n" +
                        "  \"beginDate\": \"3020-10-10\",\n" +
                        "  \"endDate\": \"3020-12-10\",\n" +
                        "  \"deliveryServiceId\": 48735,\n" +
                        "  \"weeklyHolidays\": [1,2],\n" +
                        "  \"customDays\": [\"3020-11-20_0\",\"3020-11-10_1\"],\n" +
                        "  \"availableOnHoliday\": \"true\"" +
                        "}";

        FunctionalTestHelper.post(baseUrl + "/deliveryCalendarUpdate?partner_id=101", request);
    }
}
