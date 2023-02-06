package ru.yandex.market.api.partner.controllers.campaign.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.api.partner.controllers.serialization.BaseOldSerializationTest;
import ru.yandex.market.core.calendar.CalendarType;
import ru.yandex.market.core.calendar.DatePeriod;
import ru.yandex.market.core.calendar.Day;
import ru.yandex.market.core.calendar.DayType;
import ru.yandex.market.core.delivery.DeliverySourceType;
import ru.yandex.market.core.delivery.calendar.DeliveryCalendar;
import ru.yandex.market.core.calendar.CalendarProperties;
import ru.yandex.market.core.geobase.model.RegionType;

import static java.util.Arrays.asList;

/**
 * @author zoom
 */
class SettingsSerializationTest extends BaseOldSerializationTest {

    @Test
    void shouldSerializeEmptyFields() {
        Settings obj = new Settings();
        testSerialization(obj,
                "{\n" +
                        "  \"shopName\": null,\n" +
                        "  \"countryRegion\": null,\n" +
                        "  \"isOnline\": null,\n" +
                        "  \"showInContext\": null,\n" +
                        "  \"showInSnippets\": null,\n" +
                        "  \"showInPremium\": null,\n" +
                        "  \"useOpenStat\": null\n" +
                        "}",
                "<settings/>");
    }

    @Test
    void shouldSerializeFilledFields() {
        Settings obj = new Settings();
        obj.setCountryRegion(1);
        obj.setOnline(true);
        obj.setShopName("shop_name");
        obj.setShowInContext(true);
        obj.setShowInPremium(false);
        obj.setShowInSnippets(true);
        obj.setUseOpenStat(false);
        DatePeriod period = DatePeriod.of(LocalDate.of(2017, 1, 2), 50);
        List<LocalDate> totalDeliveryHolidays = asList(LocalDate.of(2017, 1, 14), LocalDate.of(2017, 2, 3));
        DeliveryCalendar localCalendar =
                new DeliveryCalendar(
                        CalendarType.SHOP_DELIVERY,
                        711,
                        744,
                        new CalendarProperties(true),
                        Collections.singletonList(DayOfWeek.FRIDAY),
                        period,
                        asList(
                                new Day(LocalDate.of(2017, 1, 13), DayType.DELIVERY_WORKDAY),
                                new Day(LocalDate.of(2017, 1, 14), DayType.DELIVERY_HOLIDAY)));
        Settings.DeliverySchedule deliverySchedule = new Settings.DeliverySchedule(localCalendar, totalDeliveryHolidays);
        obj.setLocalRegion(new Settings.LocalRegion(225, "Россия", RegionType.COUNTRY, DeliverySourceType.YML, deliverySchedule));
        testSerialization(obj,
                "{\n" +
                        "  \"shopName\": \"shop_name\",\n" +
                        "  \"countryRegion\": 1,\n" +
                        "  \"isOnline\": true,\n" +
                        "  \"showInContext\": true,\n" +
                        "  \"showInSnippets\": true,\n" +
                        "  \"showInPremium\": false,\n" +
                        "  \"useOpenStat\": false,\n" +
                        "  \"localRegion\": {\n" +
                        "    \"id\": 225,\n" +
                        "    \"name\": \"Россия\",\n" +
                        "    \"type\": \"COUNTRY\",\n" +
                        "    \"deliveryOptionsSource\": \"YML\",\n" +
                        "    \"delivery\": {\n" +
                        "      \"schedule\": {\n" +
                        "        \"availableOnHolidays\": true,\n" +
                        "        \"period\": {\n" +
                        "          \"fromDate\": \"02-01-2017\",\n" +
                        "          \"toDate\": \"20-02-2017\"\n" +
                        "        },\n" +
                        "        \"weeklyHolidays\": [\n" +
                        "          5\n" +
                        "        ],\n" +
                        "        \"customWorkingDays\": [\n" +
                        "          \"13-01-2017\"\n" +
                        "        ],\n" +
                        "        \"customHolidays\": [\n" +
                        "          \"14-01-2017\"\n" +
                        "        ],\n" +
                        "        \"totalHolidays\": [\n" +
                        "          \"14-01-2017\",\n" +
                        "          \"03-02-2017\"\n" +
                        "        ]\n" +
                        "      }\n" +
                        "    }\n" +
                        "  }\n" +
                        "}",
                "<settings shop-name=\"shop_name\"\n" +
                        "          country-region=\"1\"\n" +
                        "          is-online=\"true\"\n" +
                        "          show-in-context=\"true\"\n" +
                        "          show-in-snippets=\"true\"\n" +
                        "          show-in-premium=\"false\"\n" +
                        "          use-open-stat=\"false\">\n" +
                        "    <local-region id=\"225\" name=\"Россия\" type=\"COUNTRY\" delivery-options-source=\"YML\">\n" +
                        "        <delivery>\n" +
                        "            <schedule available-on-holidays=\"true\">\n" +
                        "                <period from-date=\"02-01-2017\" to-date=\"20-02-2017\"/>\n" +
                        "                <weekly-holidays>\n" +
                        "                    <dow>5</dow>\n" +
                        "                </weekly-holidays>\n" +
                        "                <custom-working-days>\n" +
                        "                    <date>13-01-2017</date>\n" +
                        "                </custom-working-days>\n" +
                        "                <custom-holidays>\n" +
                        "                    <date>14-01-2017</date>\n" +
                        "                </custom-holidays>\n" +
                        "                <total-holidays>\n" +
                        "                    <date>14-01-2017</date>\n" +
                        "                    <date>03-02-2017</date>\n" +
                        "                </total-holidays>\n" +
                        "            </schedule>\n" +
                        "        </delivery>\n" +
                        "    </local-region>\n" +
                        "</settings>");
    }
}
