#include <market/dynamic_pricing/deprecated/autostrategy/optimizer/margin_v1.h>
#include <market/dynamic_pricing/deprecated/autostrategy/price_randomizer/price_randomizer.h>

#include <library/cpp/logger/global/global.h>
#include <library/cpp/json/json_value.h>
#include <library/cpp/testing/unittest/gtest.h>

#include <util/generic/maybe.h>

using namespace NMarket::NDynamicPricing;
using namespace NMarket::NDynamicPricing::NOptimizer;
using namespace NMarket::NDynamicPricing::NPriceRandomizer;

namespace {
    const TMargin TARGET_MARGIN_THRESHOLD = 1e-4;
    constexpr int STEP = 10;

    void AddSkuLinearDemand(
        TDemandForecast& demandForecast,
        TPrice startPrice, TPrice endPrice, TPrice stepPrice,
        TDemand startDemand, TDemand endDemand
    ) {
        const TDemand stepDemand = stepPrice * (endDemand - startDemand) / (endPrice - startPrice);
        TPrice price = startPrice;
        TDemand demand = startDemand;
        for (; price <= endPrice; price += stepPrice, demand += stepDemand) {
            demandForecast.emplace(price, demand);
        }
    }

    void AddSku(
        TGroupSkuData& data,
        TMarketSkuId skuId,
        TPrice buyPrice,
        TStock stock,
        TDemand startDemand,
        TDemand endDemand,
        TMaybe<TPrice> lowPrice = Nothing(),
        TMaybe<TPrice> highPrice = Nothing(),
        EAbcStatus abcStatus = EAbcStatus::Unknown
    ) {
        TMarketSkuData msku;
        TShopSkuData ssku;
        ssku.MarketSkuId = skuId;
        ssku.ShopSkuId = ToString(skuId);
        ssku.BuyPrice = buyPrice;
        ssku.PurchasePrice = buyPrice;
        ssku.CurrentPrice = buyPrice;
        ssku.Stock = stock;
        ssku.AbcStatus = abcStatus;
        if (lowPrice) {
            ssku.LowPrice = *lowPrice;
        }
        if (highPrice) {
            ssku.HighPrice = *highPrice;
        }
        msku.ShopsData.push_back(ssku);
        msku.TotalStock = stock;
        const TPrice minPrice = static_cast<int>(buyPrice * 0.7) / STEP * STEP;
        AddSkuLinearDemand(
            msku.DemandForecast,
            minPrice, buyPrice * 1.5, STEP,
            startDemand, endDemand);
        data.emplace(skuId, msku);
    }

    const NJson::TJsonValue CreateUsePromoConfig(TMargin targetMargin, TMargin targetMarginThreshold, bool useSkuWithPromo) {
        auto config = NJson::TJsonValue();
        config.InsertValue("target_margin", targetMargin);
        config.InsertValue("target_margin_threshold", targetMarginThreshold);
        config.InsertValue("use_sku_with_promo", useSkuWithPromo);
        return config;
    }

    const auto nullPriceRandomizer = TPriceRandomizer();
    const auto metricsCalculator = NOptimizer::TMetricsCalculator();
}

TEST(TestPricingMarginV1, TestLinear_StockUnlimited)
{
    InitGlobalLog2Null();

    TGroupSkuData data;
    AddSku(
        data,
        1, 1000, 1000,
        10, 1);
    AddSku(
        data,
        2, 1100, 1000,
        8, 2);
    AddSku(
        data,
        3, 1500, 1000,
        6, 3);
    const TMargin targetMargin = 0.01;
    TMarginCalculatorV1 calculator(data, nullPriceRandomizer, metricsCalculator, targetMargin, TARGET_MARGIN_THRESHOLD);
    const TOutputResult result = calculator.Calculate();
    // Was brute-forced.
    const THashMap<TShopSkuId, TPrice> expected {
        {"1", 880},
        {"2", 1060},
        {"3", 1860}
    };

    for (const auto&[id, price] : result.Prices) {
        Y_UNUSED(id);
        EXPECT_NEAR(price.NewPrice, expected.Value(price.SkuData.ShopSkuId, -100), STEP);
    }
}

TEST(TestPricingMarginV1, TestLinear_MaxDailySalesUnlimited_HighTurnoverSku)
{
    InitGlobalLog2Null();

    TGroupSkuData data;
    AddSku(
        data,
        1, 1000, 1000,
        10, 1);
    AddSku(
        data,
        2, 1100, 1000,
        8, 2);
    AddSku(
        data,
        3, 1500, 1000,
        6, 3);

    data[1].ShopsData[0].MaxDailySales = 1000;
    data[2].ShopsData[0].MaxDailySales = 1000;
    data[3].ShopsData[0].MaxDailySales = 1000;
    data[1].ShopsData[0].AbcStatus = EAbcStatus::A;
    data[2].ShopsData[0].AbcStatus = EAbcStatus::B;
    data[3].ShopsData[0].AbcStatus = EAbcStatus::TopSeller;

    const TMargin targetMargin = 0.01;
    TMarginCalculatorV1 calculator(data, nullPriceRandomizer, metricsCalculator, targetMargin, TARGET_MARGIN_THRESHOLD);
    const TOutputResult result = calculator.Calculate();
    // Was brute-forced.
    const THashMap<TShopSkuId, TPrice> expected {
        {"1", 880},
        {"2", 1060},
        {"3", 1860}
    };

    for (const auto&[skuId, price] : result.Prices) {
        EXPECT_NEAR(price.NewPrice, expected.Value(price.SkuData.ShopSkuId, -100), STEP);
    }
}

TEST(TestPricingMarginV1, TestLinear_StockLimited)
{
    InitGlobalLog2Null();

    TGroupSkuData data;
    AddSku(
        data,
        1, 1000, 5*3,
        10, 1);
    AddSku(
        data,
        2, 1100, 3*3,
        8, 2);
    AddSku(
        data,
        3, 1500, 1*3,
        6, 3);
    const TMargin targetMargin = 0.01;
    TMarginCalculatorV1 calculator(data, nullPriceRandomizer, metricsCalculator, targetMargin, TARGET_MARGIN_THRESHOLD);
    const TOutputResult result = calculator.Calculate();
    // Was brute-forced.
    const THashMap<TShopSkuId, TPrice> expected {
        {"1", 820},
        {"2", 1010},
        {"3", 2250}
    };
    for (const auto&[skuId, price] : result.Prices) {
        EXPECT_NEAR(price.NewPrice, expected.Value(price.SkuData.ShopSkuId, -100), STEP);
    }
}

TEST(TestPricingMarginV1, TestLinear_MaxDailySalesLimited_LowTurnoverSku)
{
    InitGlobalLog2Null();

    TGroupSkuData data;
    AddSku(
        data,
        1, 1000, 1000,
        10, 1);
    AddSku(
        data,
        2, 1100, 1000,
        8, 2);
    AddSku(
        data,
        3, 1500, 1000,
        6, 3);

    data[1].ShopsData[0].MaxDailySales = 0;
    data[2].ShopsData[0].MaxDailySales = 0;
    data[3].ShopsData[0].MaxDailySales = 0;
    data[1].ShopsData[0].AbcStatus = EAbcStatus::C;
    data[2].ShopsData[0].AbcStatus = EAbcStatus::DeadStock;
    data[3].ShopsData[0].AbcStatus = EAbcStatus::SlowMover;

    const TMargin targetMargin = 0.01;
    TMarginCalculatorV1 calculator(data, nullPriceRandomizer, metricsCalculator, targetMargin, TARGET_MARGIN_THRESHOLD);
    const TOutputResult result = calculator.Calculate();
    // Was brute-forced.
    const THashMap<TShopSkuId, TPrice> expected {
        {"1", 880},
        {"2", 1060},
        {"3", 1860}
    };

    for (const auto&[skuId, price] : result.Prices) {
        EXPECT_NEAR(price.NewPrice, expected.Value(price.SkuData.ShopSkuId, -100), STEP);
    }
}

TEST(TestPricingMarginV1, TestLinear_MaxDailySalesLimited_HighTurnover)
{
    InitGlobalLog2Null();

    TGroupSkuData data;
    AddSku(
        data,
        1, 1000, 1000,
        10, 1);
    AddSku(
        data,
        2, 1100, 1000,
        8, 2);
    AddSku(
        data,
        3, 1500, 1000,
        6, 3);

    data[1].ShopsData[0].MaxDailySales = 5;
    data[2].ShopsData[0].MaxDailySales = 3;
    data[3].ShopsData[0].MaxDailySales = 1;
    data[1].ShopsData[0].AbcStatus = EAbcStatus::A;
    data[2].ShopsData[0].AbcStatus = EAbcStatus::B;
    data[3].ShopsData[0].AbcStatus = EAbcStatus::TopSeller;

    const TMargin targetMargin = 0.01;
    TMarginCalculatorV1 calculator(data, nullPriceRandomizer, metricsCalculator, targetMargin, TARGET_MARGIN_THRESHOLD);
    const TOutputResult result = calculator.Calculate();
    // Was brute-forced.
    const THashMap<TShopSkuId, TPrice> expected {
        {"1", 880},
        {"2", 1070},
        {"3", 1850}
    };
    for (const auto&[skuId, price] : result.Prices) {
        EXPECT_NEAR(price.NewPrice, expected.Value(price.SkuData.ShopSkuId, -100), STEP);
    }
}

TEST(TestPricingMarginV1, TestLinear_StockLimitedHard)
{
    InitGlobalLog2Null();

    TGroupSkuData data;
    AddSku(
        data,
        1, 1000, 0,
        10, 1);
    AddSku(
        data,
        2, 1100, 0,
        8, 2);
    AddSku(
        data,
        3, 1500, 1000,
        6, 3);
    const TMargin targetMargin = 0.01;
    TMarginCalculatorV1 calculator(data, nullPriceRandomizer, metricsCalculator, targetMargin, TARGET_MARGIN_THRESHOLD);
    const TOutputResult result = calculator.Calculate();
    // Was brute-forced.
    const THashMap<TShopSkuId, TPrice> expected {
        {"1", 1000},
        {"2", 1100},
        {"3", 1720}
    };
    for (const auto&[skuId, price] : result.Prices) {
        EXPECT_NEAR(price.NewPrice, expected.Value(price.SkuData.ShopSkuId, -100), STEP);
    }
}

TEST(TestPricingMarginV1, Test_StrongLimits)
{
    InitGlobalLog2Null();

    TGroupSkuData data;
    AddSku(
        data,
        1, 1000, 1000,
        10, 1,
        10000, 10000);
    AddSku(
        data,
        2, 1000, 1000,
        10, 1,
        999, 1001);
    const TMargin targetMargin = 0.01;
    TMarginCalculatorV1 calculator(data, nullPriceRandomizer, metricsCalculator, targetMargin, TARGET_MARGIN_THRESHOLD);
    const TOutputResult result = calculator.Calculate();
    // Was brute-forced.
    const THashMap<TShopSkuId, TPrice> expected {
        {"1", 10000},
        {"2", 1000}
    };
    for (const auto&[skuId, price] : result.Prices) {
        EXPECT_NEAR(price.NewPrice, expected.Value(price.SkuData.ShopSkuId, -100), 0.1);
    }
}

TEST(TestPricingMarginV1, Test_NoDemand)
{
    InitGlobalLog2Null();

    TGroupSkuData data;
    {
        TMarketSkuData msku;
        TShopSkuData ssku;
        ssku.BuyPrice = 1000;
        ssku.PurchasePrice = 1000;
        ssku.CurrentPrice = 1500;
        ssku.Stock = 1000;
        ssku.ShopSkuId = "1";
        ssku.MarketSkuId = 1;
        msku.ShopsData.push_back(ssku);
        data.emplace(1, msku);
    }
    const TMargin targetMargin = 0.01;
    TMarginCalculatorV1 calculator(data, nullPriceRandomizer, metricsCalculator, targetMargin, TARGET_MARGIN_THRESHOLD);
    const TOutputResult result = calculator.Calculate();
    // Was brute-forced.
    const THashMap<TShopSkuId, TPrice> expected {
        {"1", 1010.1}
    };
    for (const auto&[skuId, price] : result.Prices) {
        EXPECT_NEAR(price.NewPrice, expected.Value(price.SkuData.ShopSkuId, -100), 0.1);
    }
}

TEST(TestPricingMarginV1, TestLinear_BlueMin3pPrice_FightForPrice)
{
    InitGlobalLog2Null();

    TGroupSkuData data;
    AddSku(
        data,
        1, 1000, 1000,
        10, 1);
    data[1].ShopsData[0].BlueMin3pPrice = 1200;
    AddSku(
        data,
        2, 1000, 1000,
        10, 1);
    const TMargin targetMargin = 0.2;
    TMarginCalculatorV1 calculator(data, nullPriceRandomizer, metricsCalculator, targetMargin, TARGET_MARGIN_THRESHOLD);
    const TOutputResult result = calculator.Calculate();
    // Was brute-forced.
    const THashMap<TShopSkuId, TPrice> expected {
        {"1", 1190},
        {"2", 1350},
    };
    for (const auto&[skuId, price] : result.Prices) {
        EXPECT_NEAR(price.NewPrice, expected.Value(price.SkuData.ShopSkuId, -100), STEP);
    }
}

TEST(TestPricingMarginV1, TestLinear_BlueMin3pPrice_DoNotFightForPrice)
{
    InitGlobalLog2Null();

    TGroupSkuData data;
    AddSku(
        data,
        1, 1000, 1000,
        10, 1);
    data[1].ShopsData[0].BlueMin3pPrice = 800;
    AddSku(
        data,
        2, 1000, 1000,
        10, 1);
    const TMargin targetMargin = 0.2;
    TMarginCalculatorV1 calculator(data, nullPriceRandomizer, metricsCalculator, targetMargin, TARGET_MARGIN_THRESHOLD);
    const TOutputResult result = calculator.Calculate();
    // Was brute-forced.
    const THashMap<TShopSkuId, TPrice> expected {
        {"1", 810}, // demand == 0
        {"2", 1250}, // guarantee target margin
    };
    for (const auto&[skuId, price] : result.Prices) {
        EXPECT_NEAR(price.NewPrice, expected.Value(price.SkuData.ShopSkuId, -100), STEP);
    }
}

TEST(TestPricingMarginV1, TestLinear_MaxAvailPrice_HideByWhiteMedian)
{
    InitGlobalLog2Null();

    TGroupSkuData data;
    AddSku(
        data,
        1, 1000, 1000,
        10, 1);
    data[1].ShopsData[0].MaxAvailPrice = 800;
    AddSku(
        data,
        2, 1000, 1000,
        10, 1);
    const TMargin targetMargin = 0.2;
    TMarginCalculatorV1 calculator(data, nullPriceRandomizer, metricsCalculator, targetMargin, TARGET_MARGIN_THRESHOLD);
    const TOutputResult result = calculator.Calculate();
    // Was brute-forced.
    const THashMap<TShopSkuId, TPrice> expected {
        {"1", 810}, // demand == 0
        {"2", 1250}, // guarantee target margin
    };
    for (const auto&[skuId, price] : result.Prices) {
        EXPECT_NEAR(price.NewPrice, expected.Value(price.SkuData.ShopSkuId, -100), STEP);
    }
}

TEST(TestPricingMarginV1, TestLinear_MaxAvailPrice_DontHideByWhiteMedian)
{
    InitGlobalLog2Null();

    TGroupSkuData data;
    AddSku(
        data,
        1, 1000, 1000,
        10, 1);
    data[1].ShopsData[0].MaxAvailPrice = 1200;
    AddSku(
        data,
        2, 1000, 1000,
        10, 1);
    const TMargin targetMargin = 0.2;
    TMarginCalculatorV1 calculator(data, nullPriceRandomizer, metricsCalculator, targetMargin, TARGET_MARGIN_THRESHOLD);
    const TOutputResult result = calculator.Calculate();
    // Was brute-forced.
    const THashMap<TShopSkuId, TPrice> expected {
        {"1", 1190},
        {"2", 1320},
    };
    for (const auto&[skuId, price] : result.Prices) {
        EXPECT_NEAR(price.NewPrice, expected.Value(price.SkuData.ShopSkuId, -100), STEP);
    }
}

TEST(TestPricingMarginV1, Test_UsePromo_Disabled)
{
    InitGlobalLog2Null();

    const bool useSkuWithPromo = false;
    TGroupSkuData data;
    AddSku(
        data,
        1, 1000, 1000,
        10, 1);
    AddSku(
        data,
        2, 1000, 1000,
        10, 1);
    AddSku(
        data,
        3, 1000, 1000,
        10, 1);
    data[1].ShopsData[0].IsPromo = true;
    data[2].ShopsData[0].IsPromo = true;
    data[3].ShopsData[0].IsPromo = false;

    const TMargin targetMargin = 0.2;
    const auto& config = CreateUsePromoConfig(targetMargin, TARGET_MARGIN_THRESHOLD, useSkuWithPromo);
    TMarginCalculatorV1 calculator(data, nullPriceRandomizer, metricsCalculator, TPricingConfig(config));

    const TOutputResult result = calculator.Calculate();

    EXPECT_NEAR(result.OptimalMetric.Profit, 953, STEP);
    EXPECT_NEAR(result.OptimalMetric.Gmv, 4765, STEP);
    EXPECT_NEAR(result.OptimalMetric.Margin, targetMargin, TARGET_MARGIN_THRESHOLD);

    // Was brute-forced.
    const THashMap<TShopSkuId, TPrice> expected {
        {"1", 1250},
        {"2", 1250},
        {"3", 1250}
    };

    for (const auto&[skuId, price] : result.Prices) {
        EXPECT_NEAR(price.NewPrice, expected.Value(price.SkuData.ShopSkuId, -100), STEP);
    }
}

TEST(TestPricingMarginV1, Test_UsePromo_Enabled)
{
    InitGlobalLog2Null();

    const bool useSkuWithPromo = true;
    TGroupSkuData data;
    AddSku(
        data,
        1, 1000, 1000,
        10, 1);
    AddSku(
        data,
        2, 1000, 1000,
        10, 1);
    AddSku(
        data,
        3, 1000, 1000,
        10, 1);
    data[1].ShopsData[0].IsPromo = true;
    data[2].ShopsData[0].IsPromo = true;
    data[3].ShopsData[0].IsPromo = false;

    const TMargin targetMargin = 0.2;
    const auto& config = CreateUsePromoConfig(targetMargin, TARGET_MARGIN_THRESHOLD, useSkuWithPromo);
    TMarginCalculatorV1 calculator(data, nullPriceRandomizer, metricsCalculator, TPricingConfig(config));
    const TOutputResult result = calculator.Calculate();

    EXPECT_NEAR(result.OptimalMetric.Profit, 953*3, STEP);
    EXPECT_NEAR(result.OptimalMetric.Gmv, 4765*3, STEP);
    EXPECT_NEAR(result.OptimalMetric.Margin, targetMargin, TARGET_MARGIN_THRESHOLD);

    // Was brute-forced.
    const THashMap<TShopSkuId, TPrice> expected {
        {"1", 1250},
        {"2", 1250},
        {"3", 1250}
    };

    for (const auto&[skuId, price] : result.Prices) {
        EXPECT_NEAR(price.NewPrice, expected.Value(price.SkuData.ShopSkuId, -100), STEP);
    }
}

TEST(TestPricingMarginV1, Test_ConfigExceptions)
{
    InitGlobalLog2Null();
    TGroupSkuData data;

    ForEach(std::make_tuple("target_margin", "target_margin_threshold"), [&](const auto& removedConfig) {
        NJson::TJsonValue config = CreateUsePromoConfig(0.2, TARGET_MARGIN_THRESHOLD, true);
        config.EraseValue(removedConfig);
        TPricingConfig pricingConfig(config);
        EXPECT_THROW(TMarginCalculatorV1(data, nullPriceRandomizer, metricsCalculator, pricingConfig), NPrivateException::yexception);
    });
}
