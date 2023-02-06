#include <market/idx/generation/genlog-diff/src/GenlogDiff.h>
#include <market/proto/indexer/GenerationLog.pb.h>
#include <library/cpp/testing/unittest/gtest.h>


using TProto = MarketIndexer::GenerationLog::Record;
const auto DIFFERENCER = CreateMessageDifferencer();


TEST(Differencer, DistinguishesFeedsAndOffers) {
    TProto foo;
    TProto bar;

    foo.set_feed_id(1);
    foo.set_offer_id("hello");
    bar.set_feed_id(1);
    bar.set_offer_id("hello");
    EXPECT_TRUE(DIFFERENCER->Compare(foo, bar));

    foo.set_feed_id(1);
    foo.set_offer_id("hello");
    bar.set_feed_id(2);
    bar.set_offer_id("hello");
    EXPECT_FALSE(DIFFERENCER->Compare(foo, bar));

    foo.set_feed_id(1);
    foo.set_offer_id("foo");
    bar.set_feed_id(1);
    bar.set_offer_id("bar");
    EXPECT_FALSE(DIFFERENCER->Compare(foo, bar));
}
