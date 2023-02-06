#include "mock_logbroker_writer.h"
#include "writer_test_runner.h"

#include <market/idx/feeds/qparser/lib/parse_stats.h>
#include <market/idx/feeds/qparser/src/writers/offer_data_writer.h>
#include <market/idx/datacamp/proto/api/ExportMessage.pb.h>
#include <market/idx/feeds/qparser/inc/parser_config.h>

#include <util/digest/city.h>
#include <google/protobuf/util/time_util.h>

#include <library/cpp/testing/unittest/gtest.h>


using namespace NMarket;
using namespace NMarket::NQParser::NTest;

void GenerateTestData(
    TFeedInfo& feedInfo,
    TVector<IWriter::TMsg>& items,
    size_t itemsCount
) {
    // Fill ParseStats (TParseStatsProcessor)
    auto& stats = TParseStats::Instance();
    AtomicSet(stats.RealTotalOffers, 10);
    AtomicSet(stats.AcceptedOffers, 10);

    TFeedId feedId = 1069;
    TFeedId realFeedId = 8352;
    TFeedId fulfillmentFeedId = 1000000;
    TShopId shopId = 9601;
    TShopId businessId = 1111;
    TWarehouseId warehouseId = 100;
    google::protobuf::Timestamp timestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(123);


    feedInfo.FeedId = feedId;
    feedInfo.RealFeedId = realFeedId;
    feedInfo.ShopId = shopId;
    feedInfo.BusinessId = businessId;
    feedInfo.WarehouseId = warehouseId;
    feedInfo.MarketColor = NMarket::EMarketColor::MC_BLUE;
    feedInfo.FulfillmentFeedId = fulfillmentFeedId;
    feedInfo.Timestamp = timestamp;

    items.reserve(itemsCount);
    for (size_t i = 1; i <= itemsCount; ++i) {
        IWriter::TMsg item(feedInfo);
        item.DataCampOffer.mutable_identifiers()->set_offer_id("offer" + ToString(i));
        item.DataCampOffer.mutable_content()->mutable_binding()->mutable_partner()->set_market_sku_id(i);
        auto* price = item.DataCampOffer.mutable_price()->mutable_basic()->mutable_binary_price();
        *price = NMarketIndexer::Common::PriceExpression();
        price->set_price(100 * i);
        item.DataCampOffer.mutable_stock_info()->mutable_partner_stocks()->set_count(i);
        item.DataCampOffer.
            mutable_content()->
            mutable_partner()->
            mutable_original()->
            mutable_url()->
            set_value("http://shop.ru/offer/" + ToString(i));
        items.push_back(std::move(item));
    }
}

TParserConfig CreateConfig(size_t batchSize = 2) {
    TStringStream stream;
    NJson::TJsonWriter writer(&stream, true);
    writer.OpenMap();
    writer.OpenMap("logbroker");
    writer.WriteKey("sort_dc_offer_data_batch_size");
    writer.Write(batchSize);
    writer.WriteKey("sort_dc_offer_data_write_probability_percent");
    writer.Write(100);
    writer.WriteKey("sort_dc_offer_data_writers_count");
    writer.Write(2);
    writer.CloseMap();
    writer.CloseMap();
    writer.Flush();
    TFileOutput fileWriter("config.json");
    fileWriter.Write(stream.Data());
    fileWriter.Finish();
    TFsPath configPath("config.json");
    TParserConfig config({ configPath });
    configPath.DeleteIfExists();

    return config;
}


TEST(OfferDataWriter, SortDCOfferDataWriter) {
    TFeedInfo feedInfo;
    TVector<IWriter::TMsg> items;
    const ui32 itemsCount = 5;
    // const ui32 batch_size = 2;
    GenerateTestData(
        feedInfo,
        items,
        itemsCount
    );

    auto writer = MakeAtomicShared<TLogBrokerMultiPartitionWriterMock>();
    auto config = CreateConfig();
    auto sortDCSourcePlatform = Market::DataCamp::Consumer::Platform::VERTICAL_GOODS;
    RunWriterTest<TOfferDataWriter>(
        feedInfo,
        items,
        writer,
        config,
        sortDCSourcePlatform
    );

    const auto writtenData = writer->GetWrittenData();
    size_t expectedMsgCount = 3;
    ASSERT_EQ(writtenData.size(), expectedMsgCount);

    ui32 partitionsCount = config.Get<ui32>("logbroker.sort_dc_offer_data_writers_count", 0);
    {
        Market::DataCamp::API::ExportMessagesBatch exportMessagesBatch;
        exportMessagesBatch.ParseFromStringOrThrow(writtenData[0].Data);
        ASSERT_EQ(exportMessagesBatch.messages_size(), 2);

        auto message0 = exportMessagesBatch.messages(0);
        ASSERT_TRUE(message0.has_offer());
        ASSERT_EQ(message0.offer().offer_id(), "offer2");
        ASSERT_TRUE(message0.offer().has_original_content());
        ASSERT_EQ(message0.offer().original_content().url(), "http://shop.ru/offer/2");
        ASSERT_EQ(message0.offer().feed_id(), 8352);
        ASSERT_EQ((int)message0.offer().service().platform(), (int)sortDCSourcePlatform);
        ASSERT_EQ(message0.offer().shop_id(), 9601);
        ASSERT_EQ(message0.offer().business_id(), 1111);
        ASSERT_EQ(message0.offer().timestamp().seconds(), 123);

        auto message1 = exportMessagesBatch.messages(1);
        ASSERT_TRUE(message1.has_offer());
        ASSERT_EQ(message1.offer().offer_id(), "offer3");
        ASSERT_TRUE(message1.offer().has_original_content());
        ASSERT_EQ(message1.offer().original_content().url(), "http://shop.ru/offer/3");
        ASSERT_EQ(message1.offer().feed_id(), 8352);
        ASSERT_EQ((int)message1.offer().service().platform(), (int)sortDCSourcePlatform);
        ASSERT_EQ(message1.offer().shop_id(), 9601);
        ASSERT_EQ(message1.offer().business_id(), 1111);
        ASSERT_EQ(message1.offer().timestamp().seconds(), 123);

        ui32 expectedPartition0 = CityHash64(message0.offer().offer_id()) % partitionsCount + 1;
        ui32 expectedPartition1 = CityHash64(message1.offer().offer_id()) % partitionsCount + 1;
        ASSERT_EQ(expectedPartition0, expectedPartition1);
        ASSERT_EQ(writtenData[0].Partition, expectedPartition0);
    }
    {
        Market::DataCamp::API::ExportMessagesBatch exportMessagesBatch;
        exportMessagesBatch.ParseFromStringOrThrow(writtenData[1].Data);
        ASSERT_EQ(exportMessagesBatch.messages_size(), 2);

        auto message0 = exportMessagesBatch.messages(0);
        ASSERT_TRUE(message0.has_offer());
        ASSERT_EQ(message0.offer().offer_id(), "offer1");
        ASSERT_TRUE(message0.offer().has_original_content());
        ASSERT_EQ(message0.offer().original_content().url(), "http://shop.ru/offer/1");
        ASSERT_EQ(message0.offer().feed_id(), 8352);
        ASSERT_EQ((int)message0.offer().service().platform(), (int)sortDCSourcePlatform);
        ASSERT_EQ(message0.offer().shop_id(), 9601);
        ASSERT_EQ(message0.offer().business_id(), 1111);
        ASSERT_EQ(message0.offer().timestamp().seconds(), 123);

        auto message1 = exportMessagesBatch.messages(1);
        ASSERT_TRUE(message1.has_offer());
        ASSERT_EQ(message1.offer().offer_id(), "offer5");
        ASSERT_TRUE(message1.offer().has_original_content());
        ASSERT_EQ(message1.offer().original_content().url(), "http://shop.ru/offer/5");
        ASSERT_EQ(message1.offer().feed_id(), 8352);
        ASSERT_EQ((int)message1.offer().service().platform(), (int)sortDCSourcePlatform);
        ASSERT_EQ(message1.offer().shop_id(), 9601);
        ASSERT_EQ(message1.offer().business_id(), 1111);
        ASSERT_EQ(message1.offer().timestamp().seconds(), 123);

        ui32 expectedPartition0 = CityHash64(message0.offer().offer_id()) % partitionsCount + 1;
        ui32 expectedPartition1 = CityHash64(message1.offer().offer_id()) % partitionsCount + 1;
        ASSERT_EQ(expectedPartition0, expectedPartition1);
        ASSERT_EQ(writtenData[1].Partition, expectedPartition0);
    }
    {
        Market::DataCamp::API::ExportMessagesBatch exportMessagesBatch;
        exportMessagesBatch.ParseFromStringOrThrow(writtenData[2].Data);
        ASSERT_EQ(exportMessagesBatch.messages_size(), 1);

        auto message0 = exportMessagesBatch.messages(0);
        ASSERT_TRUE(message0.has_offer());
        ASSERT_EQ(message0.offer().offer_id(), "offer4");
        ASSERT_TRUE(message0.offer().has_original_content());
        ASSERT_EQ(message0.offer().original_content().url(), "http://shop.ru/offer/4");
        ASSERT_EQ(message0.offer().feed_id(), 8352);
        ASSERT_EQ((int)message0.offer().service().platform(), (int)sortDCSourcePlatform);
        ASSERT_EQ(message0.offer().shop_id(), 9601);
        ASSERT_EQ(message0.offer().business_id(), 1111);
        ASSERT_EQ(message0.offer().timestamp().seconds(), 123);

        ui32 expectedPartition0 = CityHash64(message0.offer().offer_id()) % partitionsCount + 1;
        ASSERT_EQ(writtenData[2].Partition, expectedPartition0);
    }
}
