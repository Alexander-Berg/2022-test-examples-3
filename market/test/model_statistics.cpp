#include <market/report/library/model_statistics/model_statistics.h>

#include <market/library/types/types.h>

#include <library/cpp/testing/unittest/gtest.h>

#include <util/string/join.h>

using namespace NMarketReport;

template <>
void Out<TModelStatistics::TShopIds>(IOutputStream& o, const TModelStatistics::TShopIds& ids) {
    o << '[' << JoinSeq(", ", ids) << ']';
}

template <>
void Out<TModelStatistics::TMarketSkus>(IOutputStream& o, const TModelStatistics::TMarketSkus& skus) {
    o << "[";
    for (const auto& [key, value]: skus) {
        o << key << ": " << value << ", ";
    }
    o << "]";
}

TEST(ModelStatistics, Discount) {
    TModelStatistics stat;
    const auto modelId = 1UL;
    const auto country = EDeliveryType::Country;
    const auto priority = EDeliveryType::Priority;
    // Add item w/o discount
    stat.Update(modelId, 100UL/*price*/, 0UL/*old price*/, 0UL/*discount*/, country, false, false, false, false, Nothing(), 0, Nothing(), 0, false, false, false, false, false, false);
    EXPECT_EQ(0UL, stat.Get(modelId)->MaxDiscount);
    EXPECT_EQ(1UL, stat.Get(modelId)->Count);
    EXPECT_EQ(1UL, stat.Get(modelId)->CpcCount);
    EXPECT_EQ(0UL, stat.Get(modelId)->CpaCount);
    EXPECT_EQ(country, stat.Get(modelId)->Delivery);

    // Add item with discount and minimal price decreases, delivery is priority
    stat.Update(modelId, 90UL/*price*/, 100UL/*old price*/, 10UL/*discount*/, priority, false, false, false, false, Nothing(), 0, Nothing(), 0, false, false, false, false, false, false);
    EXPECT_EQ(10UL, stat.Get(modelId)->MaxDiscount);
    EXPECT_EQ(2UL, stat.Get(modelId)->Count);
    EXPECT_EQ(2UL, stat.Get(modelId)->CpcCount);
    EXPECT_EQ(0UL, stat.Get(modelId)->CpaCount);
    EXPECT_EQ(90UL, stat.Get(modelId)->MinPrice);
    EXPECT_EQ(100UL, stat.Get(modelId)->MinOldPrice);
    stat.Finalize();
    EXPECT_EQ(10UL, stat.Get(modelId)->MaxDiscount);
    EXPECT_EQ(100UL, stat.Get(modelId)->MinOldPrice);
    EXPECT_EQ(priority, stat.Get(modelId)->Delivery);

    // Add item with discount and minimal price not decreases, delivery is country
    stat.Update(modelId, 200UL/*price*/, 400UL/*old price*/, 50UL/*discount*/, country, false, false, false, false, Nothing(), 0, Nothing(), 0, false, false, false, false, false, false);
    EXPECT_EQ(50UL, stat.Get(modelId)->MaxDiscount);
    EXPECT_EQ(3UL, stat.Get(modelId)->Count);
    EXPECT_EQ(3UL, stat.Get(modelId)->CpcCount);
    EXPECT_EQ(0UL, stat.Get(modelId)->CpaCount);
    EXPECT_EQ(90UL, stat.Get(modelId)->MinPrice);
    EXPECT_EQ(90UL, stat.Get(modelId)->MinCpcPrice);
    EXPECT_EQ(100UL, stat.Get(modelId)->MinOldPrice);
    stat.Finalize();
    EXPECT_EQ(50UL, stat.Get(modelId)->MaxDiscount);
    EXPECT_EQ(100UL, stat.Get(modelId)->MinOldPrice);
    EXPECT_EQ(priority, stat.Get(modelId)->Delivery);
    EXPECT_EQ(false, stat.Get(modelId)->HasCpa);
    EXPECT_EQ(false, stat.Get(modelId)->Has1P);

    // Add item with smallest price
    // After Finalize useless discount info should be dropped
    stat.Update(modelId, 70UL/*price*/, 0UL/*old price*/, 0UL/*discount*/, country, false, false, false, false, Nothing(), 0, Nothing(), 0, false, false, true, true, false, false);
    EXPECT_EQ(50UL, stat.Get(modelId)->MaxDiscount);
    EXPECT_EQ(4UL, stat.Get(modelId)->Count);
    EXPECT_EQ(3UL, stat.Get(modelId)->CpcCount);
    EXPECT_EQ(1UL, stat.Get(modelId)->CpaCount);
    EXPECT_EQ(70UL, stat.Get(modelId)->MinPrice);
    EXPECT_EQ(90UL, stat.Get(modelId)->MinCpcPrice);
    EXPECT_EQ(70UL, stat.Get(modelId)->MinCpaPrice);
    EXPECT_EQ(70UL, stat.Get(modelId)->MinOldPrice);
    stat.Finalize();
    EXPECT_EQ(0UL, stat.Get(modelId)->MaxDiscount);
    EXPECT_EQ(4UL, stat.Get(modelId)->Count);
    EXPECT_EQ(70UL, stat.Get(modelId)->MinPrice);
    EXPECT_EQ(0UL, stat.Get(modelId)->MinOldPrice);
    EXPECT_EQ(true, stat.Get(modelId)->HasCpa);
    EXPECT_EQ(true, stat.Get(modelId)->Has1P);

    // Add item with cashless delivery
    // Now MinCashlessPrice should be set
    stat.Update(modelId, 71UL/*price*/, 0UL/*old price*/, 0UL/*discount*/, country, false, false, false, false, Nothing(), 0, Nothing(), 0, false, false, true, true, false, true);
    EXPECT_EQ(71UL, stat.Get(modelId)->MinCashlessPrice);
}

TEST(ModelStatistics, MinOldPrice) {
    TModelStatistics stat;

    // Add reference item.
    stat.Update(1UL/*model*/, 100UL/*price*/, 500UL/*old price*/, 80UL/*discount*/, EDeliveryType::Priority, false, false, false, false, Nothing(), 0, Nothing(), 0, false, false, false, false, false, false);
    EXPECT_EQ(500UL, stat.Get(1UL/*model*/)->MinOldPrice);

    // Add item with greater min price and lesser old min price.
    stat.Update(1UL/*model*/, 200UL/*price*/, 300UL/*old price*/, 33UL/*discount*/, EDeliveryType::Priority, false, false, false, false, Nothing(), 0, Nothing(), 0, false, false, false, false, false, false);
    EXPECT_EQ(300UL, stat.Get(1UL/*model*/)->MinOldPrice);
    stat.Finalize();
    EXPECT_EQ(300UL, stat.Get(1UL/*model*/)->MinOldPrice);
}

TEST(ModelStatistics, MinimalReferencePrice) {
    TModelStatistics stat;

    stat.Update(1UL/*model*/, 100UL/*price*/, 100UL/*old price*/, 0UL/*discount*/, EDeliveryType::Priority, false, false, false, false, Nothing(), 0, Nothing(), 0, false, false, false, false, false, false);
    EXPECT_EQ(Nothing(), stat.Get(1UL/*model*/)->MinReferencePrice);
    stat.Update(1UL/*model*/, 300UL/*price*/, 300UL/*old price*/, 0UL/*discount*/, EDeliveryType::Priority, false, false, false, true, Nothing(), 0, Nothing(), 0, false, false, false, false, false, false);
    EXPECT_EQ(300UL, stat.Get(1UL/*model*/)->MinReferencePrice);
    stat.Update(1UL/*model*/, 200UL/*price*/, 200UL/*old price*/, 0UL/*discount*/, EDeliveryType::Priority, false, false, false, true, Nothing(), 0, Nothing(), 0, false, false, false, false, false, false);
    EXPECT_EQ(200UL, stat.Get(1UL/*model*/)->MinReferencePrice);
    stat.Update(1UL/*model*/, 100UL/*price*/, 100UL/*old price*/, 0UL/*discount*/, EDeliveryType::Priority, false, false, false, false, Nothing(), 0, Nothing(), 0, false, false, false, false, false, false);
    EXPECT_EQ(200UL, stat.Get(1UL/*model*/)->MinReferencePrice);
}

TEST(ModelStatistics, ShopIds) {
    TModelStatistics stat;

    stat.Update(1UL/*model*/, 100UL/*price*/, 100UL/*old price*/, 0UL/*discount*/, EDeliveryType::Priority, false, false, false, false, 10/*shopId*/, 0, Nothing(), 0, false, false, false, false, false, false);
    stat.Update(1UL/*model*/, 100UL/*price*/, 100UL/*old price*/, 0UL/*discount*/, EDeliveryType::Priority, false, false, false, false, 11/*shopId*/, 0, Nothing(), 0, false, false, false, false, false, false);
    stat.Update(1UL/*model*/, 100UL/*price*/, 100UL/*old price*/, 0UL/*discount*/, EDeliveryType::Priority, false, false, false, false, 12/*shopId*/, 0, Nothing(), 0, false, false, false, false, false, false);
    EXPECT_EQ(TModelStatistics::TShopIds({10, 11, 12}), stat.Get(1UL/*model*/)->ShopIds);

    stat.Update(2UL/*model*/, 100UL/*price*/, 100UL/*old price*/, 0UL/*discount*/, EDeliveryType::Priority, false, false, false, false, Nothing()/*shopId*/, 0, Nothing(), 0, false, false, false, false, false, false);
    EXPECT_EQ(TModelStatistics::TShopIds(), stat.Get(2UL/*model*/)->ShopIds);
}

TEST(ModelStatistics, MarketSkus) {
    TModelStatistics stat;

    stat.Update(1UL/*model*/, 100UL/*price*/, 100UL/*old price*/, 0UL/*discount*/, EDeliveryType::Priority, false, false, false, false, Nothing()/*shopId*/, 0, TString("MarketSku1"), 0, false, false, false, false, false, false);
    stat.Update(1UL/*model*/, 100UL/*price*/, 100UL/*old price*/, 0UL/*discount*/, EDeliveryType::Priority, false, false, false, false, Nothing()/*shopId*/, 0, TString("MarketSku2"), 0, false, false, false, false, false, false);
    stat.Update(1UL/*model*/, 100UL/*price*/, 100UL/*old price*/, 0UL/*discount*/, EDeliveryType::Priority, false, false, false, false, Nothing()/*shopId*/, 0, TString("MarketSku1"), 0, false, false, false, false, false, false);

    EXPECT_EQ(2, stat.Get(1UL/*model*/)->MarketSkus.size());
    EXPECT_EQ(2, stat.Get(1UL/*model*/)->MarketSkus.at(TString("MarketSku1")));
    EXPECT_EQ(1, stat.Get(1UL/*model*/)->MarketSkus.at(TString("MarketSku2")));

    stat.Update(2UL/*model*/, 100UL/*price*/, 100UL/*old price*/, 0UL/*discount*/, EDeliveryType::Priority, false, false, false, false, Nothing()/*shopId*/, 0, Nothing(), 0, false, false, false, false, false, false);
    EXPECT_EQ(TModelStatistics::TMarketSkus(), stat.Get(2UL/*model*/)->MarketSkus);
}
