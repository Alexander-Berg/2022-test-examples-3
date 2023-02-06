#include "utils.h"

#include <market/report/library/relevance/blue_market/dynamic_pricing/ref_min_price_strategy.h>

#include <library/cpp/testing/unittest/gtest.h>

using namespace NMarket;
using namespace NMarket::NBlueMarket;
using namespace NMarket::NBlueMarket::NDynamicPricing;
using namespace NMarket::NBlueMarket::NDynamicPricing::NTestUtils;

// Drop price to minimal reference if it exists, sku is from golden matrix
// and minimal reference price is reachable by nax discount
TEST(TestRefMinPriceStrategy, Test_PriceDropToRefMin)
{
    const TFixedPointNumber price(4000.0);
    const float maxDiscountPercent = 10.0;
    const auto refMinPrice = TFixedPointNumber(3800.0);

    auto strategy = TRefMinPricingStrategy(maxDiscountPercent, refMinPrice);
    strategy.Calculate(price);

    EXPECT_FIXED_POINT_EQ(strategy.GetNewPrice(), refMinPrice);
    EXPECT_FIXED_POINT_EQ(strategy.GetMinAllowedPrice(), refMinPrice);
    EXPECT_FIXED_POINT_EQ(strategy.GetPriceBeforeDynamicStrategy().GetRef(), price);
}

TEST(TestRefMinPriceStrategy, Test_ZeroDiscount)
{
    const TFixedPointNumber price(4000.0);
    const float maxDiscountPercent = 0.0;
    const auto refMinPrice = TFixedPointNumber(3800.0);

    auto strategy = TRefMinPricingStrategy(maxDiscountPercent, refMinPrice);
    strategy.Calculate(price);

    EXPECT_FIXED_POINT_EQ(strategy.GetNewPrice(), price);
    EXPECT_FIXED_POINT_EQ(strategy.GetMinAllowedPrice(), price);
    EXPECT_FIXED_POINT_EQ(strategy.GetPriceBeforeDynamicStrategy().GetRef(), price);
}

TEST(TestRefMinPriceStrategy, Test_TooLowDiscount)
{
    const TFixedPointNumber price(4000.0);
    const float maxDiscountPercent = 10.0;
    const auto refMinPrice = TFixedPointNumber(3200.0);

    auto strategy = TRefMinPricingStrategy(maxDiscountPercent, refMinPrice);
    strategy.Calculate(price);

    EXPECT_FIXED_POINT_EQ(strategy.GetNewPrice(), price);
    EXPECT_FIXED_POINT_EQ(strategy.GetMinAllowedPrice(), TFixedPointNumber(3600)); // MinAllowedPrice = max(refMinPrice, price*(1-maxDiscountPercent))
    EXPECT_FIXED_POINT_EQ(strategy.GetPriceBeforeDynamicStrategy().GetRef(), price);
}

TEST(TestRefMinPriceStrategy, Test_NoRefMinPrice)
{
    const TFixedPointNumber price(4000.0);
    const float maxDiscountPercent = 20.0;
    const auto refMinPrice = Nothing();

    auto strategy = TRefMinPricingStrategy(maxDiscountPercent, refMinPrice);
    strategy.Calculate(price);

    EXPECT_FIXED_POINT_EQ(strategy.GetNewPrice(), price);
    EXPECT_FIXED_POINT_EQ(strategy.GetMinAllowedPrice(), price);
    EXPECT_FIXED_POINT_EQ(strategy.GetPriceBeforeDynamicStrategy().GetRef(), price);
}

TEST(TestRefMinPriceStrategy, Test_PriceBelowRefMinPrice)
{
    const TFixedPointNumber price(4000.0);
    const float maxDiscountPercent = 20.0;
    const auto refMinPrice = TFixedPointNumber(4100.0);

    auto strategy = TRefMinPricingStrategy(maxDiscountPercent, refMinPrice);
    strategy.Calculate(price);

    EXPECT_FIXED_POINT_EQ(strategy.GetNewPrice(), price);
    EXPECT_FIXED_POINT_EQ(strategy.GetMinAllowedPrice(), price);
    EXPECT_FIXED_POINT_EQ(strategy.GetPriceBeforeDynamicStrategy().GetRef(), price);
}

