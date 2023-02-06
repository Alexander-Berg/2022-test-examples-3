#include <library/cpp/testing/unittest/gtest.h>
#include <market/library/offer_filter/src/reader.h>
#include <market/library/offer_filter/src/writer.h>

using namespace NMarket;

TEST(OFFER_FILTER, READ_NOT_EXISTS)
{
    TOfferFilterReader reader;
    ASSERT_THROW(reader.Load("not_exists"), yexception);
}

TEST(OFFER_FILTER, WRITE_READ_EMPTY)
{
    const auto fileName = "empty.pbuf.sn";
    TOfferFilterWriter writer({});

    writer.Save(fileName);

    int count = 0;
    const auto enumerator = [&count](uint32_t feedId, const TString& offerId) {
        Y_UNUSED(feedId);
        Y_UNUSED(offerId);
        ++count;
    };

    TOfferFilterReader reader;
    reader.Load(fileName);
    reader.Enumerate(enumerator);

    ASSERT_EQ(count, 0);
}

TEST(OFFER_FILTER, WRITE_READ_NOTEMPTY)
{
    const auto fileName = "offer_filter.pbuf.sn";
    TOfferFilterWriter writer({
            {42, {TString("offer1"), TString("offer2")}}
    });

    writer.Save(fileName);

    THashSet<uint32_t> feeds;
    THashSet<TString> offers;

    const auto enumerator = [&feeds, &offers](uint32_t feedId, const TString& offerId) {
        feeds.insert(feedId);
        offers.insert(offerId);
    };

    TOfferFilterReader reader;
    reader.Load(fileName);
    reader.Enumerate(enumerator);

    THashSet<uint32_t> expectedFeeds = {42};
    ASSERT_EQ(feeds, expectedFeeds);

    THashSet<TString> expectedOffers = {"offer1", "offer2"};
    ASSERT_EQ(offers, expectedOffers);
}
