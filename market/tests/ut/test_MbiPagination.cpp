#include <market/idx/offers/lib/iworkers/MbiPaginationWorker.h>
#include <market/library/offer_id_rewriter/offer_id_rewriter.h>
#include <library/cpp/testing/unittest/gtest.h>
#include <util/stream/str.h>


TEST(FeedSessionsData, Parses) {
    TString data = ""
        "123,foo,bar,baz,100\n"
        "\n"
        "456,foo,bar,baz,200\n"
    ;
    TStringInput in(data);

    auto parsedData = ParseFeedSessions(in);
    ASSERT_EQ(2, parsedData.size());

    EXPECT_EQ(123, parsedData[0].FeedId);
    EXPECT_EQ(100, parsedData[0].NumOffers);

    EXPECT_EQ(456, parsedData[1].FeedId);
    EXPECT_EQ(200, parsedData[1].NumOffers);
}


TEST(MbiPagination, HandlesZeroCount) {
    EXPECT_EQ(0, CalcPage(0, 0));
    EXPECT_EQ(0, CalcPage(0, ULONG_MAX));
}


TEST(MbiPagination, HandlesOnePage) {
    for (uint64_t numItems = 0; numItems <= Market::SHOP_OFFERS_CHUNK_SIZE; ++numItems) {
        EXPECT_EQ(0, CalcPage(numItems, 0));
        EXPECT_EQ(0, CalcPage(numItems, 1));
        EXPECT_EQ(0, CalcPage(numItems, ULONG_MAX / 2));
        EXPECT_EQ(0, CalcPage(numItems, ULONG_MAX - 1));
        EXPECT_EQ(0, CalcPage(numItems, ULONG_MAX));
    }
}


TEST(MbiPagination, GeneralCase) {
    uint64_t numItems = 3 * Market::SHOP_OFFERS_CHUNK_SIZE;
    EXPECT_EQ(0, CalcPage(numItems, 0));
    EXPECT_EQ(0, CalcPage(numItems, 1));
    EXPECT_EQ(1, CalcPage(numItems, ULONG_MAX / 2));
    EXPECT_EQ(2, CalcPage(numItems, ULONG_MAX - 1));
    EXPECT_EQ(2, CalcPage(numItems, ULONG_MAX));
}
