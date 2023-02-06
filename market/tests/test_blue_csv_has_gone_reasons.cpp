#include <market/idx/feeds/qparser/lib/processors/offer_disabled_processor.h>

#include <library/cpp/testing/unittest/gtest.h>

using namespace NMarket;


TEST(HasGoneReasons, OfferDisabledProcessorEmptyReason) {
    TFeedInfo feedInfo;
    IWriter::TMsgPtr processed = MakeAtomicShared<IWriter::TMsg>(feedInfo);

    TOfferDisabledProcessor processor;
    processor.Process(feedInfo, {}, processed);

    ASSERT_TRUE(processed->HasGoneReasons.empty());
}

TEST(HasGoneReasons, OfferDisabledProcessorDisableReason) {
    TFeedInfo feedInfo;
    IWriter::TMsgPtr processed = MakeAtomicShared<IWriter::TMsg>(feedInfo);

    processed->IsDisabled = true;

    TOfferDisabledProcessor processor;
    processor.Process(feedInfo, {}, processed);

    ASSERT_TRUE(!processed->HasGoneReasons.empty());
    ASSERT_EQ(processed->HasGoneReasons.count(MarketIndexer::GenerationLog::Record::OHGR_DISABLED), 1);
}
