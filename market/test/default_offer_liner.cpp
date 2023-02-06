#include <market/report/src/default_offer.h>

#include <library/cpp/testing/unittest/gtest.h>

using namespace NMarketReport;

TEST(DefaultOfferLiner, Basics) {
    // very cheap
    // row 1, line 2
    EXPECT_EQ(2, RateDefaultModelOffer(10*(1.0 + 5.00), 10));
    // row 1, line 5
    EXPECT_EQ(5, RateDefaultModelOffer(10*(1.0 + 1.50), 10));
    // row 1, line 9
    EXPECT_EQ(9, RateDefaultModelOffer(10*(1.0 + 0.20), 10));

    // average price
    // row 6, line 2
    EXPECT_EQ(2, RateDefaultModelOffer(10000*(1.0 + 0.33), 10000));
    // row 6, line 5
    EXPECT_EQ(5, RateDefaultModelOffer(10000*(1.0 + 0.16), 10000));
    // row 6, line 9
    EXPECT_EQ(9, RateDefaultModelOffer(10000*(1.0 + 0.023), 10000));

    // very expensive
    // row 10, line 2
    EXPECT_EQ(2, RateDefaultModelOffer(1000000*(1.0 + 0.14), 1000000));
    // row 10, line 5
    EXPECT_EQ(5, RateDefaultModelOffer(1000000*(1.0 + 0.06), 1000000));
    // row 10, line 9
    EXPECT_EQ(9, RateDefaultModelOffer(1000000*(1.0 + 0.01), 1000000));
}

