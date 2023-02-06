#include <market/idx/feeds/qparser/lib/parse_stats.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>


TEST(ParseStats, IsGoodProportion) {
    NMarket::TParseStats stats;
    AtomicSet(stats.RealTotalOffers, 10);
    AtomicSet(stats.AcceptedOffers, 7);

    ASSERT_TRUE(stats.IsGoodProportion());
}

TEST(ParseStats, IsGoodProportion2) {
    NMarket::TParseStats stats;
    AtomicSet(stats.RealTotalOffers, 10);
    AtomicSet(stats.AcceptedOffers, 10);

    ASSERT_TRUE(stats.IsGoodProportion());
}


TEST(ParseStats, IsZeroRealTotalOffers) {
    NMarket::TParseStats stats;
    AtomicSet(stats.RealTotalOffers, 0);
    AtomicSet(stats.AcceptedOffers, 7);

    ASSERT_FALSE(stats.IsGoodProportion());
}


TEST(ParseStats, IsBadPropotion) {
    NMarket::TParseStats stats;
    AtomicSet(stats.RealTotalOffers, 14);
    AtomicSet(stats.AcceptedOffers, 7);

    ASSERT_FALSE(stats.IsGoodProportion());
}


TEST(ParseStats, IsAbsolutlyBadPropotion) {
    NMarket::TParseStats stats;
    AtomicSet(stats.RealTotalOffers, 100500);
    AtomicSet(stats.AcceptedOffers, 2);

    ASSERT_FALSE(stats.IsGoodProportion());
}

TEST(ParseStats, ResetShouldSet0) {
    NMarket::TParseStats stats;
    AtomicSet(stats.RealTotalOffers, 100500);
    AtomicSet(stats.IgnoredOffers, 15);
    AtomicSet(stats.AcceptedOffers, 24);
    AtomicSet(stats.OffersWithSupplierSku, 4);
    AtomicSet(stats.TimeInDeduplication, 123123);
    AtomicSet(stats.TimeInDeduplicationRequest, 234234);

    stats.Reset();

    ASSERT_EQ(AtomicGet(stats.RealTotalOffers), 0);
    ASSERT_EQ(AtomicGet(stats.IgnoredOffers), 0);
    ASSERT_EQ(AtomicGet(stats.AcceptedOffers), 0);
    ASSERT_EQ(AtomicGet(stats.OffersWithSupplierSku), 0);
    ASSERT_EQ(AtomicGet(stats.TimeInDeduplication), 0);
    ASSERT_EQ(AtomicGet(stats.TimeInDeduplicationRequest), 0);
}

TEST(ParseStats, FillFields) {
    NMarket::TParseStats stats;
    AtomicSet(stats.RealTotalOffers, 20);
    AtomicSet(stats.IgnoredOffers, 15);
    AtomicSet(stats.UnloadedOffers, 4);

    ASSERT_EQ(stats.TotalOffers(), 5);
    ASSERT_EQ(AtomicGet(stats.IgnoredOffers), 15);
    ASSERT_EQ(stats.LoadedOffers(), 1);
}