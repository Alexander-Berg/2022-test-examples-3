#include "config.h"

#include <market/dynamic_pricing/pricing/dynamic_pricing/autostrategy/price_randomizer/price_randomizer.h>

#include <library/cpp/logger/global/global.h>
#include <library/cpp/testing/unittest/gtest.h>
#include <util/generic/maybe.h>

using namespace NMarket::NDynamicPricing;
using namespace NMarket::NDynamicPricing::NPriceRandomizer;

namespace {
    void AddSku(
        TGroupSkuData& data,
        TMarketSkuId mskuId,
        TShopSkuId sskuId,
        TPrice lowPrice,
        TPrice highPrice,
        TPrice currentPrice
    ) {
        TMarketSkuData msku;
        TShopSkuData ssku;
        ssku.LowPrice = lowPrice;
        ssku.HighPrice = highPrice;
        ssku.CurrentPrice = currentPrice;
        ssku.ShopSkuId = sskuId;
        ssku.MarketSkuId = mskuId;
        msku.ShopsData.push_back(ssku);
        data.emplace(mskuId, msku);
    }

    int RandomSeed = 42;
}

// No sku with random prices
TEST(TestPrepareDailySeed, Test_DateTime)
{
    InitGlobalLog2Null();

    const TString date = "2019-09-01";
    const auto seed = PrepareDailySeed(date);
    EXPECT_EQ(seed, 20190901);
}

// No sku with random prices
TEST(TestPriceRandomizer, Test_EmptyRandomSample)
{
    InitGlobalLog2Null();

    TGroupSkuData data;
    AddSku(data, 1, "1", 800, 1000, 900);
    AddSku(data, 2, "2", 500, 700, 600);
    AddSku(data, 3, "3", 200, 430, 300);

    const auto config = NTestConfig::CreateConfig(0.0);
    TPriceRandomizer priceRandomizer(RandomSeed);
    priceRandomizer.Randomize(config, data);

    // Expect no random prices
    EXPECT_FALSE(priceRandomizer.GetPrice("1", 1));
    EXPECT_FALSE(priceRandomizer.GetPrice("2", 2));
    EXPECT_FALSE(priceRandomizer.GetPrice("3", 3));
}

// All sku are price randomized
TEST(TestPriceRandomizer, Test_FullRandomSample)
{
    InitGlobalLog2Null();

    TGroupSkuData data;
    AddSku(data, 1, "1", 800, 1000, 900);
    AddSku(data, 2, "2", 500, 700, 600);
    AddSku(data, 3, "3", 200, 430, 300);

    const auto config = NTestConfig::CreateConfig(1.0);
    TPriceRandomizer priceRandomizer(RandomSeed);
    priceRandomizer.Randomize(config, data);

    // Expect random prices
    EXPECT_TRUE(priceRandomizer.GetPrice("1", 1));
    EXPECT_TRUE(priceRandomizer.GetPrice("2", 2));
    EXPECT_TRUE(priceRandomizer.GetPrice("3", 3));
}

// Half of sku have random prices
TEST(TestPriceRandomizer, Test_RandomSample50Percent)
{
    InitGlobalLog2Null();

    const size_t dataSize = 10;
    TGroupSkuData data;
    for (size_t i = 0; i < dataSize; ++i) {
        AddSku(data, i + 1, ToString(i + 1), i * 10, 100 + i * 10, 50 + i * 10);
    }

    const auto config = NTestConfig::CreateConfig(0.5);
    TPriceRandomizer priceRandomizer(RandomSeed);
    priceRandomizer.Randomize(config, data);

    // Expect half random prices
    size_t randomnessCounter = 0;
    for (size_t i = 0; i < dataSize; ++i) {
        if (priceRandomizer.GetPrice(ToString(i + 1), i + 1))
            ++randomnessCounter;
    }

    EXPECT_EQ(randomnessCounter, dataSize * 0.5);
}

// Check randomness
TEST(TestPriceRandomizer, Test_FoolCheckRandomness)
{
    InitGlobalLog2Null();

    TGroupSkuData data;
    AddSku(data, 1, "1", 800, 1000, 900);
    AddSku(data, 2, "2", 500, 700, 600);
    AddSku(data, 3, "3", 200, 430, 300);

    const auto config = NTestConfig::CreateConfig(1.0);

    TPriceRandomizer oldPriceRandomizer(RandomSeed);
    oldPriceRandomizer.Randomize(config, data);
    const auto oldPrice1 = oldPriceRandomizer.GetPrice("1", 1);
    const auto oldPrice2 = oldPriceRandomizer.GetPrice("2", 2);
    const auto oldPrice3 = oldPriceRandomizer.GetPrice("3", 3);

    TPriceRandomizer newPriceRandomizer(RandomSeed + 10);
    newPriceRandomizer.Randomize(config, data);
    const auto newPrice1 = newPriceRandomizer.GetPrice("1", 1);
    const auto newPrice2 = newPriceRandomizer.GetPrice("2", 2);
    const auto newPrice3 = newPriceRandomizer.GetPrice("3", 3);

    EXPECT_NE(newPrice1, oldPrice1);
    EXPECT_NE(newPrice2, oldPrice2);
    EXPECT_NE(newPrice3, oldPrice3);
}

// Check randomness repeatability (equal seed)
TEST(TestPriceRandomizer, Test_CheckEqualSeed)
{
    InitGlobalLog2Null();

    TGroupSkuData data;
    AddSku(data, 1, "1", 800, 1000, 900);
    AddSku(data, 2, "2", 500, 700, 600);
    AddSku(data, 3, "3", 200, 430, 300);

    const auto config = NTestConfig::CreateConfig(1.0);

    for (int i = 0; i < 10000; ++i) {
        TPriceRandomizer oldPriceRandomizer(RandomSeed);
        oldPriceRandomizer.Randomize(config, data);
        const auto oldPrice1 = oldPriceRandomizer.GetPrice("1", 1);
        const auto oldPrice2 = oldPriceRandomizer.GetPrice("2", 2);
        const auto oldPrice3 = oldPriceRandomizer.GetPrice("3", 3);

        TPriceRandomizer newPriceRandomizer(RandomSeed);
        newPriceRandomizer.Randomize(config, data);
        const auto newPrice1 = newPriceRandomizer.GetPrice("1", 1);
        const auto newPrice2 = newPriceRandomizer.GetPrice("2", 2);
        const auto newPrice3 = newPriceRandomizer.GetPrice("3", 3);

        EXPECT_EQ(newPrice1, oldPrice1);
        EXPECT_EQ(newPrice2, oldPrice2);
        EXPECT_EQ(newPrice3, oldPrice3);
    }
}

// Check price bounds
TEST(TestPriceRandomizer, Test_PriceBounds)
{
    InitGlobalLog2Null();

    TGroupSkuData data;
    AddSku(data, 1, "1", 800, 1000, 900);
    AddSku(data, 2, "2", 500, 500, 500);
    AddSku(data, 3, "3", 200, 201, 201);
    AddSku(data, 4, "4", 10, 150, 25);

    const auto config = NTestConfig::CreateConfig(1.0);
    TPriceRandomizer priceRandomizer(RandomSeed);
    priceRandomizer.Randomize(config, data);

    // Base case
    EXPECT_TRUE(priceRandomizer.GetPrice("1", 1) > 800 && priceRandomizer.GetPrice("1", 1) < 1000);
    // Fixed bounds
    EXPECT_EQ(priceRandomizer.GetPrice("2", 2), 500);
    // One integer between
    EXPECT_EQ(priceRandomizer.GetPrice("3", 3), 200);
    // Changes couldn't be more than 3%
    EXPECT_EQ(priceRandomizer.GetPrice("4", 4), 24);
}

// Check close price bounds
TEST(TestPriceRandomizer, Test_ClosePriceBounds)
{
    InitGlobalLog2Null();

    TGroupSkuData data;
    AddSku(data, 1, "1", 500, 500, 500);
    AddSku(data, 2, "2", 200, 200, 190);
    AddSku(data, 3, "3", 201, 202, 200);

    const auto config = NTestConfig::CreateConfig(1.0);
    TPriceRandomizer priceRandomizer(RandomSeed);
    priceRandomizer.Randomize(config, data);

    // Fixed bounds
    EXPECT_EQ(priceRandomizer.GetPrice("1", 1), 500);
    EXPECT_EQ(priceRandomizer.GetPrice("2", 2), 200);
    EXPECT_EQ(priceRandomizer.GetPrice("3", 3), 201);
}
