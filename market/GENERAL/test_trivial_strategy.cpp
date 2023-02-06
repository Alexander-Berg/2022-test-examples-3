#include "utils.h"

#include <market/report/library/relevance/blue_market/dynamic_pricing/trivial_strategy.h>

#include <library/cpp/testing/unittest/gtest.h>

using namespace NMarket;
using namespace NMarket::NBlueMarket;
using namespace NMarket::NBlueMarket::NDynamicPricing;
using namespace NMarket::NBlueMarket::NDynamicPricing::NTestUtils;

// Return same price as input was
TEST(TestTrivialStrategy, Test_Trivial)
{
    const TFixedPointNumber price(4000.0);
    auto strategy = TTrivialPricingStrategy();
    strategy.Calculate(price);

    EXPECT_FIXED_POINT_EQ(strategy.GetNewPrice(), price);
    EXPECT_FIXED_POINT_EQ(strategy.GetMinAllowedPrice(), price);
    EXPECT_TRUE(strategy.GetPriceBeforeDynamicStrategy().Empty());
}
