#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/env.h>
#include <market/library/trees/category_tree.h>
#include <market/report/src/experimental/report_rest_service.h>
#include <util/random/shuffle.h>

namespace {

using namespace NMarket;
using namespace NMarket::NReport;

TReferenceOfferInfo MakeOffer(uint64_t price, NMarketReport::TShopId shopId)
{
    TReferenceOfferInfo offer;
    offer.Prices.Price = TFixedPointNumber(price);
    offer.ShopId = shopId;
    return offer;
}

TEST(ReferenceOffers, FairFilter_OnlyExpensiveOffers)
{
    const uint64_t bluePrice = 100;
    const TReferenceOffersFairFilterParams fairFilter;

    {
        TReferenceOffers offers;
        for (unsigned i = 0; i < 10; ++i)
            offers.push_back(MakeOffer(105, i));

        FilterReferenceOffersFairly(offers, bluePrice, fairFilter);
        EXPECT_EQ(offers.size(), 10);
    }
    {
        TReferenceOffers offers;
        for (unsigned i = 0; i < 30; ++i)
            offers.push_back(MakeOffer(105, i));

        FilterReferenceOffersFairly(offers, bluePrice, fairFilter);
        EXPECT_EQ(offers.size(), 10);
    }
    {
        TReferenceOffers offers;
        for (unsigned i = 0; i < 8; ++i)
            offers.push_back(MakeOffer(105, i));

        FilterReferenceOffersFairly(offers, bluePrice, fairFilter);
        EXPECT_EQ(offers.size(), 8);
    }
    {
        TReferenceOffers offers = { MakeOffer(105, 0) }; // too few offers

        FilterReferenceOffersFairly(offers, bluePrice, fairFilter);
        EXPECT_EQ(offers.size(), 0);
    }
}

TEST(ReferenceOffers, FairFilter_OnlyCheapOffers)
{
    const uint64_t bluePrice = 100;
    const TReferenceOffersFairFilterParams fairFilter;

    {
        TReferenceOffers offers;
        for (unsigned i = 0; i > 10; ++i)
            offers.push_back(MakeOffer(99, i));

        FilterReferenceOffersFairly(offers, bluePrice, fairFilter);
        EXPECT_EQ(offers.size(), 0);
    }
}

TEST(ReferenceOffers, FairFilter_TooManyCheap)
{
    const uint64_t bluePrice = 100;
    const TReferenceOffersFairFilterParams fairFilter;

    {
        TReferenceOffers offers = {
            MakeOffer(101, 0), MakeOffer(99, 1)
        };
        FilterReferenceOffersFairly(offers, bluePrice, fairFilter);
        EXPECT_EQ(offers.size(), 0);
    }
    {
        TReferenceOffers offers = {
            MakeOffer(101, 0), MakeOffer(100, 1), MakeOffer(99, 2)
        };
        FilterReferenceOffersFairly(offers, bluePrice, fairFilter);
        EXPECT_EQ(offers.size(), 0);
    }
    {
        TReferenceOffers offers = {
            MakeOffer(99, 0), MakeOffer(100, 1), MakeOffer(101, 2), MakeOffer(102, 3),
        };
        FilterReferenceOffersFairly(offers, bluePrice, fairFilter);
        EXPECT_EQ(offers.size(), 0);
    }
}

TEST(ReferenceOffers, FairFilter_GoodCases)
{
    const uint64_t bluePrice = 100;
    const TReferenceOffersFairFilterParams fairFilter;

    {
        TReferenceOffers offers = {
            MakeOffer(103, 0),
            MakeOffer(101, 1),
            MakeOffer(99, 2),
            MakeOffer(100, 3),
            MakeOffer(102, 4)
        };
        FilterReferenceOffersFairly(offers, bluePrice, fairFilter);
        EXPECT_EQ(offers.size(), 5);
    }
    {
        TReferenceOffers offers = {
            MakeOffer(100, 1),
            MakeOffer(103, 2),
            MakeOffer(99, 3),
            MakeOffer(101, 4),
            MakeOffer(104, 5),
            MakeOffer(102, 6)
        };
        FilterReferenceOffersFairly(offers, bluePrice, fairFilter);
        EXPECT_EQ(offers.size(), 6);
    }
    {
        TReferenceOffers offers = {
            MakeOffer(100, 1),
            MakeOffer(103, 2),
            MakeOffer(99, 3),
            MakeOffer(101, 4),
            MakeOffer(104, 5),
            MakeOffer(97, 6),  // will be discarded as too cheap
            MakeOffer(102, 7)
        };
        FilterReferenceOffersFairly(offers, bluePrice, fairFilter);
        EXPECT_EQ(offers.size(), 6);
    }
    {
        TReferenceOffers offers;
        for (unsigned i = 0; i < 8; ++ i)
            offers.push_back(MakeOffer(101, i));
        for (unsigned i = 8; i < 30; ++ i) // only 2 offers will be considered
            offers.push_back(MakeOffer(99, i));
        Shuffle(offers.begin(), offers.end());

        FilterReferenceOffersFairly(offers, bluePrice, fairFilter);
        EXPECT_EQ(offers.size(), 10);
    }
}

TEST(ReferenceOffers, FairFilter_ChooseBestPricePerStore)
{
    const uint64_t bluePrice = 100;
    const TReferenceOffersFairFilterParams fairFilter;

    TReferenceOffers offers = {
            MakeOffer(100, 0),
            MakeOffer(101, 1),
            MakeOffer(99, 2),
            MakeOffer(100, 3),
            MakeOffer(102, 4), // cheaper offer
            MakeOffer(103, 4) // more expensive offer will be discarded
    };
    FilterReferenceOffersFairly(offers, bluePrice, fairFilter);
    EXPECT_EQ(offers.size(), 5);
}

} // namespace

