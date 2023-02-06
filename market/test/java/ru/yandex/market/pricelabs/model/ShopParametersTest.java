package ru.yandex.market.pricelabs.model;


import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.pricelabs.misc.Utils;
import ru.yandex.market.pricelabs.model.ShopParameters.Export;
import ru.yandex.market.pricelabs.model.ShopParameters.Marginality;
import ru.yandex.market.pricelabs.model.ShopParameters.Marginality.SourceType;
import ru.yandex.market.pricelabs.model.ShopParameters.StatOrders;
import ru.yandex.market.pricelabs.model.ShopParameters.StatOrders.AccessType;
import ru.yandex.market.pricelabs.model.ShopParameters.WorkingTime;
import ru.yandex.market.pricelabs.model.ShopParameters.WorkingTime.ShopOffType;
import ru.yandex.market.pricelabs.model.types.ShopStatsType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShopParametersTest {

    private static ShopParameters example;

    @BeforeAll
    static void init() {
        example = ShopParameters.fromJsonString(Utils.readResource("pricelabs/models/shop_settings.json"));
    }

    @Test
    void parseJson() {
        ShopParameters expect = new ShopParameters();
        Marginality marginality = expect.getMarginality();
        marginality.set_purchase_prices_enabled(true);
        marginality.setMargin(0);
        marginality.setSource_type(SourceType.CSV);
        marginality.setSource_csv_url("http://....e96.ru/prices/for-price-labs?access-token=...&regionId=...");
        marginality.set_marginality_strategy_enabled(true);
        marginality.setCoeff_conversion(0);
        marginality.setCoeff_phone(50);
        marginality.setCoeff_marketing(100);
        marginality.setMin_conversion_value(10);
        marginality.setMin_conversion_days(7);
        WorkingTime workingTime = expect.getWorking_time();
        workingTime.setDays(new HashMap<>() {{
            put("1", true);
            put("2", true);
            put("3", true);
            put("4", true);
            put("5", true);
            put("6", true);
            put("7", true);
        }});
        workingTime.setFrom("00:00");
        workingTime.setTo("24:00");
        workingTime.setShop_off(ShopOffType.DEFAULT);
        StatOrders statOrders = expect.getStat_orders();
        statOrders.set_enabled(true);
        statOrders.setType(ShopStatsType.GA);
        statOrders.setAccess_type(AccessType.INDIVIDUAL);
        statOrders.setYm_source_type("ym:s:UTMSource");
        statOrders.setYm_source_value("...");
        statOrders.setYm_offer_type("ym:s:UTMCampaign");
        statOrders.setGa_profile_id("0");
        statOrders.setGa_source_type("campaign");
        statOrders.setGa_source_value("...");
        statOrders.setGa_offer_type("keyword");
        expect.set_show_min_price_in_model(true);

        Export export = new Export();
        export.set_excel_enabled(true);
        export.set_csv_enabled(true);
        export.setCsv_delimeter(";");
        export.setCsv_enclosure("\"");
        export.setSort_type("price");
        export.setCompetitors(List.of());
        export.setCompetitor_codes(List.of());
        export.set_competitors_only_my_region(true);
        export.set_exclude_my_shop(true);
        export.set_show_shipping_cost(true);
        export.set_show_in_stock(true);

        expect.setExport(export);

        expect.set_minimal_bids(true);
        expect.set_pl2(true);
        assertEquals(expect, example);
    }

    @Test
    void testWorkingTimeWithoutParameters() {

        ShopParameters parameters = new ShopParameters();
        var time = parameters.getWorking_time();

        assertTrue(time.isWorkingNow(Utils.parseDateTimeAsInstant("2019-09-18T00:00:00")));
        assertTrue(time.isWorkingNow(Instant.ofEpochMilli(0)));
    }

    @Test
    void testWorkingTimeWithDefaultParameters() {
        var time = example.getWorking_time();
        assertTrue(time.isWorkingNow(Utils.parseDateTimeAsInstant("2019-09-18T00:00:00")));
        assertTrue(time.isWorkingNow(Utils.parseDateTimeAsInstant("2019-09-18T00:00:01")));
        assertTrue(time.isWorkingNow(Utils.parseDateTimeAsInstant("2019-09-17T23:59:59")));
        assertTrue(time.isWorkingNow(Utils.parseDateTimeAsInstant("2019-09-18T03:00:00")));
    }

    @ParameterizedTest
    @MethodSource("workingTimeFrom0Ranges")
    void testWorkingTimeFrom0(boolean expect, String instant) {
        var time = new WorkingTime();
        time.setFrom("00:00");
        time.setTo("22:00");
        time.setDays(Map.of("1", true, "2", true, "3", true, "4", true, "5", true));

        assertEquals(expect, time.isWorkingNow(Utils.parseDateTimeAsInstant(instant)));
    }

    @ParameterizedTest
    @MethodSource("workingTimeTo0Ranges")
    void testWorkingTimeTo0(boolean expect, String instant) {
        var time = new WorkingTime();
        time.setFrom("10:00");
        time.setTo("24:00");
        time.setDays(Map.of("1", true, "2", true, "3", true, "4", true, "5", true));

        assertEquals(expect, time.isWorkingNow(Utils.parseDateTimeAsInstant(instant)));
    }


    @ParameterizedTest
    @MethodSource("simpleRanges")
    void testSimpleWorkingTime(boolean expect, String instant) {
        var time = new WorkingTime();
        time.setFrom("10:00");
        time.setTo("22:00");
        time.setDays(Map.of("1", true, "2", true, "3", true, "4", true, "5", true));

        assertEquals(expect, time.isWorkingNow(Utils.parseDateTimeAsInstant(instant)));
    }

    @ParameterizedTest
    @MethodSource("complexRanges")
    void testComplexWorkingTime(boolean expect, String instant) {
        var time = new WorkingTime();
        time.setFrom("10:00");
        time.setTo("24:00+05:00");
        time.setDays(Map.of("1", true, "2", true, "3", true, "4", true, "5", true, "6", false, "7", false));

        assertEquals(expect, time.isWorkingNow(Utils.parseDateTimeAsInstant(instant)));
    }

    static Object[][] workingTimeFrom0Ranges() {
        // 0-22, с понедельника по пятницу
        return new Object[][]{
                {true, "2019-09-16T00:00:00"}, // понедельник
                {true, "2019-09-16T09:59:59"},
                {true, "2019-09-16T10:00:00"},
                {true, "2019-09-16T15:00:00"},
                {true, "2019-09-16T21:59:59"},
                {false, "2019-09-16T22:00:00"},

                {true, "2019-09-17T15:00:00"},
                {true, "2019-09-18T15:00:00"},
                {true, "2019-09-19T15:00:00"},
                {true, "2019-09-20T15:00:00"},

                {false, "2019-09-21T00:00:00"},  // суббота
                {false, "2019-09-21T15:00:00"},
                {false, "2019-09-21T23:59:59"},
                {false, "2019-09-22T15:00:00"}, // воскресенье
                {false, "2019-09-22T23:59:59"}
        };
    }

    static Object[][] workingTimeTo0Ranges() {
        // 10-23:59:59, с понедельника по пятницу
        return new Object[][]{
                {false, "2019-09-16T09:59:59"}, // понедельник
                {true, "2019-09-16T10:00:00"},
                {true, "2019-09-16T15:00:00"},
                {true, "2019-09-16T21:59:59"},
                {true, "2019-09-16T22:00:00"},
                {true, "2019-09-16T23:59:59"},

                {true, "2019-09-17T15:00:00"},
                {true, "2019-09-18T15:00:00"},
                {true, "2019-09-19T15:00:00"},
                {true, "2019-09-20T15:00:00"},
                {true, "2019-09-20T23:59:59"},

                {false, "2019-09-21T00:00:00"},  // суббота
                {false, "2019-09-21T15:00:00"},
                {false, "2019-09-21T23:59:59"},
                {false, "2019-09-22T15:00:00"}, // воскресенье
                {false, "2019-09-22T23:59:59"}
        };
    }

    static Object[][] simpleRanges() {
        // 10-22, с понедельника по пятницу
        return new Object[][]{
                {false, "2019-09-16T09:59:59"}, // понедельник
                {true, "2019-09-16T10:00:00"},
                {true, "2019-09-16T15:00:00"},
                {true, "2019-09-16T21:59:59"},
                {false, "2019-09-16T22:00:00"},

                {true, "2019-09-17T15:00:00"},
                {true, "2019-09-18T15:00:00"},
                {true, "2019-09-19T15:00:00"},
                {true, "2019-09-20T15:00:00"},

                {false, "2019-09-21T00:00:00"},  // суббота
                {false, "2019-09-21T15:00:00"},
                {false, "2019-09-21T23:59:59"},
                {false, "2019-09-22T15:00:00"}
        };
    }

    static Object[][] complexRanges() {
        // 10-05, с понедельника по пятницу
        return new Object[][]{
                {false, "2019-09-16T09:59:59"}, // понедельник
                {true, "2019-09-16T10:00:00"},
                {true, "2019-09-16T15:00:00"},
                {true, "2019-09-16T21:59:59"},
                {true, "2019-09-16T22:00:00"},
                {true, "2019-09-17T04:59:59"},
                {false, "2019-09-17T05:00:00"},

                {true, "2019-09-17T15:00:00"},
                {true, "2019-09-18T04:59:59"},
                {false, "2019-09-18T05:00:00"},

                {true, "2019-09-18T15:00:00"},
                {true, "2019-09-19T04:59:59"},
                {false, "2019-09-19T05:00:00"},

                {true, "2019-09-19T15:00:00"},
                {true, "2019-09-20T04:59:59"},
                {false, "2019-09-20T05:00:00"},

                {true, "2019-09-20T15:00:00"},
                {true, "2019-09-21T04:59:59"},
                {false, "2019-09-21T05:00:00"},

                {false, "2019-09-21T15:00:00"}, // суббота
                {false, "2019-09-22T04:59:59"},
                {false, "2019-09-22T05:00:00"},

                {false, "2019-09-22T15:00:00"},
                {false, "2019-09-23T04:59:59"},
                {false, "2019-09-23T05:00:00"}
        };
    }
}
