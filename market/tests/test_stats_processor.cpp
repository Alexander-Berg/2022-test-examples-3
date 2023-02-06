#include "processor_test_runner.h"

#include <market/idx/feeds/qparser/lib/parse_stats.h>
#include <market/idx/feeds/qparser/lib/processors/stats_processor.h>
#include <market/idx/feeds/lib/feed_parsing_task_utils.h>
#include <market/library/currency_exchange/proto_price_expression.h>
#include <market/library/process_log/checker_process_log.h>
#include <market/library/process_log/process_log.h>
#include <market/proto/SessionMetadata.pb.h>

#include <google/protobuf/util/time_util.h>
#include <google/protobuf/util/json_util.h>

#include <library/cpp/testing/unittest/gtest.h>

#include <util/folder/tempdir.h>
#include <library/cpp/deprecated/atomic/atomic.h>
#include <util/system/fs.h>

using namespace NMarket;

class TestParseStatsProcessor : public ::testing::Test {
public:
    virtual ~TestParseStatsProcessor() = default;

protected:
    void SetUp() override {
        TParseStats::Instance().Reset();
        Processor_ = MakeHolder<TParseStatsProcessor>();
    }

    IWriter::TMsgPtr Process(const TFeedInfo& feedInfo, const TFeedShopInfo& feedShopInfo, const IFeedParser::TMsgPtr raw) {
        Processor_->Process(feedInfo, feedShopInfo, raw);
        return raw;
    }

    IWriter::TMsgPtr Process(IFeedParser::TMsgPtr raw) {
        return Process(DefaultFeedInfo_, DefaultFeedShopInfo_, raw);
    }

    THolder<TParseStatsProcessor> Processor_;

    const TFeedInfo DefaultFeedInfo_ = TFeedInfo{};
    const TFeedShopInfo DefaultFeedShopInfo_ = TFeedShopInfo{};
};

TEST_F(TestParseStatsProcessor, TestEmptyOffer) {
    TOfferCarrier initialOffer(DefaultFeedInfo_);
    const auto offer = Process(
        MakeAtomicShared<IFeedParser::TMsg>(initialOffer)
    );

    ASSERT_EQ(TParseStats::Instance().OffersWithShopSkuAndOfferId, 0);
    ASSERT_EQ(TParseStats::Instance().OffersWithShopSku, 0);
    ASSERT_EQ(TParseStats::Instance().OffersWithShopSkuEqualsOfferId, 0);
}

TEST_F(TestParseStatsProcessor, TestOnlyOfferId) {
    TOfferCarrier initialOffer(DefaultFeedInfo_);
    initialOffer.DataCampOffer.mutable_identifiers()->set_offer_id("offer_id");
    initialOffer.RawOfferId = "offer_id";
    const auto offer = Process(
        MakeAtomicShared<IFeedParser::TMsg>(initialOffer)
    );

    ASSERT_EQ(TParseStats::Instance().OffersWithShopSkuAndOfferId, 0);
    ASSERT_EQ(TParseStats::Instance().OffersWithShopSku, 0);
    ASSERT_EQ(TParseStats::Instance().OffersWithShopSkuEqualsOfferId, 0);
}

TEST_F(TestParseStatsProcessor, TestOnlyShopSku) {
    TOfferCarrier initialOffer(DefaultFeedInfo_);
    initialOffer.DataCampOffer.mutable_identifiers()->set_offer_id("shop_sku");
    initialOffer.DataCampOffer.mutable_identifiers()->mutable_extra()->set_shop_sku("shop_sku");
    const auto offer = Process(
        MakeAtomicShared<IFeedParser::TMsg>(initialOffer)
    );

    ASSERT_EQ(TParseStats::Instance().OffersWithShopSkuAndOfferId, 0);
    ASSERT_EQ(TParseStats::Instance().OffersWithShopSku, 1);
    ASSERT_EQ(TParseStats::Instance().OffersWithShopSkuEqualsOfferId, 0);
}

TEST_F(TestParseStatsProcessor, TestShopSkuAndOfferId) {
    TOfferCarrier initialOffer(DefaultFeedInfo_);
    initialOffer.RawOfferId = "offer_id";
    initialOffer.DataCampOffer.mutable_identifiers()->set_offer_id("shop_sku");
    initialOffer.DataCampOffer.mutable_identifiers()->mutable_extra()->set_shop_sku("shop_sku");
    const auto offer = Process(
        MakeAtomicShared<IFeedParser::TMsg>(initialOffer)
    );

    ASSERT_EQ(TParseStats::Instance().OffersWithShopSkuAndOfferId, 1);
    ASSERT_EQ(TParseStats::Instance().OffersWithShopSku, 1);
    ASSERT_EQ(TParseStats::Instance().OffersWithShopSkuEqualsOfferId, 0);
}

TEST_F(TestParseStatsProcessor, TestShopSkuEqualsOfferId) {
    TOfferCarrier initialOffer(DefaultFeedInfo_);
    initialOffer.RawOfferId = "shop_sku";
    initialOffer.DataCampOffer.mutable_identifiers()->set_offer_id("shop_sku");
    initialOffer.DataCampOffer.mutable_identifiers()->mutable_extra()->set_shop_sku("shop_sku");
    const auto offer = Process(
        MakeAtomicShared<IFeedParser::TMsg>(initialOffer)
    );

    ASSERT_EQ(TParseStats::Instance().OffersWithShopSkuAndOfferId, 1);
    ASSERT_EQ(TParseStats::Instance().OffersWithShopSku, 1);
    ASSERT_EQ(TParseStats::Instance().OffersWithShopSkuEqualsOfferId, 1);
}
