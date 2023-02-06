package ru.yandex.market.pricelabs.integration.api;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import ru.yandex.market.pricelabs.apis.LogOutput.PL1ExportConst;
import ru.yandex.market.pricelabs.exports.params.CsvParameters;
import ru.yandex.market.pricelabs.generated.server.pub.model.MaxRecommendationsResponse;
import ru.yandex.market.pricelabs.model.openapi.DateGroupTypeEnum;
import ru.yandex.market.pricelabs.model.openapi.OrderByPerExportPriceEnum;

import static ru.yandex.market.pricelabs.tms.processing.TmsTestUtils.filter;

public class PublicApiScenarios {

    private PublicApiScenarios() {
        //
    }

    static Object[][] supportedGroupByCombinations() {
        // String dateGroupType, Boolean aggregate, Boolean withTotals
        return new Object[][]{
                {DateGroupTypeEnum.days, true, false},
                {DateGroupTypeEnum.days, true, true},
                {DateGroupTypeEnum.days, false, true},
                {DateGroupTypeEnum.days, false, false},
                {null, false, true},
                {null, false, false},
        };
    }

    static Object[][] supportedGroupByCombinationsPerOffer() {
        // String dateGroupType, Boolean aggregate, Boolean aggregateByShop, Boolean withTotals
        return new Object[][]{
                {DateGroupTypeEnum.days, true, false, false},
                {DateGroupTypeEnum.days, true, true, false},
                {DateGroupTypeEnum.days, true, false, true},
                {DateGroupTypeEnum.days, true, true, true},
                {DateGroupTypeEnum.days, false, false, true},
                {DateGroupTypeEnum.days, false, true, true},
                {DateGroupTypeEnum.days, false, false, false},
                {DateGroupTypeEnum.days, false, true, false},
                {null, false, false, true},
                {null, false, true, true},
                {null, false, false, false},
                {null, false, true, false}
        };
    }

    static Object[][] exportPricesOrder() {
        return new Object[][]{
                {OrderByPerExportPriceEnum.bid, List.of(), List.of(),
                        "integration/api/exportPrices-full.csv",
                        "integration/api/exportPrices-full.xlsx"},
                {OrderByPerExportPriceEnum.price, List.of(), List.of(),
                        "integration/api/exportPrices-full-byprice.csv",
                        "integration/api/exportPrices-full-byprice.xlsx"},
                {OrderByPerExportPriceEnum.bid, List.of("Shop 2", "Shop 1"), List.of(),
                        "integration/api/exportPrices-full-bybid-shop.csv",
                        "integration/api/exportPrices-full-bybid-shop.xlsx"},
                {OrderByPerExportPriceEnum.price, List.of("Shop 2", "Shop 1"), List.of(),
                        "integration/api/exportPrices-full-byprice-shop.csv",
                        "integration/api/exportPrices-full-byprice-shop.xlsx"},
                {OrderByPerExportPriceEnum.bid, List.of("Магазин номер 2", "Магазин номер 1"), List.of(2, 1),
                        "integration/api/exportPrices-full-bybid-shop2.csv",
                        "integration/api/exportPrices-full-bybid-shop2.xlsx"},
        };
    }

    static Object[][] exportPricesS3() {
        return new Object[][]{
                {OrderByPerExportPriceEnum.bid, List.of(), List.of(),
                        true, PL1ExportConst.EXPORT_PRICES_SORT_BY_BID,
                        "integration/api/exportPrices-full-same-region.csv",
                        "integration/api/exportPrices-full-same-region.xlsx"},
                {OrderByPerExportPriceEnum.bid, List.of(), List.of(),
                        false, PL1ExportConst.EXPORT_PRICES_SORT_BY_BID,
                        "integration/api/exportPrices-full.csv",
                        "integration/api/exportPrices-full.xlsx"},
                {OrderByPerExportPriceEnum.price, List.of(), List.of(),
                        false, PL1ExportConst.EXPORT_PRICES_SORT_BY_PRICE,
                        "integration/api/exportPrices-full-byprice.csv",
                        "integration/api/exportPrices-full-byprice.xlsx"},
                {OrderByPerExportPriceEnum.bid, List.of("Shop 2", "Shop 1"), List.of(),
                        false, PL1ExportConst.EXPORT_PRICES_SORT_BY_COMPETITORS,
                        "integration/api/exportPrices-full-bybid-shop.csv",
                        "integration/api/exportPrices-full-bybid-shop.xlsx"},
                {OrderByPerExportPriceEnum.price, List.of("Shop 2", "Shop 1"), List.of(),
                        false, PL1ExportConst.EXPORT_PRICES_SORT_BY_COMPETITORS,
                        "integration/api/exportPrices-full-byprice-shop.csv",
                        "integration/api/exportPrices-full-byprice-shop.xlsx"},
                {OrderByPerExportPriceEnum.bid, List.of("Магазин номер 2", "Магазин номер 1"), List.of(2, 1),
                        false, PL1ExportConst.EXPORT_PRICES_SORT_BY_COMPETITORS,
                        "integration/api/exportPrices-full-bybid-shop2.csv",
                        "integration/api/exportPrices-full-bybid-shop2.xlsx"},
        };
    }

    static Object[][] exportPrices() {
        return new Object[][]{
                {false, false, "integration/api/exportPrices-default.csv",
                        "integration/api/exportPrices-default.xlsx"},
                {true, false,
                        "integration/api/exportPrices-withDeliveryCost.csv",
                        "integration/api/exportPrices-withDeliveryCost.xlsx"},
                {false, true,
                        "integration/api/exportPrices-withAvailability.csv",
                        "integration/api/exportPrices-withAvailability.xlsx"},
                {true, true,
                        "integration/api/exportPrices-full.csv",
                        "integration/api/exportPrices-full.xlsx"}
        };
    }


    static Object[][] analyticsPerOffer() {
        return new Object[][]{
                {DateGroupTypeEnum.days, true, false,
                        "integration/api/analyticsPerOffer-group-days.csv",
                        "integration/api/analyticsPerOffer-group-days.xlsx"},
                {DateGroupTypeEnum.months, true, false,
                        "integration/api/analyticsPerOffer-group-months.csv",
                        "integration/api/analyticsPerOffer-group-months.xlsx"},
                {DateGroupTypeEnum.weeks, true, false,
                        "integration/api/analyticsPerOffer-group-weeks.csv",
                        "integration/api/analyticsPerOffer-group-weeks.xlsx"},
                {DateGroupTypeEnum.days, false, false,
                        "integration/api/analyticsPerOffer-days.csv",
                        "integration/api/analyticsPerOffer-days.xlsx"},
                {null, false, false,
                        "integration/api/analyticsPerOffer.csv",
                        "integration/api/analyticsPerOffer.xlsx"},
                {null, false, true,
                        "integration/api/analyticsPerOffer-shop.csv",
                        "integration/api/analyticsPerOffer-shop.xlsx"},
                {DateGroupTypeEnum.days, false, true,
                        "integration/api/analyticsPerOffer-shop-date.csv",
                        "integration/api/analyticsPerOffer-shop-date.xlsx"},
                {DateGroupTypeEnum.days, true, true,
                        "integration/api/analyticsPerOffer-shop-group-days.csv",
                        "integration/api/analyticsPerOffer-shop-group-days.xlsx"}
        };
    }

    static Object[][] analyticsPerCategory() {
        return new Object[][]{
                {DateGroupTypeEnum.days, true,
                        "integration/api/analyticsPerCategory-group-days.csv",
                        "integration/api/analyticsPerCategory-group-days.xlsx"},
                {DateGroupTypeEnum.months, true,
                        "integration/api/analyticsPerCategory-group-months.csv",
                        "integration/api/analyticsPerCategory-group-months.xlsx"},
                {DateGroupTypeEnum.weeks, true,
                        "integration/api/analyticsPerCategory-group-weeks.csv",
                        "integration/api/analyticsPerCategory-group-weeks.xlsx"},
                {DateGroupTypeEnum.days, false,
                        "integration/api/analyticsPerCategory-days.csv",
                        "integration/api/analyticsPerCategory-days.xlsx"},
                {null, false,
                        "integration/api/analyticsPerCategory.csv",
                        "integration/api/analyticsPerCategory.xlsx"}
        };
    }

    static Object[][] analyticsPerFeed() {
        return new Object[][]{
                {DateGroupTypeEnum.days, true,
                        "integration/api/analyticsPerFeed-group-days.csv",
                        "integration/api/analyticsPerFeed-group-days.xlsx"},
                {DateGroupTypeEnum.months, true,
                        "integration/api/analyticsPerFeed-group-months.csv",
                        "integration/api/analyticsPerFeed-group-months.xlsx"},
                {DateGroupTypeEnum.weeks, true,
                        "integration/api/analyticsPerFeed-group-weeks.csv",
                        "integration/api/analyticsPerFeed-group-weeks.xlsx"},
                {DateGroupTypeEnum.days, false,
                        "integration/api/analyticsPerFeed-days.csv",
                        "integration/api/analyticsPerFeed-days.xlsx"},
                {null, false,
                        "integration/api/analyticsPerFeed.csv",
                        "integration/api/analyticsPerFeed.xlsx"}
        };
    }

    static Object[][] scheduleAnalyticsPerOffer() {
        var charset = CsvParameters.DEFAULT.getEncoding();
        return new Object[][]{
                {null, charset, true,
                        "integration/api/analyticsPerOffer-group-days-total.csv",
                        "integration/api/analyticsPerOffer-group-days-total.xlsx"},
                {"charset=UTF-8", StandardCharsets.UTF_8, true,
                        "integration/api/analyticsPerOffer-group-days-total.csv",
                        "integration/api/analyticsPerOffer-group-days-total.xlsx"},
                {"charset=UTF-8;delimiter=\",\";quote=\"'\"", StandardCharsets.UTF_8, true,
                        "integration/api/analyticsPerOffer-group-different-format-totals.csv",
                        "integration/api/analyticsPerOffer-group-different-format-totals.xlsx"},
                {null, charset, false,
                        "integration/api/analyticsPerOffer-days-total.csv",
                        "integration/api/analyticsPerOffer-days-total.xlsx"}
        };
    }

    static Object[][] scheduleExportPrices() {
        var charset = CsvParameters.DEFAULT.getEncoding();
        return new Object[][]{
                {null, charset,
                        "integration/api/exportPrices-full.csv",
                        "integration/api/exportPrices-full.xlsx"},
                {"charset=UTF-8", StandardCharsets.UTF_8,
                        "integration/api/exportPrices-full.csv",
                        "integration/api/exportPrices-full.xlsx"},
                {"charset=UTF-8;delimiter=\",\";quote=\"'\"", StandardCharsets.UTF_8,
                        "integration/api/exportPrices-full-different-format.csv",
                        "integration/api/exportPrices-full-different-format.xlsx"}
        };
    }

    static Object[][] exportPricesEstimate() {
        var emptyFilter = filter(1);
        var filterWithCategory = filter(1, f -> f.setCategories_by_id(Set.of(382L)));

        return new Object[][]{
                {"empty", emptyFilter, 0, true},
                {"empty", emptyFilter, 1, true},
                {"empty", emptyFilter, 9, true},
                {"empty", emptyFilter, 10, false},
                {"empty", emptyFilter, 999999, false},
                {"by category", filterWithCategory, 0, true},
                {"by category", filterWithCategory, 1, true},
                {"by category", filterWithCategory, 7, true},
                {"by category", filterWithCategory, 8, false},
                {"by category", filterWithCategory, 999, false}
        };
    }

    static Object[][] analyticsPerMonth() {
        return new Object[][]{
                {false, "integration/api/analyticsPerMonth.csv",
                        "integration/api/analyticsPerMonth.xlsx"},
                {true,
                        "integration/api/analyticsPerMonth-byshop.csv",
                        "integration/api/analyticsPerMonth-byshop.xlsx"},
        };
    }

    static Object[][] recommendationsPositions() {
        var p1 = new MaxRecommendationsResponse()
                .cardBidMax(30.1).cardBidAvg(25.1).searchBidMax(10.1).searchBidAvg(10.1);

        var p10 = new MaxRecommendationsResponse()
                .cardBidMax(31.0).cardBidAvg(26.0).searchBidMax(11.0).searchBidAvg(11.0);

        var p11 = new MaxRecommendationsResponse()
                .cardBidMax(31.0).cardBidAvg(26.0).searchBidMax(11.1).searchBidAvg(11.1);

        var p12 = new MaxRecommendationsResponse()
                .cardBidMax(31.0).cardBidAvg(26.0).searchBidMax(0.).searchBidAvg(0.);

        var pAny = new MaxRecommendationsResponse()
                .cardBidMax(0.).cardBidAvg(0.).searchBidMax(0.).searchBidAvg(0.);

        return new Object[][]{
                {1, p1}, {2, pAny}, {3, pAny}, {4, pAny}, {5, pAny}, {6, pAny}, {7, pAny}, {8, pAny}, {9, pAny},
                {10, p10}, {11, p11}, {12, p12}
        };
    }
}
