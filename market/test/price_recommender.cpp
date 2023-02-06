#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/env.h>
#include <market/report/src/place/price_recommender/by_demand_forecaster.h>

namespace {

using namespace NMarket;
using namespace NMarket::NReport;
using namespace NMarketReport::NPriceRecommender;


TEST(PriceRecommender, PredictionToRecommendationThresholds_MinAndCurrent) {
    const TDemandPredictionData data = {
        { "2018-01-01", {
            { "currentPrice", TDemandPredictionPriceData{
                .price = 100,
                .sources = { {0, 0.5}, {1, 0.5} }
            } },
            { "minimumPrice", TDemandPredictionPriceData{
                .price = 10,
                .sources = { {2, 1.0}, {3, 1.0} }
            } },
            { "minRefPrice", TDemandPredictionPriceData{
                .price = 10,
                .sources = { {2, 1.0}, {3, 1.0} }
            } },
        } },
        { "2018-01-02", {
            { "currentPrice", TDemandPredictionPriceData{
                .price = 100,
                .sources = { {0, 0.5}, {1, 0.5} }
            } },
            { "minimumPrice", TDemandPredictionPriceData{
                .price = 10,
                .sources = { {2, 1.0}, {3, 1.0} }
            } },
            { "minRefPrice", TDemandPredictionPriceData{
                .price = 10,
                .sources = { {2, 1.0}, {3, 1.0} }
            } },
        } },
        { "2018-01-03", {
            { "currentPrice", TDemandPredictionPriceData{
                .price = 100,
                .sources = { {0, 0.5}, {1, 0.5} }
            } },
            { "minimumPrice", TDemandPredictionPriceData{
                .price = 10,
                .sources = { {2, 1.0}, {3, 1.0} }
            } },
            { "minRefPrice", TDemandPredictionPriceData{
                .price = 10,
                .sources = { {2, 1.0}, {3, 1.0} }
            } },
        } }
    };

    const TMap<EThresholdType, TThreshold> thresholds = GetRecommendationThresholds(data);

    EXPECT_EQ(3, thresholds.size());

    EXPECT_EQ(100, thresholds.at(TT_BY_SUPPLIER).Price.GetIntegerPart());
    EXPECT_EQ(3, thresholds.at(TT_BY_SUPPLIER).Sales);

    EXPECT_EQ(10, thresholds.at(TT_MIN_BLUE).Price.GetIntegerPart());
    EXPECT_EQ(6, thresholds.at(TT_MIN_BLUE).Sales);

    EXPECT_EQ(10, thresholds.at(TT_MIN_REFERENCE).Price.GetIntegerPart());
    EXPECT_EQ(6, thresholds.at(TT_MIN_REFERENCE).Sales);
}


TEST(PriceRecommender, PredictionToRecommendationThresholds_MinRefAndDefaultOffer) {
    const TDemandPredictionData data = {
        { "2018-02-01", {
            { "minRefPrice", TDemandPredictionPriceData{
                .price = 10,
                .sources = { {0, 0.5}, {1, 0.5} }
            } },
            { "defaultOfferPrice", TDemandPredictionPriceData{
                .price = 20,
                .sources = { {2, 1.0}, {3, 1.0} }
            } },
        } },
        { "2018-02-02", {
            { "minRefPrice", TDemandPredictionPriceData{
                .price = 10,
                .sources = { {0, 0.5}, {1, 0.5} }
            } },
            { "defaultOfferPrice", TDemandPredictionPriceData{
                .price = 20,
                .sources = { {2, 1.0}, {3, 1.0} }
            } },
        } },
        { "2018-02-03", {
            { "minRefPrice", TDemandPredictionPriceData{
                .price = 10,
                .sources = { {0, 0.5}, {1, 0.5} }
            } },
            { "defaultOfferPrice", TDemandPredictionPriceData{
                .price = 20,
                .sources = { {2, 1.0}, {3, 1.0} }
            } },
        } }
    };

    const TMap<EThresholdType, TThreshold> thresholds = GetRecommendationThresholds(data);

    EXPECT_EQ(1, thresholds.size()); // "defaultOfferPrice" ignored

    EXPECT_EQ(10, thresholds.at(TT_MIN_REFERENCE).Price.GetIntegerPart());
    EXPECT_EQ(3, thresholds.at(TT_MIN_REFERENCE).Sales);
}


// promos with a gap and different prices
TEST(PriceRecommender, PredictionToRecommendationThresholds_Promo1) {
    const TDemandPredictionData data = {
        { "2018-03-01", {
            { "promoPrice", TDemandPredictionPriceData{
                .price = 10,
                .sources = { {0, 0.5}, {1, 0.5} }
            } }
        } },
        { "2018-03-02", {
            { "promoPrice", TDemandPredictionPriceData{
                .price = 10,
                .sources = { {0, 1.0}, {1, 1.0} }
            } }
        } },
        { "2018-03-03", {
            { "promoPrice", TDemandPredictionPriceData{
                .price = 10,
                .sources = { {0, 2.0}, {1, 2.0} }
            } }
        } }, // end of first three-days promo, sales = 7.0

        { "2018-03-07", {
            { "promoPrice", TDemandPredictionPriceData{
                .price = 9,
                .sources = { {0, 1.0}, {1, 1.0} }
            } }
        } },
        { "2018-03-08", {
            { "promoPrice", TDemandPredictionPriceData{
                .price = 9,
                .sources = { {0, 2.0}, {1, 2.0} }
            } }
        } } // end of second promo (must be ignored)
    };

    const TMap<EThresholdType, TThreshold> thresholds = GetRecommendationThresholds(data);

    EXPECT_EQ(1, thresholds.size());

    EXPECT_EQ(10, thresholds.at(TT_PROMO).Price.GetIntegerPart());
    EXPECT_EQ(7.0, thresholds.at(TT_PROMO).Sales);
    EXPECT_EQ(1519862400, thresholds.at(TT_PROMO).StartDate); // 2018-03-01 00:00 GMT
    EXPECT_EQ(1520121600, thresholds.at(TT_PROMO).EndDate); // 2018-03-04 00:00 GMT
}


// promos with a gap
TEST(PriceRecommender, PredictionToRecommendationThresholds_Promo2) {

    const TDemandPredictionData data = {
        { "2018-03-01", {
            { "promoPrice", TDemandPredictionPriceData{
                .price = 10,
                .sources = { {0, 0.5}, {1, 0.5} }
            } }
        } },
        { "2018-03-02", {
            { "promoPrice", TDemandPredictionPriceData{
                .price = 10,
                .sources = { {0, 0.5}, {1, 0.5} }
            } }
        } }, // end of first two-days promo, sales = 2.0

        { "2018-03-04", {
            { "promoPrice", TDemandPredictionPriceData{
                .price = 10,
                .sources = { {0, 0.5}, {1, 0.5} }
            } }
        } },
        { "2018-03-05", {
            { "promoPrice", TDemandPredictionPriceData{
                .price = 10,
                .sources = { {0, 0.5}, {1, 0.5} }
            } }
        } }, // end of second promo
    };

    const TMap<EThresholdType, TThreshold> thresholds = GetRecommendationThresholds(data);

    EXPECT_EQ(1, thresholds.size());

    EXPECT_EQ(10, thresholds.at(TT_PROMO).Price.GetIntegerPart());
    EXPECT_EQ(2.0, thresholds.at(TT_PROMO).Sales);
    EXPECT_EQ(1519862400, thresholds.at(TT_PROMO).StartDate); // 2018-03-01 00:00 GMT
    EXPECT_EQ(1520035200, thresholds.at(TT_PROMO).EndDate); // 2018-03-03 00:00 GMT
}


// promos with different prices
TEST(PriceRecommender, PredictionToRecommendationThresholds_Promo3) {
    const TDemandPredictionData data = {
        { "2018-03-01", {
            { "promoPrice", TDemandPredictionPriceData{
                .price = 10,
                .sources = { {0, 0.5}, {1, 0.5} }
            } }
        } }, // end of first one-day promo, sales = 1.0
        { "2018-03-02", {
            { "promoPrice", TDemandPredictionPriceData{
                .price = 5,
                .sources = { {0, 1.0}, {1, 1.0} }
            } }
        } } // end of second promo
    };

    const TMap<EThresholdType, TThreshold> thresholds = GetRecommendationThresholds(data);

    EXPECT_EQ(1, thresholds.size());

    EXPECT_EQ(10, thresholds.at(TT_PROMO).Price.GetIntegerPart());
    EXPECT_EQ(1.0, thresholds.at(TT_PROMO).Sales);
    EXPECT_EQ(1519862400, thresholds.at(TT_PROMO).StartDate); // 2018-03-01 00:00 GMT
    EXPECT_EQ(1519948800, thresholds.at(TT_PROMO).EndDate); // 2018-03-02 00:00 GMT
}


// supplier threshold must be used as min. Blue and min. reference ones
TEST(PriceRecommender, PredictionToRecommendationThresholds_OnlySupplier) {
    const TDemandPredictionData data = {
        { "2018-01-01", {
            { "currentPrice", TDemandPredictionPriceData{
                .price = 100,
                .sources = { {0, 1.0} }
            } }
        } }
    };

    const TMap<EThresholdType, TThreshold> thresholds = GetRecommendationThresholds(data);

    EXPECT_EQ(2, thresholds.size());

    EXPECT_EQ(100, thresholds.at(TT_BY_SUPPLIER).Price.GetIntegerPart());
    EXPECT_EQ(1, thresholds.at(TT_BY_SUPPLIER).Sales);

    EXPECT_EQ(100, thresholds.at(TT_MIN_BLUE).Price.GetIntegerPart());
    EXPECT_EQ(1, thresholds.at(TT_MIN_BLUE).Sales);
}


// supplier threshold must replace min. Blue
TEST(PriceRecommender, PredictionToRecommendationThresholds_SupplierIsTheCheapest) {
    const TDemandPredictionData data = {
        { "2018-01-01", {
            { "currentPrice", TDemandPredictionPriceData{
                .price = 10,
                .sources = { {0, 3.0} }
            } },
            { "minimumPrice", TDemandPredictionPriceData{
                .price = 20,
                .sources = { {0, 2.0} }
            } },
            { "minRefPrice", TDemandPredictionPriceData{
                .price = 30,
                .sources = { {0, 1.0} }
            } }
        } }
    };

    const TMap<EThresholdType, TThreshold> thresholds = GetRecommendationThresholds(data);

    EXPECT_EQ(3, thresholds.size());

    EXPECT_EQ(10, thresholds.at(TT_BY_SUPPLIER).Price.GetIntegerPart());
    EXPECT_EQ(3, thresholds.at(TT_BY_SUPPLIER).Sales);

    EXPECT_EQ(10, thresholds.at(TT_MIN_BLUE).Price.GetIntegerPart());
    EXPECT_EQ(3, thresholds.at(TT_MIN_BLUE).Sales);

    EXPECT_EQ(30, thresholds.at(TT_MIN_REFERENCE).Price.GetIntegerPart());
    EXPECT_EQ(1, thresholds.at(TT_MIN_REFERENCE).Sales);
}


// TThreshold::Sales must stay undefined
TEST(PriceRecommender, PredictionToRecommendationThresholds_NoSales) {
    const TDemandPredictionData data = {
        { "2018-01-01", {
            { "minRefPrice", TDemandPredictionPriceData{
                .price = 10,
                .sources = { }
            } }
        } }
    };

    const TMap<EThresholdType, TThreshold> thresholds = GetRecommendationThresholds(data);

    EXPECT_EQ(1, thresholds.size());

    EXPECT_EQ(10, thresholds.at(TT_MIN_REFERENCE).Price.GetIntegerPart());
    EXPECT_EQ(Nothing(), thresholds.at(TT_MIN_REFERENCE).Sales);
}

} // anonymous namespace


