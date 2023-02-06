#include "utils.h"

#include <market/report/library/relevance/blue_market/dynamic_pricing/buybox_strategy.h>

#include <library/cpp/testing/unittest/gtest.h>

using namespace NMarket;
using namespace NMarket::NBlueMarket;
using namespace NMarket::NBlueMarket::NDynamicPricing;
using namespace NMarket::NBlueMarket::NDynamicPricing::NTestUtils;

// Adjust price to buybox
TEST(TestBuyBoxStrategy, Test_MinAllowedPrice)
{
    const float maxDiscountPercent = 10.0;
    const TFixedPointNumber price(4000.0);
    const TFixedPointNumber minAllowedPrice(3600.0);

    auto strategy = TBuyBoxPricingStrategy(maxDiscountPercent, Nothing(), false);
    strategy.Calculate(price);

    EXPECT_FIXED_POINT_EQ(strategy.GetNewPrice(), price);
    EXPECT_FIXED_POINT_EQ(strategy.GetMinAllowedPrice(), minAllowedPrice);
    EXPECT_FIXED_POINT_EQ(strategy.GetPriceBeforeDynamicStrategy().GetRef(), price);
}

TEST(TestBuyBoxStrategy, Test_ZeroDiscount)
{
    const float maxDiscountPercent = 0.0;
    const TFixedPointNumber price(4000.0);

    auto strategy = TBuyBoxPricingStrategy(maxDiscountPercent, Nothing(), false);
    strategy.Calculate(price);

    EXPECT_FIXED_POINT_EQ(strategy.GetNewPrice(), price);
    EXPECT_FIXED_POINT_EQ(strategy.GetMinAllowedPrice(), price);
    EXPECT_FIXED_POINT_EQ(strategy.GetPriceBeforeDynamicStrategy().GetRef(), price);
}
