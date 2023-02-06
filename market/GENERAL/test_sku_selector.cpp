#include <market/dynamic_pricing/deprecated/autostrategy/sku_selector/sku_selector.h>

#include <library/cpp/logger/global/global.h>
#include <library/cpp/testing/unittest/gtest.h>

using namespace NMarket::NDynamicPricing;

namespace {

    TShopSkuData GetFixPriceSsku() {
        return {
            .ShopSkuId = "1",
            .LowPrice = 100.0,
            .HighPrice = 100.0,
            .PurchasePrice = 100.0,
            .BoundsRule = NSkuSelector::ConvertToBoundsRule("ФиксЦен"),
            .Stock = 100,
            .MaxDailySales = 50
        };
    }

     TShopSkuData GetFixPriceSsku2() {
        return {
            .ShopSkuId = "11",
            .LowPrice = 105.0,
            .HighPrice = 105.0,
            .PurchasePrice = 100.0,
            .BoundsRule = NSkuSelector::ConvertToBoundsRule("ФиксЦен"),
            .Stock = 10,
            .MaxDailySales = 5
        };
    }

    TShopSkuData GetMarketPriceSsku() {
        return {
            .ShopSkuId = "2",
            .LowPrice = 200.0,
            .HighPrice = 200.0,
            .PurchasePrice = 200.0,
            .BoundsRule = NSkuSelector::ConvertToBoundsRule("РРЦ"),
            .Stock = 200,
            .MaxDailySales = 100
        };
    }

    TShopSkuData GetOtherSsku() {
        return {
            .ShopSkuId = "3",
            .LowPrice = 300.0,
            .HighPrice = 300.0,
            .PurchasePrice = 300.0,
            .BoundsRule = NSkuSelector::ConvertToBoundsRule("Другое"),
            .Stock = 300,
            .MaxDailySales = 150
        };
    }

    TShopSkuData GetOtherSsku2() {
        return {
            .ShopSkuId = "4",
            .LowPrice = 400.0,
            .HighPrice = 400.0,
            .PurchasePrice = 400.0,
            .BoundsRule = NSkuSelector::ConvertToBoundsRule("Другое"),
            .Stock = 400,
            .MaxDailySales = 600
        };
    }
}

TEST(TestSskuSelector, Test_MarketPrice)
{
    InitGlobalLog2Null();
    TShopSkuData sskuFix = GetFixPriceSsku();
    TShopSkuData sskuMarket = GetMarketPriceSsku();
    TShopSkuData sskuOther = GetOtherSsku();
    TShopSkuData sskuOther2 = GetOtherSsku2();

    TMarketSkuData data;
    data.ShopsData = {sskuOther, sskuFix, sskuMarket, sskuOther2};
    NSkuSelector::AggregateMarketSkuData(data);

    EXPECT_EQ(data.ShopsData[0].LowPrice, sskuMarket.LowPrice);
    EXPECT_EQ(data.ShopsData[0].HighPrice, sskuMarket.HighPrice);
    EXPECT_EQ(data.ShopsData[0].PurchasePrice, sskuMarket.PurchasePrice);
    EXPECT_EQ(data.ShopsData[0].IsPromo, sskuMarket.IsPromo);
    EXPECT_EQ(data.ShopsData[0].IsDeadStock, sskuMarket.IsDeadStock);
}

TEST(TestSskuSelector, Test_FixPrice)
{
    InitGlobalLog2Null();
    TShopSkuData sskuFix = GetFixPriceSsku();
    TShopSkuData sskuOther = GetOtherSsku();

    TMarketSkuData data;
    data.ShopsData = {sskuOther, sskuFix};
    NSkuSelector::AggregateMarketSkuData(data);

    EXPECT_EQ(data.ShopsData[0].LowPrice, sskuFix.LowPrice);
    EXPECT_EQ(data.ShopsData[0].HighPrice, sskuFix.HighPrice);
    EXPECT_EQ(data.ShopsData[0].PurchasePrice, sskuFix.PurchasePrice);
    EXPECT_EQ(data.ShopsData[0].IsPromo, sskuFix.IsPromo);
    EXPECT_EQ(data.ShopsData[0].IsDeadStock, sskuFix.IsDeadStock);
}

TEST(TestSskuSelector, Test_TwoOther)
{
    InitGlobalLog2Null();
    TShopSkuData sskuOther = GetOtherSsku();
    TShopSkuData sskuOther2 = GetOtherSsku2();

    TMarketSkuData data;
    data.ShopsData = {sskuOther, sskuOther2};
    NSkuSelector::AggregateMarketSkuData(data);

    EXPECT_EQ(data.ShopsData[0].LowPrice, sskuOther.LowPrice);
    EXPECT_EQ(data.ShopsData[0].HighPrice, sskuOther.HighPrice);
    EXPECT_EQ(data.ShopsData[0].PurchasePrice, sskuOther.PurchasePrice);
    EXPECT_EQ(data.ShopsData[0].IsPromo, sskuOther.IsPromo);
    EXPECT_EQ(data.ShopsData[0].IsDeadStock, sskuOther.IsDeadStock);
}


TEST(TestSskuSelector, Test_Stock)
{
    InitGlobalLog2Null();
    TShopSkuData sskuFix = GetFixPriceSsku();
    TShopSkuData sskuMarket = GetMarketPriceSsku();
    TShopSkuData sskuOther = GetOtherSsku();

    TMarketSkuData data;
    data.ShopsData = {sskuOther, sskuFix, sskuMarket};
    NSkuSelector::AggregateMarketSkuData(data);

    EXPECT_EQ(data.TotalStock, sskuOther.Stock + sskuFix.Stock + sskuMarket.Stock);
    EXPECT_EQ(data.ShopsData[0].MaxDailySales, sskuMarket.MaxDailySales);
}

TEST(TestSskuSelector, Test_StockWithError)
{
    InitGlobalLog2Null();
    TShopSkuData sskuOther2 = GetOtherSsku2();

    TMarketSkuData data;
    data.ShopsData = {sskuOther2};
    NSkuSelector::AggregateMarketSkuData(data);

    EXPECT_EQ(data.ShopsData[0].Stock, sskuOther2.Stock);
    EXPECT_EQ(data.ShopsData[0].MaxDailySales, data.TotalStock);
}

// In case of same priorty - choose cheaper one
TEST(TestSskuSelector, Test_TwoRulesSamePriority)
{
    InitGlobalLog2Null();
    TShopSkuData sskuFix = GetFixPriceSsku();
    TShopSkuData sskuFix2 = GetFixPriceSsku2();

    TMarketSkuData data;
    data.ShopsData = {sskuFix, sskuFix2};
    NSkuSelector::AggregateMarketSkuData(data);

    EXPECT_EQ(data.ShopsData[0].LowPrice, sskuFix.LowPrice);
    EXPECT_EQ(data.ShopsData[0].HighPrice, sskuFix.HighPrice);
    EXPECT_EQ(data.ShopsData[0].PurchasePrice, sskuFix.PurchasePrice);
    EXPECT_EQ(data.ShopsData[0].IsPromo, sskuFix.IsPromo);
}

// In case of same priorty and same price, select lower id
TEST(TestSskuSelector, Test_SskuIdPriority)
{
    InitGlobalLog2Null();
    TShopSkuData sku1 = GetOtherSsku2();
    sku1.ShopSkuId = "000189.E1677900";
    sku1.PurchasePrice = 1;
    TShopSkuData sku2 = GetOtherSsku2();
    sku2.ShopSkuId = "000348.E1677900";
    sku2.PurchasePrice = 2;
    TShopSkuData sku3 = GetOtherSsku2();
    sku3.ShopSkuId = "000432.E1677900";
    sku3.PurchasePrice = 3;

    TMarketSkuData data;
    data.ShopsData = {sku1, sku2, sku3};
    NSkuSelector::AggregateMarketSkuData(data);

    EXPECT_EQ(data.ShopsData[0].LowPrice, sku1.LowPrice);
    EXPECT_EQ(data.ShopsData[0].HighPrice, sku1.HighPrice);
    EXPECT_EQ(data.ShopsData[0].PurchasePrice, sku1.PurchasePrice);
    EXPECT_EQ(data.ShopsData[0].IsPromo, sku1.IsPromo);
}

TEST(TestSskuSelector, Test_SskuIdPriority2)
{
    InitGlobalLog2Null();
    TShopSkuData sku1 = GetOtherSsku2();
    sku1.ShopSkuId = "2";
    sku1.PurchasePrice = 1;
    TShopSkuData sku2 = GetOtherSsku2();
    sku2.ShopSkuId = "1";
    sku2.PurchasePrice = 2;
    TShopSkuData sku3 = GetOtherSsku2();
    sku3.ShopSkuId = "3";
    sku3.PurchasePrice = 3;

    TMarketSkuData data;
    data.ShopsData = {sku1, sku2, sku3};
    NSkuSelector::AggregateMarketSkuData(data);

    EXPECT_EQ(data.ShopsData[0].LowPrice, sku2.LowPrice);
    EXPECT_EQ(data.ShopsData[0].HighPrice, sku2.HighPrice);
    EXPECT_EQ(data.ShopsData[0].PurchasePrice, sku2.PurchasePrice);
    EXPECT_EQ(data.ShopsData[0].IsPromo, sku2.IsPromo);
}

TEST(TestSskuSelector, Test_LowerPricePriority)
{
    InitGlobalLog2Null();
    TShopSkuData sku1 = GetFixPriceSsku();
    sku1.LowPrice = 90;
    TShopSkuData sku2 = GetOtherSsku2();
    sku2.LowPrice = 50;
    TMarketSkuData data;
    data.ShopsData = {sku1, sku2};
    NSkuSelector::AggregateMarketSkuData(data);

    EXPECT_EQ(data.ShopsData[1].PriorityLowPrice, sku1.LowPrice);
}
