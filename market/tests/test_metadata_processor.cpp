#include "processor_test_runner.h"

#include <market/idx/feeds/qparser/lib/parse_stats.h>
#include <market/idx/feeds/qparser/lib/processors/qp_metadata_processor.h>
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

using TTestMetaDataProcessor = TestProcessor<TMetaDataProcessor>;


template<typename TTestingProcessor>
Market::SessionMetadata::Feedparser RunMetadataProcessor(
    TFeedInfo& feedInfo,
    const TFeedShopInfo& feedShopInfo,
    TVector<IFeedParser::TMsgPtr>& offers
) {
    TString outputFile("fp_metadata");

    // Fill ParseStats (TParseStatsProcessor)
    auto& stats = TParseStats::Instance();
    AtomicSet(stats.RealTotalOffers, 11);
    AtomicSet(stats.AcceptedOffers, 7);
    AtomicSet(stats.IgnoredOffers, 1);
    AtomicSet(stats.TimeInDeduplication, 123);
    AtomicSet(stats.TimeInDeduplicationRequest, 234);

    AtomicSet(stats.OffersWithShopSku, 1);
    AtomicSet(stats.OffersWithShopSkuAndOfferId, 2);
    AtomicSet(stats.OffersWithShopSkuEqualsOfferId, 3);

    AtomicSet(stats.DeduplicationStats.OriginalPartnerContentUpdated, 1);
    AtomicSet(stats.DeduplicationStats.ActualPartnerContentUpdated, 2);
    AtomicSet(stats.DeduplicationStats.OriginalTermsUpdated, 3);
    AtomicSet(stats.DeduplicationStats.BindingUpdated, 4);
    AtomicSet(stats.DeduplicationStats.PartnerInfoUpdated, 5);
    AtomicSet(stats.DeduplicationStats.PartnerStocksUpdated, 6);
    AtomicSet(stats.DeduplicationStats.PartnerStocksDefaultUpdated, 7);
    AtomicSet(stats.DeduplicationStats.StatusUpdated, 8);
    AtomicSet(stats.DeduplicationStats.DeliveryUpdated, 9);
    AtomicSet(stats.DeduplicationStats.PriceUpdated, 10);
    AtomicSet(stats.DeduplicationStats.BidsUpdated, 11);
    AtomicSet(stats.DeduplicationStats.PicturesUpdated, 12);
    AtomicSet(stats.DeduplicationStats.ResolutionUpdated, 13);
    AtomicSet(stats.DeduplicationStats.MetaUpdated,14);
    AtomicSet(stats.DeduplicationStats.HandlerFailed, 15);
    AtomicSet(stats.DeduplicationStats.YtRequestFailed, 16);
    AtomicSet(stats.DeduplicationStats.SkippedByBinding, 17);
    AtomicSet(stats.DeduplicationStats.TotalSent, 18);

    {
        TTestingProcessor processor(outputFile);
        for (const auto& offer : offers) {
            processor.Process(feedInfo, feedShopInfo, offer);
        }
        processor.Flush();
    }

    ASSERT_TRUE(NFs::Exists(outputFile));

    Market::SessionMetadata::Feedparser metadata;
    TFileInput input(outputFile);
    metadata.ParseFromArcadiaStream(&input);

    return metadata;
}

TEST(TTestMetaDataProcessor, Test) {
    TFeedInfo feedInfo;
    TFeedShopInfo feedShopInfo {  // collecting throw parsing process
        .Platform = "Market 1C",
        .Version = "2.1",
    };

    // basic offer
    IWriter::TMsgPtr processed1 = MakeAtomicShared<IWriter::TMsg>(feedInfo);
    auto* originalPartnerContent1 = processed1->DataCampOffer.mutable_content()->mutable_partner()->mutable_original();
    processed1->RawOriginalPrice = "RUR 1050";
    processed1->RawVatSource = "VAT_10";
    processed1->Position = "2:0";
    processed1->DataCampOffer.mutable_identifiers()->set_offer_id("CsvCheckFeed1");
    originalPartnerContent1->mutable_name()->set_value("odin");
    auto* price1 = processed1->DataCampOffer.mutable_price()->mutable_basic()->mutable_binary_price();
    *price1 = NMarketIndexer::Common::PriceExpression();
    originalPartnerContent1->mutable_url()->set_value("http://my_test_url.ru");
    processed1->DataCampOffer.mutable_content()->mutable_binding()->mutable_partner()->set_market_sku_id(12345);
    Market::NCurrency::FillPriceExpression(&*price1, 1050, "RUR");

    // disabled offer
    IWriter::TMsgPtr processed2 = MakeAtomicShared<IWriter::TMsg>(feedInfo);
    processed2->DataCampOffer.mutable_identifiers()->set_offer_id("CsvCheckFeed2");
    processed2->IsDisabled = true;
    processed2->DataCampOffer.mutable_stock_info()->mutable_partner_stocks()->set_count(10);

    // offer with undefault currency
    IWriter::TMsgPtr processed3 = MakeAtomicShared<IWriter::TMsg>(feedInfo);
    processed3->DataCampOffer.mutable_identifiers()->set_offer_id("CsvCheckFeed3");
    auto* price3 = processed3->DataCampOffer.mutable_price()->mutable_basic()->mutable_binary_price();
    *price3 = NMarketIndexer::Common::PriceExpression();
    Market::NCurrency::FillPriceExpression(&*price3, 1050, "USD");

    TVector<IFeedParser::TMsgPtr> offers = {
        processed1,
        processed2,
        processed3,
    };

    auto metadata = RunMetadataProcessor<NMarket::TMetaDataProcessor>(feedInfo, feedShopInfo, offers);

    ASSERT_EQ(metadata.parse_stats().total_offers(), 10);
    ASSERT_EQ(metadata.parse_stats().valid_offers(), 7);
    ASSERT_EQ(metadata.parse_stats().error_offers(), 3);
    ASSERT_EQ(metadata.parse_stats().ignored_offers(), 1);
    ASSERT_EQ(metadata.deduplication_time(), 123);
    ASSERT_EQ(metadata.deduplication_request_time(), 234);

    ASSERT_EQ(metadata.offers_with_shop_sku(), 1);
    ASSERT_EQ(metadata.offers_with_shop_sku_and_offer_id(), 2);
    ASSERT_EQ(metadata.offers_with_shop_sku_equals_offer_id(), 3);

    ASSERT_EQ(metadata.deduplication_stats().original_partner_content_updated(), 1);
    ASSERT_EQ(metadata.deduplication_stats().actual_partner_content_updated(), 2);
    ASSERT_EQ(metadata.deduplication_stats().original_terms_updated(), 3);
    ASSERT_EQ(metadata.deduplication_stats().binding_updated(), 4);
    ASSERT_EQ(metadata.deduplication_stats().partner_info_updated(), 5);
    ASSERT_EQ(metadata.deduplication_stats().partner_stocks_updated(), 6);
    ASSERT_EQ(metadata.deduplication_stats().partner_stocks_default_updated(), 7);
    ASSERT_EQ(metadata.deduplication_stats().status_updated(), 8);
    ASSERT_EQ(metadata.deduplication_stats().delivery_updated(), 9);
    ASSERT_EQ(metadata.deduplication_stats().price_updated(), 10);
    ASSERT_EQ(metadata.deduplication_stats().bids_updated(), 11);
    ASSERT_EQ(metadata.deduplication_stats().pictures_updated(), 12);
    ASSERT_EQ(metadata.deduplication_stats().resolution_updated(), 13);
    ASSERT_EQ(metadata.deduplication_stats().meta_updated(), 14);
    ASSERT_EQ(metadata.deduplication_stats().handler_failed(), 15);
    ASSERT_EQ(metadata.deduplication_stats().yt_request_failed(), 16);
    ASSERT_EQ(metadata.deduplication_stats().skipped_by_binding(), 17);
    ASSERT_EQ(metadata.deduplication_stats().total_sent(), 18);

    ASSERT_STREQ(metadata.platform(), "Market 1C");
    ASSERT_STREQ(metadata.version(), "2.1");

    TString output;
    google::protobuf::util::JsonPrintOptions opt;
    opt.add_whitespace = true;
    opt.preserve_proto_field_names = true;
    google::protobuf::util::MessageToJsonString(metadata, &output, opt);
    Cerr << output << Endl;
}
