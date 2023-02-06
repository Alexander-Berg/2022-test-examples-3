#include "market/library/partners/colors.h"
#include "mock_logbroker_writer.h"
#include "writer_test_runner.h"

#include <market/idx/feeds/qparser/lib/parse_stats.h>
#include <market/idx/feeds/qparser/src/writers/complete_feed_writer.h>

#include <market/idx/datacamp/proto/api/Commands.pb.h>
#include <market/idx/datacamp/proto/api/DatacampMessage.pb.h>
#include <market/idx/datacamp/proto/offer/DataCampOffer.pb.h>
#include <market/idx/feeds/lib/feed_parsing_task_utils.h>

#include <google/protobuf/util/time_util.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>


using namespace NMarket;
using namespace NMarket::NQParser::NTest;


void GenerateTestData(
    TFeedInfo& feedInfo,
    TVector<IWriter::TMsg>& items,
    TVector<TString>& untouchableOffersActual,
    THashMap<TString, TVector<TWarehouseId>>& untouchableMultiWhOffersActual,
    bool multiWh = false,
    size_t itemsCount = 21,
    bool addOfferUrl = false
) {
    // Fill ParseStats (TParseStatsProcessor)
    auto& stats = TParseStats::Instance();
    AtomicSet(stats.RealTotalOffers, 10);
    AtomicSet(stats.AcceptedOffers, 10);

    TFeedId feedId = 1069;
    TFeedId realFeedId = 8352;
    TFeedId fulfillmentFeedId = 1000000;
    TFeedId businessId = 7777;
    TShopId shopId = 9601;
    TWarehouseId warehouseId = 100;

    feedInfo.FeedId = feedId;
    feedInfo.RealFeedId = realFeedId;
    feedInfo.ShopId = shopId;
    feedInfo.WarehouseId = warehouseId;
    feedInfo.MarketColor = NMarket::EMarketColor::MC_BLUE;
    feedInfo.FulfillmentFeedId = fulfillmentFeedId;

    items.reserve(itemsCount);
    untouchableOffersActual.reserve(itemsCount);

    for (size_t i = 1; i <= itemsCount; ++i) {
        IWriter::TMsg item(feedInfo);
        item.DataCampOffer.mutable_identifiers()->set_offer_id("offer" + ToString(i));
        item.DataCampOffer.mutable_identifiers()->set_business_id(businessId);
        item.DataCampOffer.mutable_identifiers()->set_shop_id(shopId);
        item.DataCampOffer.mutable_identifiers()->set_warehouse_id(warehouseId);
        item.DataCampOffer.mutable_content()->mutable_binding()->mutable_partner()->set_market_sku_id(i);
        auto* price = item.DataCampOffer.mutable_price()->mutable_basic()->mutable_binary_price();
        *price = NMarketIndexer::Common::PriceExpression();
        price->set_price(100 * i);
        item.DataCampOffer.mutable_stock_info()->mutable_partner_stocks()->set_count(i);
        if (addOfferUrl) {
            item.DataCampOffer.
                mutable_content()->
                mutable_partner()->
                mutable_original()->
                mutable_url()->
                set_value("http://shop.ru/offer/" + ToString(i));
        }

        if (multiWh && i % 5 == 0) {
            auto whId = static_cast<TWarehouseId>(i);
            item.Warehouses = {{whId, TWarehouseInfo{.Instock = 10}}};
            untouchableMultiWhOffersActual[item.DataCampOffer.identifiers().offer_id()] = {whId};
        } else {
            untouchableOffersActual.push_back(item.DataCampOffer.identifiers().offer_id());
        }
        items.push_back(std::move(item));
    }
}


TEST(CompleteFeedWriter, TestShardedCommandForCompleteFeed) {
    TFeedInfo feedInfo;
    TVector<IWriter::TMsg> items;
    TVector<TString> untouchableOffersActual;
    THashMap<TString, TVector<TWarehouseId>> untouchableMultiWhOffersActual;
    GenerateTestData(feedInfo, items, untouchableOffersActual, untouchableMultiWhOffersActual);
    auto datacampMessagesWriter = MakeAtomicShared<TLogBrokerMultiWriterMock>(4u);
    auto offerWriter = MakeAtomicShared<TLogBrokerMultiWriterMock>(1u);
    auto sortDCFeedUpdatesWriter = MakeAtomicShared<TLogBrokerMultiPartitionWriterMock>();
    ui32 sortDCFeedUpdatesPartitionsCount = 4;
    TVector<Market::DataCamp::Consumer::Platform> sortDCConsumerPlatforms;
    auto sortDCSourcePlatform = Market::DataCamp::Consumer::Platform::UNKNOWN_PLATFORM;

    TFeedParsingTask feedParsingTask;
    google::protobuf::Timestamp timestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(123);
    feedParsingTask.mutable_timestamp()->CopyFrom(timestamp);
    feedParsingTask.set_type(Market::DataCamp::API::FEED_CLASS_COMPLETE);
    static constexpr ui32 batchSize = 5;
    bool enableTopicCommandsQueue = true;

    RunWriterTest<TCompleteFeedWriter>(
        feedInfo,
        items,
        datacampMessagesWriter,
        offerWriter,
        sortDCFeedUpdatesWriter,
        sortDCConsumerPlatforms,
        sortDCSourcePlatform,
        feedParsingTask,
        feedInfo,
        batchSize,
        batchSize,
        GenerateTemplateOfferForFeedType(feedParsingTask),
        MRCommandQueueContext{},
        ExplicitDisablingContext{},
        enableTopicCommandsQueue,
        sortDCFeedUpdatesPartitionsCount
    );

    const auto writtenData = datacampMessagesWriter->GetWrittenData();

    TVector<TString> untouchableOffers;
    for (const auto& serialized : writtenData) {
        Market::DataCamp::API::DatacampMessage datacampMessage;
        datacampMessage.ParseFromStringOrThrow(serialized);
        const auto params = datacampMessage.tech_command()[0].command_params().complete_feed_command_params();
        for (const auto& offer : params.untouchable_offers()) {
            untouchableOffers.push_back(offer);
        }

        ASSERT_TRUE(datacampMessage.tech_command()[0].command_params().complete_feed_command_params().has_default_offer_values());
        const Market::DataCamp::Offer& actualOfferForCompleteTask =
            datacampMessage.tech_command()[0].command_params().complete_feed_command_params().default_offer_values();
        ASSERT_TRUE(actualOfferForCompleteTask.has_status());
        ASSERT_EQ(actualOfferForCompleteTask.status().disabled().size(), 1);
        const auto& disableFlag = actualOfferForCompleteTask.status().disabled()[0];
        ASSERT_EQ(disableFlag.flag(), true);
        ASSERT_TRUE(disableFlag.has_meta());
        ASSERT_TRUE(disableFlag.meta().has_timestamp());
        ASSERT_EQ(disableFlag.meta().timestamp().seconds(), timestamp.seconds());
        ASSERT_TRUE(disableFlag.meta().has_source());
        ASSERT_TRUE(disableFlag.meta().source() == Market::DataCamp::DataSource::PUSH_PARTNER_FEED);
    }

    std::sort(untouchableOffersActual.begin(), untouchableOffersActual.end());
    std::sort(untouchableOffers.begin(), untouchableOffers.end());
    ASSERT_EQ(untouchableOffersActual, untouchableOffers);
}


TEST(CompleteFeedWriter, TestShardedCommandForStockFeed) {
    TFeedInfo feedInfo;
    TVector<IWriter::TMsg> items;
    TVector<TString> untouchableOffersActual;
    THashMap<TString, TVector<TWarehouseId>> untouchableMultiWhOffersActual;
    GenerateTestData(feedInfo, items, untouchableOffersActual, untouchableMultiWhOffersActual);

    auto datacampMessagesWriter = MakeAtomicShared<TLogBrokerMultiWriterMock>(4u);
    auto offerWriter = MakeAtomicShared<TLogBrokerMultiWriterMock>(1u);
    auto sortDCFeedUpdatesWriter = MakeAtomicShared<TLogBrokerMultiPartitionWriterMock>();
    ui32 sortDCFeedUpdatesPartitionsCount = 4;
    TVector<Market::DataCamp::Consumer::Platform> sortDCConsumerPlatforms;
    auto sortDCSourcePlatform = Market::DataCamp::Consumer::Platform::UNKNOWN_PLATFORM;

    TFeedParsingTask feedParsingTask;
    google::protobuf::Timestamp timestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(123);
    feedParsingTask.mutable_timestamp()->CopyFrom(timestamp);
    feedParsingTask.set_type(Market::DataCamp::API::FEED_CLASS_STOCK);
    static constexpr ui32 batchSize = 5;
    bool enableTopicCommandsQueue = true;

    RunWriterTest<TCompleteFeedWriter>(
        feedInfo,
        items,
        datacampMessagesWriter,
        offerWriter,
        sortDCFeedUpdatesWriter,
        sortDCConsumerPlatforms,
        sortDCSourcePlatform,
        feedParsingTask,
        feedInfo,
        batchSize,
        batchSize,
        GenerateTemplateOfferForFeedType(feedParsingTask),
        MRCommandQueueContext{},
        ExplicitDisablingContext{},
        enableTopicCommandsQueue,
        sortDCFeedUpdatesPartitionsCount
    );

    const auto writtenData = datacampMessagesWriter->GetWrittenData();

    TVector<TString> untouchableOffers;
    for (const auto& serialized : writtenData) {
        Market::DataCamp::API::DatacampMessage datacampMessage;
        datacampMessage.ParseFromStringOrThrow(serialized);
        const auto params = datacampMessage.tech_command()[0].command_params().complete_feed_command_params();
        for (const auto& offer : params.untouchable_offers()) {
            untouchableOffers.push_back(offer);
        }

        ASSERT_EQ(params.real_feed_id(), 8352);

        ASSERT_TRUE(datacampMessage.tech_command()[0].command_params().complete_feed_command_params().has_default_offer_values());
        const Market::DataCamp::Offer& actualOfferForStockTask =
            datacampMessage.tech_command()[0].command_params().complete_feed_command_params().default_offer_values();

        ASSERT_TRUE(actualOfferForStockTask.has_stock_info());
        ASSERT_TRUE(actualOfferForStockTask.stock_info().has_partner_stocks());
        const auto& partnerStocks = actualOfferForStockTask.stock_info().partner_stocks();
        ASSERT_TRUE(partnerStocks.has_count());
        ASSERT_EQ(partnerStocks.count(), 0);
        ASSERT_TRUE(partnerStocks.has_meta());
        ASSERT_TRUE(partnerStocks.meta().has_timestamp());
        ASSERT_EQ(partnerStocks.meta().timestamp().seconds(), timestamp.seconds());
        ASSERT_TRUE(partnerStocks.meta().has_source());
        ASSERT_TRUE(partnerStocks.meta().source() == Market::DataCamp::DataSource::PUSH_PARTNER_FEED);
    }

    std::sort(untouchableOffersActual.begin(), untouchableOffersActual.end());
    std::sort(untouchableOffers.begin(), untouchableOffers.end());

    ASSERT_EQ(untouchableOffersActual, untouchableOffers);
}


TEST(CompleteFeedWriter, TestShardedCommandForMultiwhCompleteFeed) {
    TFeedInfo feedInfo;
    TVector<IWriter::TMsg> items;
    TVector<TString> untouchableOffersActual;
    THashMap<TString, TVector<TWarehouseId>> untouchableMultiWhOffersActual;
    GenerateTestData(feedInfo, items, untouchableOffersActual, untouchableMultiWhOffersActual, true);
    auto datacampMessagesWriter = MakeAtomicShared<TLogBrokerMultiWriterMock>(4u);
    auto offerWriter = MakeAtomicShared<TLogBrokerMultiWriterMock>(1u);
    auto sortDCFeedUpdatesWriter = MakeAtomicShared<TLogBrokerMultiPartitionWriterMock>();
    ui32 sortDCFeedUpdatesPartitionsCount = 4;

    TFeedParsingTask feedParsingTask;
    google::protobuf::Timestamp timestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(123);
    feedParsingTask.mutable_timestamp()->CopyFrom(timestamp);
    feedParsingTask.set_type(Market::DataCamp::API::FEED_CLASS_COMPLETE);
    (*feedParsingTask.mutable_partner_warehouses_mapping())["123"].set_warehouse_id(23);
    static constexpr ui32 batchSize = 5;
    TVector<Market::DataCamp::Consumer::Platform> sortDCConsumerPlatforms;
    auto sortDCSourcePlatform = Market::DataCamp::Consumer::Platform::UNKNOWN_PLATFORM;
    bool enableTopicCommandsQueue = true;

    RunWriterTest<TCompleteFeedWriter>(
        feedInfo,
        items,
        datacampMessagesWriter,
        offerWriter,
        sortDCFeedUpdatesWriter,
        sortDCConsumerPlatforms,
        sortDCSourcePlatform,
        feedParsingTask,
        feedInfo,
        batchSize,
        batchSize,
        GenerateTemplateOfferForFeedType(feedParsingTask),
        MRCommandQueueContext{},
        ExplicitDisablingContext{},
        enableTopicCommandsQueue,
        sortDCFeedUpdatesPartitionsCount
    );

    const auto writtenData = datacampMessagesWriter->GetWrittenData();

    TVector<TString> untouchableOffers;
    for (const auto& serialized : writtenData) {
        Market::DataCamp::API::DatacampMessage datacampMessage;
        datacampMessage.ParseFromStringOrThrow(serialized);
        const auto params = datacampMessage.tech_command()[0].command_params().complete_feed_command_params();
        for (const auto& offer : params.untouchable_offers()) {
            untouchableOffers.push_back(offer);
        }
        for (const auto& [offerId, whIds] : params.untouchable_multi_wh_offers()) {
            ASSERT_TRUE(untouchableMultiWhOffersActual.contains(offerId));
            ASSERT_TRUE(whIds.warehouse_ids().size() == 1);
            ASSERT_TRUE(whIds.warehouse_ids().Get(0) == untouchableMultiWhOffersActual[offerId][0]);
        }

        ASSERT_TRUE(datacampMessage.tech_command()[0].command_params().complete_feed_command_params().has_default_offer_values());
        const Market::DataCamp::Offer& actualOfferForCompleteTask =
            datacampMessage.tech_command()[0].command_params().complete_feed_command_params().default_offer_values();
        ASSERT_TRUE(actualOfferForCompleteTask.has_status());
        ASSERT_EQ(actualOfferForCompleteTask.status().disabled().size(), 1);
        const auto& disableFlag = actualOfferForCompleteTask.status().disabled()[0];
        ASSERT_EQ(disableFlag.flag(), true);
        ASSERT_TRUE(disableFlag.has_meta());
        ASSERT_TRUE(disableFlag.meta().has_timestamp());
        ASSERT_EQ(disableFlag.meta().timestamp().seconds(), timestamp.seconds());
        ASSERT_TRUE(disableFlag.meta().has_source());
        ASSERT_TRUE(disableFlag.meta().source() == Market::DataCamp::DataSource::PUSH_PARTNER_FEED);

        ASSERT_TRUE(actualOfferForCompleteTask.has_stock_info());
        ASSERT_TRUE(actualOfferForCompleteTask.stock_info().has_partner_stocks());
        const auto& partnerStocks = actualOfferForCompleteTask.stock_info().partner_stocks();
        ASSERT_TRUE(partnerStocks.has_count());
        ASSERT_EQ(partnerStocks.count(), 0);
        ASSERT_TRUE(partnerStocks.has_meta());
        ASSERT_TRUE(partnerStocks.meta().has_timestamp());
        ASSERT_EQ(partnerStocks.meta().timestamp().seconds(), timestamp.seconds());
        ASSERT_TRUE(partnerStocks.meta().has_source());
        ASSERT_TRUE(partnerStocks.meta().source() == Market::DataCamp::DataSource::PUSH_PARTNER_FEED);
    }

    std::sort(untouchableOffersActual.begin(), untouchableOffersActual.end());
    std::sort(untouchableOffers.begin(), untouchableOffers.end());
    ASSERT_EQ(untouchableOffersActual, untouchableOffers);
}

TEST(CompleteFeedWriter, TestSortDCCommandCompleteFeed) {
    TFeedInfo feedInfo;
    TVector<IWriter::TMsg> items;
    TVector<TString> untouchableOffersActual;
    THashMap<TString, TVector<TWarehouseId>> untouchableMultiWhOffersActual;
    GenerateTestData(
        feedInfo,
        items,
        untouchableOffersActual,
        untouchableMultiWhOffersActual,
        /*multiWh=*/false,
        /*itemsCount=*/12,
        /*addOfferUrl=*/true
    );

    // Build feed parsing task.
    TFeedParsingTask feedParsingTask;
    google::protobuf::Timestamp timestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(123);
    feedInfo.Timestamp = timestamp;
    feedParsingTask.mutable_timestamp()->CopyFrom(timestamp);
    feedParsingTask.set_type(Market::DataCamp::API::FEED_CLASS_COMPLETE);
    feedParsingTask.set_business_id(7777);
    feedParsingTask.set_shop_id(8888);
    feedParsingTask.mutable_shops_dat_parameters()->set_vertical_share(true);
    feedParsingTask.mutable_shops_dat_parameters()->set_direct_standby(true);

    static constexpr ui32 batchSize = 10;
    // Make sortDcbatchSize greater than batchSize to check
    // that for SortDC commands the sortDcbatchSize is used.
    static constexpr ui32 sortDcbatchSize = 5;
    auto datacampMessagesWriter = MakeAtomicShared<TLogBrokerMultiWriterMock>(4u);
    auto offerWriter = MakeAtomicShared<TLogBrokerMultiWriterMock>(1u);
    auto sortDCFeedUpdatesWriter = MakeAtomicShared<TLogBrokerMultiPartitionWriterMock>();
    ui32 sortDCFeedUpdatesPartitionsCount = 4;
    TVector<Market::DataCamp::Consumer::Platform> sortDCConsumerPlatforms = {
        Market::DataCamp::Consumer::Platform::VERTICAL_GOODS,
        Market::DataCamp::Consumer::Platform::DIRECT
    };
    auto sortDCSourcePlatform = Market::DataCamp::Consumer::Platform::VERTICAL_GOODS;
    bool enableTopicCommandsQueue = true;

    RunWriterTest<TCompleteFeedWriter>(
        feedInfo,
        items,
        datacampMessagesWriter,
        offerWriter,
        sortDCFeedUpdatesWriter,
        sortDCConsumerPlatforms,
        sortDCSourcePlatform,
        feedParsingTask,
        feedInfo,
        batchSize,
        sortDcbatchSize,
        GenerateTemplateOfferForFeedType(feedParsingTask),
        MRCommandQueueContext{},
        ExplicitDisablingContext{},
        enableTopicCommandsQueue,
        sortDCFeedUpdatesPartitionsCount
    );

    const auto writtenData = sortDCFeedUpdatesWriter->GetWrittenData();
    size_t expectedMsgCount = 3;  // Ceil round of itemsCount/batchSize
    ASSERT_EQ(writtenData.size(), expectedMsgCount);

    // Check params equal for every message.
    TVector<Market::DataCamp::API::FeedContents> msgs;
    auto expectedPartition = (*(feedInfo.RealFeedId) % sortDCFeedUpdatesPartitionsCount) + 1;
    TVector<int> expectedPlatforms(sortDCConsumerPlatforms.begin(), sortDCConsumerPlatforms.end());
    for (const auto& item : writtenData) {
        Market::DataCamp::API::FeedContents sortDCFeedContents;
        sortDCFeedContents.ParseFromStringOrThrow(item.Data);
        ASSERT_EQ(item.Partition, expectedPartition);
        ASSERT_EQ(sortDCFeedContents.platform_size(), expectedPlatforms.size());
        TVector<int> platforms(sortDCFeedContents.platform().begin(), sortDCFeedContents.platform().end());
        ASSERT_EQ(platforms, expectedPlatforms);
        ASSERT_EQ((int)sortDCFeedContents.src_platform(), (int)sortDCSourcePlatform);
        ASSERT_EQ(sortDCFeedContents.start_parsing_ts().seconds(), timestamp.seconds());
        ASSERT_EQ(sortDCFeedContents.business_id(), feedParsingTask.business_id());
        ASSERT_EQ(sortDCFeedContents.shop_id(), feedParsingTask.shop_id());
        ASSERT_EQ(sortDCFeedContents.real_feed_id(), feedInfo.RealFeedId);
        msgs.push_back(sortDCFeedContents);
    }

    // Check first message.
    ASSERT_EQ(msgs[0].has_begin_offer(), false);  // Empty begin offer means -infinity
    ASSERT_EQ(msgs[0].offers_size(), 5);
    TVector<std::pair<TString, TString>> expected = {
        {"http://shop.ru/offer/1", "offer1"},
        {"http://shop.ru/offer/10", "offer10"},
        {"http://shop.ru/offer/11", "offer11"},
        {"http://shop.ru/offer/12", "offer12"},
        {"http://shop.ru/offer/2", "offer2"}
    };
    for (size_t i = 0; i < expected.size(); i++) {
        ASSERT_EQ(msgs[0].offers(i).url(), expected[i].first);
        ASSERT_EQ(msgs[0].offers(i).offer_id(), expected[i].second);
    }
    ASSERT_EQ(msgs[0].end_offer().url(), "http://shop.ru/offer/3");
    ASSERT_EQ(msgs[0].end_offer().offer_id(), "offer3");

    // Check second message.
    ASSERT_EQ(msgs[1].begin_offer().url(), "http://shop.ru/offer/3");
    ASSERT_EQ(msgs[1].begin_offer().offer_id(), "offer3");
    ASSERT_EQ(msgs[1].offers_size(), 5);
    expected = {
        {"http://shop.ru/offer/3", "offer3"},
        {"http://shop.ru/offer/4", "offer4"},
        {"http://shop.ru/offer/5", "offer5"},
        {"http://shop.ru/offer/6", "offer6"},
        {"http://shop.ru/offer/7", "offer7"}
    };
    for (size_t i = 0; i < expected.size(); i++) {
        ASSERT_EQ(msgs[1].offers(i).url(), expected[i].first);
        ASSERT_EQ(msgs[1].offers(i).offer_id(), expected[i].second);
    }
    ASSERT_EQ(msgs[1].end_offer().url(), "http://shop.ru/offer/8");
    ASSERT_EQ(msgs[1].end_offer().offer_id(), "offer8");

    // Check last message.
    ASSERT_EQ(msgs[2].begin_offer().url(), "http://shop.ru/offer/8");
    ASSERT_EQ(msgs[2].begin_offer().offer_id(), "offer8");
    expected = {
        {"http://shop.ru/offer/8", "offer8"},
        {"http://shop.ru/offer/9", "offer9"}
    };
    for (size_t i = 0; i < expected.size(); i++) {
        ASSERT_EQ(msgs[2].offers(i).url(), expected[i].first);
        ASSERT_EQ(msgs[2].offers(i).offer_id(), expected[i].second);
    }
    ASSERT_EQ(msgs[2].has_end_offer(), false);  // Empty end url means +infinity
}

TEST(CompleteFeedWriter, TestSortDCCommandCompleteFeedRandomPartition) {
    TFeedInfo feedInfo;
    TVector<IWriter::TMsg> items;
    TVector<TString> untouchableOffersActual;
    THashMap<TString, TVector<TWarehouseId>> untouchableMultiWhOffersActual;
    GenerateTestData(
        feedInfo,
        items,
        untouchableOffersActual,
        untouchableMultiWhOffersActual,
        /*multiWh=*/false,
        /*itemsCount=*/12,
        /*addOfferUrl=*/true
    );

    // Build feed parsing task.
    TFeedParsingTask feedParsingTask;
    google::protobuf::Timestamp timestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(123);
    feedInfo.Timestamp = timestamp;
    feedParsingTask.mutable_timestamp()->CopyFrom(timestamp);
    feedParsingTask.set_type(Market::DataCamp::API::FEED_CLASS_COMPLETE);
    feedParsingTask.set_business_id(7777);
    feedParsingTask.set_shop_id(8888);
    feedParsingTask.mutable_shops_dat_parameters()->set_vertical_share(true);
    feedParsingTask.mutable_shops_dat_parameters()->set_direct_standby(true);

    static constexpr ui32 batchSize = 5;
    auto datacampMessagesWriter = MakeAtomicShared<TLogBrokerMultiWriterMock>(4u);
    auto offerWriter = MakeAtomicShared<TLogBrokerMultiWriterMock>(1u);
    auto sortDCFeedUpdatesWriter = MakeAtomicShared<TLogBrokerMultiPartitionWriterMock>();
    ui32 sortDCFeedUpdatesPartitionsCount = 4;
    TVector<Market::DataCamp::Consumer::Platform> sortDCConsumerPlatforms = {
        Market::DataCamp::Consumer::Platform::VERTICAL_GOODS,
        Market::DataCamp::Consumer::Platform::DIRECT
    };
    auto sortDCSourcePlatform = Market::DataCamp::Consumer::Platform::UNKNOWN_PLATFORM;
    bool enableTopicCommandsQueue = true;

    RunWriterTest<TCompleteFeedWriter>(
        feedInfo,
        items,
        datacampMessagesWriter,
        offerWriter,
        sortDCFeedUpdatesWriter,
        sortDCConsumerPlatforms,
        sortDCSourcePlatform,
        feedParsingTask,
        feedInfo,
        batchSize,
        batchSize,
        GenerateTemplateOfferForFeedType(feedParsingTask),
        MRCommandQueueContext{},
        ExplicitDisablingContext{},
        enableTopicCommandsQueue,
        sortDCFeedUpdatesPartitionsCount,
        /*enableSortDCRandomPartitions=*/true
    );

    const auto writtenData = sortDCFeedUpdatesWriter->GetWrittenData();
    size_t expectedMsgCount = 3;  // Ceil round of itemsCount/batchSize
    ASSERT_EQ(writtenData.size(), expectedMsgCount);

    // Check partition is randome for every message.
    TVector<int> expectedPlatforms(sortDCConsumerPlatforms.begin(), sortDCConsumerPlatforms.end());
    for (const auto& item : writtenData) {
        ASSERT_EQ(item.Partition, 0);  // 0 means random partition for every write.
    }
}

TEST(CompleteFeedWriter, TestSortDCCommandCompleteFeedBannedPlatformsForVerticalShare) {
    TFeedInfo feedInfo;
    TVector<IWriter::TMsg> items;
    TVector<TString> untouchableOffersActual;
    THashMap<TString, TVector<TWarehouseId>> untouchableMultiWhOffersActual;
    GenerateTestData(
        feedInfo,
        items,
        untouchableOffersActual,
        untouchableMultiWhOffersActual,
        /*multiWh=*/false,
        /*itemsCount=*/12,
        /*addOfferUrl=*/true
    );

    TVector<std::pair<TMaybe<bool>, TVector<int>>> tests = {
        {Nothing(), {}},
        {true, {}},
        {false, {Market::DataCamp::Consumer::Platform::VERTICAL_GOODS}}
    };
    for (auto& [verticalShare, expectedBannedPlatforms] : tests) {
        // Build feed parsing task.
        TFeedParsingTask feedParsingTask;
        google::protobuf::Timestamp timestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(123);
        feedInfo.Timestamp = timestamp;
        feedParsingTask.mutable_timestamp()->CopyFrom(timestamp);
        feedParsingTask.set_type(Market::DataCamp::API::FEED_CLASS_COMPLETE);
        feedParsingTask.set_business_id(7777);
        feedParsingTask.set_shop_id(8888);
        if (verticalShare.Defined()) {
            feedParsingTask.mutable_shops_dat_parameters()->set_vertical_share(*verticalShare);
        }
        feedParsingTask.mutable_shops_dat_parameters()->set_direct_standby(true);
        feedParsingTask.mutable_shops_dat_parameters()->set_direct_goods_ads(true);

        static constexpr ui32 batchSize = 5;
        auto datacampMessagesWriter = MakeAtomicShared<TLogBrokerMultiWriterMock>(4u);
        auto offerWriter = MakeAtomicShared<TLogBrokerMultiWriterMock>(1u);
        auto sortDCFeedUpdatesWriter = MakeAtomicShared<TLogBrokerMultiPartitionWriterMock>();
        ui32 sortDCFeedUpdatesPartitionsCount = 4;
        TVector<Market::DataCamp::Consumer::Platform> sortDCConsumerPlatforms = {
            Market::DataCamp::Consumer::Platform::DIRECT
        };
        auto sortDCSourcePlatform = Market::DataCamp::Consumer::Platform::UNKNOWN_PLATFORM;
        bool enableTopicCommandsQueue = true;

        RunWriterTest<TCompleteFeedWriter>(
            feedInfo,
            items,
            datacampMessagesWriter,
            offerWriter,
            sortDCFeedUpdatesWriter,
            sortDCConsumerPlatforms,
            sortDCSourcePlatform,
            feedParsingTask,
            feedInfo,
            batchSize,
            batchSize,
            GenerateTemplateOfferForFeedType(feedParsingTask),
            MRCommandQueueContext{},
            ExplicitDisablingContext{},
            enableTopicCommandsQueue,
            sortDCFeedUpdatesPartitionsCount
        );

        const auto writtenData = sortDCFeedUpdatesWriter->GetWrittenData();
        size_t expectedMsgCount = 3;  // Ceil round of itemsCount/batchSize
        ASSERT_EQ(writtenData.size(), expectedMsgCount);

        for (const auto& item : writtenData) {
            Market::DataCamp::API::FeedContents sortDCFeedContents;
            sortDCFeedContents.ParseFromStringOrThrow(item.Data);
            TVector<int> bannedPlatforms(
                sortDCFeedContents.banned_platforms().begin(),
                sortDCFeedContents.banned_platforms().end()
            );
            ASSERT_EQ(bannedPlatforms, expectedBannedPlatforms);
        }
    }
}

TEST(CompleteFeedWriter, TestDirectWithParsingError) {
    TFeedInfo feedInfo;
    TVector<IWriter::TMsg> items;
    TVector<TString> untouchableOffersActual;
    THashMap<TString, TVector<TWarehouseId>> untouchableMultiWhOffersActual;
    GenerateTestData(
        feedInfo,
        items,
        untouchableOffersActual,
        untouchableMultiWhOffersActual,
        /*multiWh=*/false,
        /*itemsCount=*/12,
        /*addOfferUrl=*/true
    );

    // Build feed parsing task.
    TFeedParsingTask feedParsingTask;
    google::protobuf::Timestamp timestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(123);
    feedInfo.Timestamp = timestamp;
    feedInfo.OfferColor = EMarketColor::MC_DIRECT;

    feedParsingTask.mutable_timestamp()->CopyFrom(timestamp);
    feedParsingTask.set_type(Market::DataCamp::API::FEED_CLASS_COMPLETE);
    feedParsingTask.set_business_id(7777);
    feedParsingTask.set_shop_id(8888);
    feedParsingTask.mutable_shops_dat_parameters()->set_direct_standby(true);

    static constexpr ui32 batchSize = 5;
    auto datacampMessagesWriter = MakeAtomicShared<TLogBrokerMultiWriterMock>(4u);
    auto offerWriter = MakeAtomicShared<TLogBrokerMultiWriterMock>(1u);
    auto sortDCFeedUpdatesWriter = MakeAtomicShared<TLogBrokerMultiPartitionWriterMock>();
    ui32 sortDCFeedUpdatesPartitionsCount = 4;
    TVector<Market::DataCamp::Consumer::Platform> sortDCConsumerPlatforms = {
        Market::DataCamp::Consumer::Platform::DIRECT
    };
    auto sortDCSourcePlatform = Market::DataCamp::Consumer::Platform::UNKNOWN_PLATFORM;
    bool enableTopicCommandsQueue = true;

    RunWriterTestWithParsingError<TCompleteFeedWriter>(
        feedInfo,
        items,
        /*parsingError=*/true,
        datacampMessagesWriter,
        offerWriter,
        sortDCFeedUpdatesWriter,
        sortDCConsumerPlatforms,
        sortDCSourcePlatform,
        feedParsingTask,
        feedInfo,
        batchSize,
        batchSize,
        GenerateTemplateOfferForFeedType(feedParsingTask),
        MRCommandQueueContext{},
        ExplicitDisablingContext{
            .EnableForcedCompleteCommands = true
        },
        enableTopicCommandsQueue,
        sortDCFeedUpdatesPartitionsCount
    );

    const auto writtenData = datacampMessagesWriter->GetWrittenData();

    ASSERT_TRUE(not writtenData.empty());
    for (const auto& serialized : writtenData) {
        Market::DataCamp::API::DatacampMessage datacampMessage;
        datacampMessage.ParseFromStringOrThrow(serialized);

        ASSERT_TRUE(datacampMessage.tech_command()[0].command_params().complete_feed_command_params().has_default_offer_values());
    }

    ASSERT_TRUE(true);
}

TEST(CompleteFeedWriter, TestSortDCCommandCompleteFeedBannedPlatformsForDirect) {
    TFeedInfo feedInfo;
    TVector<IWriter::TMsg> items;
    TVector<TString> untouchableOffersActual;
    THashMap<TString, TVector<TWarehouseId>> untouchableMultiWhOffersActual;
    GenerateTestData(
        feedInfo,
        items,
        untouchableOffersActual,
        untouchableMultiWhOffersActual,
        /*multiWh=*/false,
        /*itemsCount=*/12,
        /*addOfferUrl=*/true
    );
    TVector<TMaybe<bool>> flagValues = {true, false, Nothing()};
    for (TMaybe<bool> direct_goods_ads : flagValues) {
        for (TMaybe<bool> direct_search_snippet_gallery : flagValues) {
            // Build feed parsing task.
            TFeedParsingTask feedParsingTask;
            google::protobuf::Timestamp timestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(123);
            feedInfo.Timestamp = timestamp;
            feedParsingTask.mutable_timestamp()->CopyFrom(timestamp);
            feedParsingTask.set_type(Market::DataCamp::API::FEED_CLASS_COMPLETE);
            feedParsingTask.set_business_id(7777);
            feedParsingTask.set_shop_id(8888);
            feedParsingTask.mutable_shops_dat_parameters()->set_vertical_share(true);
            if (direct_search_snippet_gallery.Defined()) {
                feedParsingTask.mutable_shops_dat_parameters()->set_direct_search_snippet_gallery(*direct_search_snippet_gallery);
            }
            if (direct_goods_ads.Defined()) {
                feedParsingTask.mutable_shops_dat_parameters()->set_direct_goods_ads(*direct_goods_ads);
            }
            static constexpr ui32 batchSize = 5;
            auto datacampMessagesWriter = MakeAtomicShared<TLogBrokerMultiWriterMock>(4u);
            auto offerWriter = MakeAtomicShared<TLogBrokerMultiWriterMock>(1u);
            auto sortDCFeedUpdatesWriter = MakeAtomicShared<TLogBrokerMultiPartitionWriterMock>();
            ui32 sortDCFeedUpdatesPartitionsCount = 4;
            TVector<Market::DataCamp::Consumer::Platform> sortDCConsumerPlatforms = {
                Market::DataCamp::Consumer::Platform::DIRECT
            };

            auto sortDCSourcePlatform = Market::DataCamp::Consumer::Platform::UNKNOWN_PLATFORM;
            bool enableTopicCommandsQueue = true;

            RunWriterTest<TCompleteFeedWriter>(
                feedInfo,
                items,
                datacampMessagesWriter,
                offerWriter,
                sortDCFeedUpdatesWriter,
                sortDCConsumerPlatforms,
                sortDCSourcePlatform,
                feedParsingTask,
                feedInfo,
                batchSize,
                batchSize,
                GenerateTemplateOfferForFeedType(feedParsingTask),
                MRCommandQueueContext{},
                ExplicitDisablingContext{},
                enableTopicCommandsQueue,
                sortDCFeedUpdatesPartitionsCount
            );

            const auto writtenData = sortDCFeedUpdatesWriter->GetWrittenData();
            size_t expectedMsgCount = 3;  // Ceil round of itemsCount/batchSize
            ASSERT_EQ(writtenData.size(), expectedMsgCount);

            for (const auto& item : writtenData) {
                Market::DataCamp::API::FeedContents sortDCFeedContents;
                sortDCFeedContents.ParseFromStringOrThrow(item.Data);
                TVector<int> bannedPlatforms(
                    sortDCFeedContents.banned_platforms().begin(),
                    sortDCFeedContents.banned_platforms().end()
                );
                bool isDirectBanned = std::find(
                    bannedPlatforms.begin(),
                    bannedPlatforms.end(),
                    Market::DataCamp::Consumer::Platform::DIRECT
                ) != bannedPlatforms.end();
                bool directBanExpected = (!direct_search_snippet_gallery.Defined() || !*direct_search_snippet_gallery)
                                         && (!direct_goods_ads.Defined() || !*direct_goods_ads);
                ASSERT_EQ(isDirectBanned, directBanExpected);
            }
        }
    }
}

TEST(CompleteFeedWriter, TestSortDCCommandCompleteFeedFakeUrl) {
    TFeedInfo feedInfo;
    feedInfo.Cpa = ECpa::REAL;

    TVector<IWriter::TMsg> items;
    TVector<TString> untouchableOffersActual;
    THashMap<TString, TVector<TWarehouseId>> untouchableMultiWhOffersActual;
    GenerateTestData(
        feedInfo,
        items,
        untouchableOffersActual,
        untouchableMultiWhOffersActual,
        /*multiWh=*/false,
        /*itemsCount=*/12,
        /*addOfferUrl=*/true
    );

    for (auto& item : items) {
        item.DataCampOffer.mutable_meta()->set_rgb(Market::DataCamp::MarketColor::BLUE);
    }

    TVector<TMaybe<bool>> flagValues = {true, false, Nothing()};
    // Build feed parsing task.
    TFeedParsingTask feedParsingTask;
    google::protobuf::Timestamp timestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(123);
    feedInfo.Timestamp = timestamp;
    feedParsingTask.mutable_timestamp()->CopyFrom(timestamp);
    feedParsingTask.set_type(Market::DataCamp::API::FEED_CLASS_COMPLETE);
    feedParsingTask.set_business_id(7777);
    feedParsingTask.set_shop_id(8888);
    feedParsingTask.set_warehouse_id(49125);
    feedParsingTask.mutable_shops_dat_parameters()->set_vertical_share(true);
    feedParsingTask.set_is_dbs(true);

    static constexpr ui32 batchSize = 5;
    auto datacampMessagesWriter = MakeAtomicShared<TLogBrokerMultiWriterMock>(4u);
    auto offerWriter = MakeAtomicShared<TLogBrokerMultiWriterMock>(1u);
    auto sortDCFeedUpdatesWriter = MakeAtomicShared<TLogBrokerMultiPartitionWriterMock>();
    ui32 sortDCFeedUpdatesPartitionsCount = 4;
    TVector<Market::DataCamp::Consumer::Platform> sortDCConsumerPlatforms = {
        Market::DataCamp::Consumer::Platform::DIRECT
    };

    auto sortDCSourcePlatform = Market::DataCamp::Consumer::Platform::UNKNOWN_PLATFORM;
    bool enableTopicCommandsQueue = true;
    bool enableSendFakeUrl = true;

    RunWriterTest<TCompleteFeedWriter>(
        feedInfo,
        items,
        datacampMessagesWriter,
        offerWriter,
        sortDCFeedUpdatesWriter,
        sortDCConsumerPlatforms,
        sortDCSourcePlatform,
        feedParsingTask,
        feedInfo,
        batchSize,
        batchSize,
        GenerateTemplateOfferForFeedType(feedParsingTask),
        MRCommandQueueContext{},
        ExplicitDisablingContext{},
        enableTopicCommandsQueue,
        sortDCFeedUpdatesPartitionsCount,
        /*enableSortDCRandomPartitions=*/false,
        enableSendFakeUrl
    );

    const auto writtenData = sortDCFeedUpdatesWriter->GetWrittenData();
    size_t expectedMsgCount = 3;  // Ceil round of itemsCount/batchSize
    ASSERT_EQ(writtenData.size(), expectedMsgCount);

    TVector<Market::DataCamp::API::FeedContents> msgs;
    for (const auto& item : writtenData) {
        Market::DataCamp::API::FeedContents sortDCFeedContents;
        sortDCFeedContents.ParseFromStringOrThrow(item.Data);
        msgs.push_back(sortDCFeedContents);
    }

    {
        TVector<TString> expected = {
            "https://www.fake.market.yandex.ru/7777/offer1/8888/49125",
            "https://www.fake.market.yandex.ru/7777/offer10/8888/49125",
            "https://www.fake.market.yandex.ru/7777/offer11/8888/49125",
            "https://www.fake.market.yandex.ru/7777/offer12/8888/49125",
            "https://www.fake.market.yandex.ru/7777/offer2/8888/49125"
        };
        ASSERT_TRUE(msgs[0].has_fake_offer_urls());
        for (size_t i = 0; i < 5; i++) {
            ASSERT_EQ(msgs[0].offers(i).url(), expected[i]);
        }
    }
    {
        TVector<TString> expected = {
            "https://www.fake.market.yandex.ru/7777/offer3/8888/49125",
            "https://www.fake.market.yandex.ru/7777/offer4/8888/49125",
            "https://www.fake.market.yandex.ru/7777/offer5/8888/49125",
            "https://www.fake.market.yandex.ru/7777/offer6/8888/49125",
            "https://www.fake.market.yandex.ru/7777/offer7/8888/49125"
        };
        ASSERT_TRUE(msgs[1].has_fake_offer_urls());
        for (size_t i = 0; i < 5; i++) {
            ASSERT_EQ(msgs[1].offers(i).url(), expected[i]);
        }
    }
    {
        TVector<TString> expected = {
            "https://www.fake.market.yandex.ru/7777/offer8/8888/49125",
            "https://www.fake.market.yandex.ru/7777/offer9/8888/49125"
        };
        ASSERT_TRUE(msgs[2].has_fake_offer_urls());
        for (size_t i = 0; i < 2; i++) {
            ASSERT_EQ(msgs[2].offers(i).url(), expected[i]);
        }
    }
}
