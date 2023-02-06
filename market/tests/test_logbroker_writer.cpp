#include "mock_logbroker_writer.h"
#include "writer_test_runner.h"

#include <market/idx/feeds/qparser/lib/datacamp_offer_converter.h>
#include <market/idx/feeds/qparser/lib/feed_info_utils.h>
#include <market/idx/feeds/qparser/src/writers/logbroker_writer.h>
#include <market/idx/feeds/qparser/src/writers/logbroker_quick_pipeline_writer.h>
#include <market/idx/feeds/qparser/inc/parser_config.h>

#include <market/idx/datacamp/lib/conversion/OfferConversions.h>
#include <market/idx/feeds/lib/feed_parsing_task_utils.h>

#include <google/protobuf/util/time_util.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/json/json_writer.h>


using namespace NMarket;
using namespace Market::DataCamp;
using namespace NMarket::NQParser::NTest;

using TBusinessId = uint32_t;

constexpr TFeedId feedId = 1069;
constexpr TFeedId fulfillmentFeedId = 1000000;
constexpr TBusinessId businessId = 10;
constexpr TShopId shopId = 9601;
constexpr TWarehouseId warehouseId = 100;

void SetMeta(UpdateMeta* meta, const google::protobuf::Timestamp& timestamp) {
    meta->mutable_timestamp()->CopyFrom(timestamp);
    meta->set_source(DataSource::PUSH_PARTNER_FEED);
}

void CheckMeta(const UpdateMeta& meta, ui64 seconds) {
    EXPECT_EQ(seconds, meta.timestamp().seconds());
    EXPECT_TRUE(DataSource::PUSH_PARTNER_FEED == meta.source());
}

void CheckFieldPlacementVersion(const TFeedParsingTask& task, const Market::DataCamp::Offer& offer, ui64 seconds) {
    if (IsSelectiveModeTask(task)) {
        EXPECT_TRUE(offer.status().has_fields_placement_version());
        EXPECT_EQ(1, offer.status().fields_placement_version().value());
        CheckMeta(offer.status().fields_placement_version().meta(), seconds);
    } else {
        EXPECT_FALSE(offer.status().has_fields_placement_version());
    }
}

TParserConfig CreateConfig(ui32 batchSize = 2) {
    TStringStream stream;
    NJson::TJsonWriter writer(&stream, true);
    writer.OpenMap();
    writer.OpenMap("logbroker");
    writer.WriteKey("batch_size");
    writer.Write(batchSize);
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

TOfferCarrier CreateOfferCarrier(const TFeedInfo& feedInfo, const ui64 seconds) {
    TOfferCarrier offerCarrier(feedInfo);
    const auto timestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(seconds);
    offerCarrier.DataCampOffer.mutable_identifiers()->set_offer_id("offer1");
    offerCarrier.DataCampOffer.mutable_content()->mutable_binding()->mutable_partner()->set_market_sku_id(1);
    SetMeta(offerCarrier.DataCampOffer.mutable_content()->mutable_binding()->mutable_partner()->mutable_meta(), timestamp);
    offerCarrier.IsDisabled = true;

    auto* content = offerCarrier.DataCampOffer.mutable_content()->mutable_partner();
    content->mutable_original()->mutable_description()->set_value("description");
    SetMeta(content->mutable_original()->mutable_description()->mutable_meta(), timestamp);
    content->mutable_original()->mutable_manufacturer()->set_value("manufacturer");
    SetMeta(content->mutable_original()->mutable_manufacturer()->mutable_meta(), timestamp);

    content->mutable_original_terms()->mutable_sales_notes()->set_value("sales notes");
    SetMeta(content->mutable_original_terms()->mutable_sales_notes()->mutable_meta(), timestamp);
    content->mutable_original_terms()->mutable_transport_unit_size()->set_value(50);
    SetMeta(content->mutable_original_terms()->mutable_transport_unit_size()->mutable_meta(), timestamp);
    content->mutable_original_terms()->mutable_seller_warranty()->set_has_warranty(true);
    SetMeta(content->mutable_original_terms()->mutable_seller_warranty()->mutable_meta(), timestamp);

    auto* pictures = offerCarrier.DataCampOffer.mutable_pictures()->mutable_partner()->mutable_original();
    pictures->add_source()->set_url("url");
    SetMeta(pictures->mutable_meta(), timestamp);

    auto* price = offerCarrier.DataCampOffer.mutable_price()->mutable_basic();
    price->mutable_binary_price()->set_price(100);
    SetMeta(price->mutable_meta(), timestamp);

    auto* originalDelivery = offerCarrier.DataCampOffer.mutable_delivery()->mutable_partner()->mutable_original();
    auto* deliveryFlag = originalDelivery->mutable_delivery();
    auto* availableFlag = originalDelivery->mutable_available();
    deliveryFlag->set_flag(true);
    SetMeta(deliveryFlag->mutable_meta(), timestamp);
    availableFlag->set_flag(true);
    SetMeta(availableFlag->mutable_meta(), timestamp);

    auto* bids = offerCarrier.DataCampOffer.mutable_bids()->mutable_bid();
    bids->set_value(99);
    SetMeta(bids->mutable_meta(), timestamp);

    auto* partnerStocks = offerCarrier.DataCampOffer.mutable_stock_info()->mutable_partner_stocks();
    partnerStocks->set_count(1);
    SetMeta(partnerStocks->mutable_meta(), timestamp);
    return offerCarrier;
}

TFeedInfo CreateFeedInfo(const ui64 seconds, NMarket::EMarketColor color=NMarket::EMarketColor::MC_BLUE) {
    TFeedInfo feedInfo;
    const auto timestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(seconds);
    feedInfo.FeedId = feedId;
    feedInfo.ShopId = shopId;
    feedInfo.WarehouseId = warehouseId;
    feedInfo.MarketColor = color;
    feedInfo.FulfillmentFeedId = fulfillmentFeedId;
    feedInfo.Timestamp = timestamp;
    return feedInfo;
}

TEST(LogbrokerWriter, SplitToBatches) {
    TFeedId feedId = 1069;
    TFeedId fulfillmentFeedId = 1000000;
    TBusinessId businessId = 10;
    TShopId shopId = 9601;
    TWarehouseId warehouseId = 100;

    constexpr ui64 seconds = 123;
    const auto timestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(seconds);

    TFeedInfo feedInfo;
    feedInfo.FeedId = feedId;
    feedInfo.ShopId = shopId;
    feedInfo.WarehouseId = warehouseId;
    feedInfo.MarketColor = NMarket::EMarketColor::MC_BLUE;
    feedInfo.FulfillmentFeedId = fulfillmentFeedId;
    feedInfo.Timestamp = timestamp;

    TVector<IWriter::TMsg> items;
    for (const auto& offerId: {"offer1", "offer2", "offer3"}) {
        IWriter::TMsg item(feedInfo);
        item.DataCampOffer.mutable_identifiers()->set_offer_id(offerId);
        items.push_back(std::move(item));
    }

    auto writer = MakeAtomicShared<TLogBrokerMultiWriterMock>();
    TFeedParsingTask feedParsingTask;

    feedParsingTask.set_business_id(businessId);

    RunWriterTest<TLogBrokerWriter>(feedInfo, items, writer, feedParsingTask, CreateConfig());

    // 2 батча - первый размером 2, второй размеров 1
    EXPECT_EQ(2, writer->GetWrittenData().size());
    {
        TDatacampMessage message;
        message.ParseFromStringOrThrow(writer->GetWrittenData()[0]);

        EXPECT_TRUE(message.offers().empty());
        EXPECT_FALSE(message.united_offers().empty());

        const auto& unitedBatch = message.united_offers(0);
        EXPECT_EQ(2, unitedBatch.offer().size());

        const auto& unitedOffer1 = unitedBatch.offer(0);
        EXPECT_EQ("offer1", unitedOffer1.basic().identifiers().offer_id());

        const auto& unitedOffer2 = unitedBatch.offer(1);
        EXPECT_EQ("offer2", unitedOffer2.basic().identifiers().offer_id());
    }
    {
        TDatacampMessage message;
        message.ParseFromStringOrThrow(writer->GetWrittenData()[1]);

        EXPECT_TRUE(message.offers().empty());
        EXPECT_FALSE(message.united_offers().empty());

        const auto& unitedBatch = message.united_offers(0);
        EXPECT_EQ(1, unitedBatch.offer().size());

        const auto& unitedOffer3 = unitedBatch.offer(0);
        EXPECT_EQ("offer3", unitedOffer3.basic().identifiers().offer_id());
    }
}

TEST(LogbrokerWriter, UnitedFullFeedNotFF) {
    TFeedId feedId = 1069;
    TFeedId fulfillmentFeedId = 1000000;
    TBusinessId businessId = 10;
    TShopId shopId = 9601;
    TWarehouseId warehouseId = 100;

    constexpr ui64 seconds = 123;
    const auto timestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(seconds);

    TFeedInfo feedInfo;
    feedInfo.FeedId = feedId;
    feedInfo.ShopId = shopId;
    feedInfo.WarehouseId = warehouseId;
    feedInfo.MarketColor = NMarket::EMarketColor::MC_BLUE;
    feedInfo.FulfillmentFeedId = fulfillmentFeedId;
    feedInfo.Timestamp = timestamp;

    TVector<IWriter::TMsg> items;
    {
        IWriter::TMsg item(feedInfo);
        item.DataCampOffer.mutable_identifiers()->set_offer_id("offer1");
        item.DataCampOffer.mutable_content()->mutable_binding()->mutable_partner()->set_market_sku_id(1);
        item.IsDisabled = true;

        auto* price = item.DataCampOffer.mutable_price()->mutable_basic();
        price->mutable_binary_price()->set_price(100);
        SetMeta(price->mutable_meta(), timestamp);

        auto* partnerStocks = item.DataCampOffer.mutable_stock_info()->mutable_partner_stocks();
        partnerStocks->set_count(1);
        SetMeta(partnerStocks->mutable_meta(), timestamp);

        items.push_back(std::move(item));
    }

    auto writer = MakeAtomicShared<TLogBrokerMultiWriterMock>();

    TFeedParsingTask feedParsingTask;
    feedParsingTask.set_business_id(businessId);

    RunWriterTest<TLogBrokerWriter>(feedInfo, items, writer, feedParsingTask, CreateConfig());

    EXPECT_EQ(1, writer->GetWrittenData().size());
    {
        TDatacampMessage message;
        message.ParseFromStringOrThrow(writer->GetWrittenData()[0]);

        EXPECT_TRUE(message.offers().empty());
        EXPECT_FALSE(message.united_offers().empty());

        const auto& unitedBatch = message.united_offers(0);
        EXPECT_EQ(1, unitedBatch.offer().size());

        const auto& unitedOffer = unitedBatch.offer(0);
        const auto& basicOffer = unitedOffer.basic();
        EXPECT_EQ(1, unitedOffer.service().size());
        const auto& serviceOffer = unitedOffer.service().at(shopId);

        {
            // базовый оффер
            EXPECT_EQ(businessId, basicOffer.identifiers().business_id());
            EXPECT_FALSE(basicOffer.identifiers().has_feed_id());
            EXPECT_EQ("offer1", basicOffer.identifiers().offer_id());

            EXPECT_TRUE(basicOffer.status().incomplete_wizard().has_flag());
            EXPECT_EQ(false, basicOffer.status().incomplete_wizard().flag());
        }
        {
            // сервисный оффер
            EXPECT_EQ(businessId, serviceOffer.identifiers().business_id());
            EXPECT_EQ(shopId, serviceOffer.identifiers().shop_id());
            EXPECT_EQ(warehouseId, serviceOffer.identifiers().warehouse_id());
            EXPECT_EQ(feedId, serviceOffer.identifiers().feed_id());
            EXPECT_EQ("offer1", serviceOffer.identifiers().offer_id());

            // Не отправляем в piper значения extra, т.к. эти значение должны буть получены от УК во время майнинга
            EXPECT_FALSE(serviceOffer.identifiers().has_extra());

            EXPECT_TRUE(MarketColor::BLUE == serviceOffer.meta().rgb());
            EXPECT_EQ(seconds, serviceOffer.meta().ts_created().seconds());

            EXPECT_EQ(100, serviceOffer.price().basic().binary_price().price());
            CheckMeta(serviceOffer.price().basic().meta(), seconds);

            // кладем стоки в сервисную часть, чтобы затем использовать их в ручке add_warehouse
            EXPECT_EQ(1, serviceOffer.stock_info().partner_stocks().count());
            CheckMeta(serviceOffer.stock_info().partner_stocks().meta(), seconds);
        }
    }
}

TEST(LogbrokerWriter, UnitedStockFeedNotFF) {
    TFeedId feedId = 1069;
    TFeedId fulfillmentFeedId = 1000000;
    TBusinessId businessId = 10;
    TShopId shopId = 9601;
    TWarehouseId warehouseId = 100;

    constexpr ui64 seconds = 123;
    const auto timestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(seconds);

    TFeedInfo feedInfo;
    feedInfo.FeedId = feedId;
    feedInfo.ShopId = shopId;
    feedInfo.WarehouseId = warehouseId;
    feedInfo.MarketColor = NMarket::EMarketColor::MC_BLUE;
    feedInfo.FulfillmentFeedId = fulfillmentFeedId;
    feedInfo.Timestamp = timestamp;

    TVector<IWriter::TMsg> items;
    {
        IWriter::TMsg item(feedInfo);
        item.DataCampOffer.mutable_identifiers()->set_offer_id("offer1");

        auto* price = item.DataCampOffer.mutable_price()->mutable_basic();
        price->mutable_binary_price()->set_price(100);
        SetMeta(price->mutable_meta(), timestamp);

        auto* partnerStocks = item.DataCampOffer.mutable_stock_info()->mutable_partner_stocks();
        partnerStocks->set_count(1);
        SetMeta(partnerStocks->mutable_meta(), timestamp);

        items.push_back(std::move(item));
    }

    auto writer = MakeAtomicShared<TLogBrokerMultiWriterMock>();

    TFeedParsingTask feedParsingTask;
    feedParsingTask.set_type(Market::DataCamp::API::FEED_CLASS_STOCK);
    feedParsingTask.set_business_id(businessId);

    RunWriterTest<TLogBrokerWriter>(feedInfo, items, writer, feedParsingTask, CreateConfig());

    EXPECT_EQ(1, writer->GetWrittenData().size());
    {
        TDatacampMessage message;
        message.ParseFromStringOrThrow(writer->GetWrittenData()[0]);

        EXPECT_TRUE(message.offers().empty());
        EXPECT_FALSE(message.united_offers().empty());

        const auto& unitedBatch = message.united_offers(0);
        EXPECT_EQ(1, unitedBatch.offer().size());

        const auto& unitedOffer = unitedBatch.offer(0);
        EXPECT_TRUE(unitedOffer.has_basic());
        EXPECT_EQ(1, unitedOffer.service().size());
        const auto& serviceOffer = unitedOffer.service().at(shopId);

        // кладем только стоки в сервисную часть, если тип фида - FEED_CLASS_STOCK
        EXPECT_FALSE(serviceOffer.has_price());

        EXPECT_EQ(1, serviceOffer.stock_info().partner_stocks().count());
        CheckMeta(serviceOffer.stock_info().partner_stocks().meta(), seconds);
    }
}

TEST(LogbrokerWriter, UnitedFullFeedFF) {
    TFeedId feedId = 1069;
    TFeedId fulfillmentFeedId = 1000000;
    TBusinessId businessId = 10;
    TShopId shopId = 9601;
    TWarehouseId warehouseId = 0;

    constexpr ui64 seconds = 123;
    const auto timestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(seconds);

    TFeedInfo feedInfo;
    feedInfo.FeedId = feedId;
    feedInfo.ShopId = shopId;
    feedInfo.WarehouseId = warehouseId;
    feedInfo.MarketColor = NMarket::EMarketColor::MC_BLUE;
    feedInfo.FulfillmentFeedId = fulfillmentFeedId;
    feedInfo.Timestamp = timestamp;

    TVector<IWriter::TMsg> items;
    {
        IWriter::TMsg item(feedInfo);

        item.DataCampOffer.mutable_identifiers()->set_offer_id("offer1");
        item.DataCampOffer.mutable_content()->mutable_binding()->mutable_partner()->set_market_sku_id(1);

        auto* price = item.DataCampOffer.mutable_price()->mutable_basic();
        price->mutable_binary_price()->set_price(100);
        SetMeta(price->mutable_meta(), timestamp);

        auto* partnerStocks = item.DataCampOffer.mutable_stock_info()->mutable_partner_stocks();
        partnerStocks->set_count(1);
        SetMeta(partnerStocks->mutable_meta(), timestamp);

        auto* originalPartnerContent = item.DataCampOffer.mutable_content()->mutable_partner()->mutable_original();

        originalPartnerContent->mutable_name()->set_value("name");
        SetMeta(originalPartnerContent->mutable_name()->mutable_meta(), timestamp);
        originalPartnerContent->mutable_vendor()->set_value("vendor");
        SetMeta(originalPartnerContent->mutable_vendor()->mutable_meta(), timestamp);
        originalPartnerContent->mutable_manufacturer_warranty()->set_flag(true);
        SetMeta(originalPartnerContent->mutable_manufacturer_warranty()->mutable_meta(), timestamp);
        originalPartnerContent->mutable_weight()->set_grams(2000);
        SetMeta(originalPartnerContent->mutable_weight()->mutable_meta(), timestamp);
        originalPartnerContent->mutable_dimensions()->set_length_mkm(10 * 10000);
        SetMeta(originalPartnerContent->mutable_dimensions()->mutable_meta(), timestamp);

        // This is temporary crouch until we remove partner_content_desc
        item.Type = EOfferType::BOOK;

        auto offerType = NMarket::NDataCamp::OfferTypeToProductType(EOfferType::BOOK);
        originalPartnerContent->mutable_type()->set_value(offerType);
        SetMeta(originalPartnerContent->mutable_type()->mutable_meta(), timestamp);

        items.push_back(std::move(item));
    }

    auto writer = MakeAtomicShared<TLogBrokerMultiWriterMock>();

    TFeedParsingTask feedParsingTask;
    feedParsingTask.set_business_id(businessId);

    RunWriterTest<TLogBrokerWriter>(feedInfo, items, writer, feedParsingTask, CreateConfig());

    EXPECT_EQ(1, writer->GetWrittenData().size());
    {
        TDatacampMessage message;
        message.ParseFromStringOrThrow(writer->GetWrittenData()[0]);

        EXPECT_TRUE(message.offers().empty());
        EXPECT_FALSE(message.united_offers().empty());

        const auto& unitedBatch = message.united_offers(0);
        EXPECT_EQ(1, unitedBatch.offer().size());

        const auto& unitedOffer = unitedBatch.offer(0);
        const auto& basicOffer = unitedOffer.basic();
        EXPECT_EQ(1, unitedOffer.service().size());
        const auto& serviceOffer = unitedOffer.service().at(shopId);

        {
            EXPECT_EQ(businessId, basicOffer.identifiers().business_id());
            EXPECT_FALSE(basicOffer.identifiers().has_feed_id());
            EXPECT_EQ("offer1", basicOffer.identifiers().offer_id());

            EXPECT_TRUE(basicOffer.status().incomplete_wizard().has_flag());
            EXPECT_EQ(false, basicOffer.status().incomplete_wizard().flag());
        }
        {
            EXPECT_EQ(businessId, serviceOffer.identifiers().business_id());
            EXPECT_EQ(shopId, serviceOffer.identifiers().shop_id());
            EXPECT_EQ(feedId, serviceOffer.identifiers().feed_id());
            EXPECT_EQ(0, serviceOffer.identifiers().warehouse_id());
            EXPECT_EQ("offer1", serviceOffer.identifiers().offer_id());

            EXPECT_EQ(100, serviceOffer.price().basic().binary_price().price());
            CheckMeta(serviceOffer.price().basic().meta(), seconds);

            // кладем партнерские стоки в сервисный оффер, чтобы затем использовать их в ручке add_warehouse
            EXPECT_FALSE(serviceOffer.stock_info().has_partner_stocks());
            EXPECT_EQ(serviceOffer.stock_info().partner_stocks_default().count(), 1);

            EXPECT_FALSE(serviceOffer.content().partner().original().name().has_value());
        }
    }

    auto writerInUnitedCatalog = MakeAtomicShared<TLogBrokerMultiWriterMock>();
    feedInfo.PushFeedClass = Market::DataCamp::API::FEED_CLASS_COMPLETE;
    RunWriterTest<TLogBrokerWriter>(feedInfo, items, writerInUnitedCatalog, feedParsingTask, CreateConfig());

    EXPECT_EQ(1, writerInUnitedCatalog->GetWrittenData().size());
    {
        TDatacampMessage message;
        message.ParseFromStringOrThrow(writerInUnitedCatalog->GetWrittenData()[0]);

        EXPECT_TRUE(message.offers().empty());
        EXPECT_FALSE(message.united_offers().empty());

        const auto& unitedBatch = message.united_offers(0);
        EXPECT_EQ(1, unitedBatch.offer().size());

        const auto& unitedOffer = unitedBatch.offer(0);
        const auto& basicOffer = unitedOffer.basic();
        EXPECT_EQ(1, unitedOffer.service().size());
        const auto& serviceOffer = unitedOffer.service().at(shopId);

        {
            EXPECT_EQ(businessId, basicOffer.identifiers().business_id());
            EXPECT_FALSE(basicOffer.identifiers().has_feed_id());
            EXPECT_EQ("offer1", basicOffer.identifiers().offer_id());

            EXPECT_TRUE(basicOffer.status().incomplete_wizard().has_flag());
            EXPECT_EQ(false, basicOffer.status().incomplete_wizard().flag());
        }
        {
            EXPECT_EQ(businessId, serviceOffer.identifiers().business_id());
            EXPECT_EQ(shopId, serviceOffer.identifiers().shop_id());
            EXPECT_EQ(feedId, serviceOffer.identifiers().feed_id());
            EXPECT_EQ(0, serviceOffer.identifiers().warehouse_id());
            EXPECT_EQ("offer1", serviceOffer.identifiers().offer_id());

            EXPECT_EQ(100, serviceOffer.price().basic().binary_price().price());
            CheckMeta(serviceOffer.price().basic().meta(), seconds);

            EXPECT_FALSE(serviceOffer.stock_info().has_partner_stocks());
            EXPECT_EQ(serviceOffer.stock_info().partner_stocks_default().count(), 1);

            // В режиме ЕКАТ контент не попадают в оффер
            EXPECT_FALSE(serviceOffer.content().has_partner());
        }
    }
}

TEST(LogbrokerWriter, UnitedStockFeedFF) {
    TFeedId feedId = 1069;
    TFeedId fulfillmentFeedId = 1000000;
    TBusinessId businessId = 10;
    TShopId shopId = 9601;
    TWarehouseId warehouseId = 0;

    constexpr ui64 seconds = 123;
    const auto timestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(seconds);

    TFeedInfo feedInfo;
    feedInfo.FeedId = feedId;
    feedInfo.ShopId = shopId;
    feedInfo.WarehouseId = warehouseId;
    feedInfo.MarketColor = NMarket::EMarketColor::MC_BLUE;
    feedInfo.FulfillmentFeedId = fulfillmentFeedId;
    feedInfo.Timestamp = timestamp;

    TVector<IWriter::TMsg> items;
    {
        IWriter::TMsg item(feedInfo);
        item.DataCampOffer.mutable_identifiers()->set_offer_id("offer1");

        auto* price = item.DataCampOffer.mutable_price()->mutable_basic();
        price->mutable_binary_price()->set_price(100);
        SetMeta(price->mutable_meta(), timestamp);

        auto* partnerStocks = item.DataCampOffer.mutable_stock_info()->mutable_partner_stocks();
        partnerStocks->set_count(1);
        SetMeta(partnerStocks->mutable_meta(), timestamp);

        items.push_back(std::move(item));
    }

    auto writer = MakeAtomicShared<TLogBrokerMultiWriterMock>();
    TFeedParsingTask feedParsingTask;

    feedParsingTask.set_type(Market::DataCamp::API::FEED_CLASS_STOCK);
    feedParsingTask.set_business_id(businessId);

    RunWriterTest<TLogBrokerWriter>(feedInfo, items, writer, feedParsingTask, CreateConfig());

    EXPECT_EQ(1, writer->GetWrittenData().size());
    {
        TDatacampMessage message;
        message.ParseFromStringOrThrow(writer->GetWrittenData()[0]);

        EXPECT_TRUE(message.offers().empty());
        EXPECT_FALSE(message.united_offers().empty());

        const auto& unitedBatch = message.united_offers(0);
        EXPECT_EQ(1, unitedBatch.offer().size());

        // кладем партнерские стоки в сервисный оффер, чтобы затем использовать их в ручке add_warehouse
        const auto& unitedOffer = unitedBatch.offer(0);
        EXPECT_EQ(1, unitedOffer.service().size());
        const auto& serviceOffer = unitedOffer.service().at(shopId);
        EXPECT_FALSE(serviceOffer.stock_info().has_partner_stocks());
        EXPECT_EQ(serviceOffer.stock_info().partner_stocks_default().count(), 1);
    }
}

TEST(LogbrokerWriter, ConvertToUnitedInSelectiveMode) {
    constexpr ui64 seconds = 123;
    const auto timestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(seconds);
    TFeedInfo feedInfo = CreateFeedInfo(seconds);
    TOfferCarrier offerCarrier = CreateOfferCarrier(feedInfo, seconds);

    TFeedParsingTask feedParsingTask;
    feedParsingTask.set_business_id(businessId);

    for (auto& taskType: {Market::DataCamp::API::FEED_CLASS_UPDATE,
                          Market::DataCamp::API::FEED_CLASS_SELECTIVE_BASIC_PATCH_UPDATE_SERVICE_FULL_COMPLETE,
                          Market::DataCamp::API::FEED_CLASS_SELECTIVE_BASIC_PATCH_UPDATE_SERVICE_PATCH_UPDATE
                         }
    ) {
        feedParsingTask.set_type(taskType);

        auto unitedOffer = ConvertToUnitedInSelectiveMode<TUnitedOffer>(feedParsingTask, offerCarrier, feedInfo);

        {
            const auto& basicOffer = unitedOffer.basic();
            EXPECT_EQ(businessId, basicOffer.identifiers().business_id());
            EXPECT_FALSE(basicOffer.identifiers().has_feed_id());
            EXPECT_EQ("offer1", basicOffer.identifiers().offer_id());

            EXPECT_EQ(1, basicOffer.content().binding().partner().market_sku_id());
            CheckMeta(basicOffer.content().binding().partner().meta(), seconds);

            const auto& content = basicOffer.content().partner();
            EXPECT_EQ("description", content.original().description().value());
            CheckMeta(content.original().description().meta(), seconds);
            EXPECT_EQ("manufacturer", content.original().manufacturer().value());
            CheckMeta(content.original().manufacturer().meta(), seconds);

            EXPECT_EQ(true, content.original_terms().seller_warranty().has_warranty());
            CheckMeta(content.original_terms().seller_warranty().meta(), seconds);
            EXPECT_FALSE(content.original_terms().has_sales_notes());
            EXPECT_FALSE(content.original_terms().has_transport_unit_size());

            EXPECT_EQ("url", basicOffer.pictures().partner().original().source(0).url());
            CheckMeta(basicOffer.pictures().partner().original().meta(), seconds);

            EXPECT_TRUE(basicOffer.status().incomplete_wizard().has_flag());
            EXPECT_EQ(false, basicOffer.status().incomplete_wizard().flag());
            CheckMeta(basicOffer.status().incomplete_wizard().meta(), seconds);
            CheckFieldPlacementVersion(feedParsingTask, basicOffer, seconds);
        }
        {
            EXPECT_EQ(1, unitedOffer.service().size());
            auto serviceOffer = (*unitedOffer.mutable_service())[shopId];

            EXPECT_EQ(businessId, serviceOffer.identifiers().business_id());
            EXPECT_EQ(shopId, serviceOffer.identifiers().shop_id());
            EXPECT_EQ(warehouseId, serviceOffer.identifiers().warehouse_id());
            EXPECT_EQ(feedId, serviceOffer.identifiers().feed_id());
            EXPECT_EQ("offer1", serviceOffer.identifiers().offer_id());

            EXPECT_TRUE(MarketColor::BLUE == serviceOffer.meta().rgb());
            EXPECT_EQ(seconds, serviceOffer.meta().ts_created().seconds());

            EXPECT_FALSE(serviceOffer.content().binding().partner().has_market_sku_id());

            auto& content = serviceOffer.content().partner();
            EXPECT_FALSE(content.original().has_description());
            EXPECT_FALSE(content.original().has_manufacturer());

            EXPECT_EQ("sales notes", content.original_terms().sales_notes().value());
            CheckMeta(content.original_terms().sales_notes().meta(), seconds);
            EXPECT_EQ(50, content.original_terms().transport_unit_size().value());
            CheckMeta(content.original_terms().transport_unit_size().meta(), seconds);
            EXPECT_FALSE(content.original_terms().has_seller_warranty());

            EXPECT_EQ(100, serviceOffer.price().basic().binary_price().price());
            CheckMeta(serviceOffer.price().basic().meta(), seconds);

            EXPECT_EQ(true, serviceOffer.delivery().partner().original().delivery().flag());
            CheckMeta(serviceOffer.delivery().partner().original().delivery().meta(), seconds);
            EXPECT_EQ(true, serviceOffer.delivery().partner().original().available().flag());
            CheckMeta(serviceOffer.delivery().partner().original().available().meta(), seconds);

            EXPECT_EQ(99, serviceOffer.bids().bid().value());
            CheckMeta(serviceOffer.bids().bid().meta(), seconds);

            EXPECT_EQ(1, serviceOffer.stock_info().partner_stocks().count());
            CheckMeta(serviceOffer.stock_info().partner_stocks().meta(), seconds);

            if (taskType == Market::DataCamp::API::FEED_CLASS_SELECTIVE_BASIC_PATCH_UPDATE_SERVICE_FULL_COMPLETE) {
                EXPECT_TRUE(content.original().has_url());
                CheckMeta(content.original().url().meta(), seconds);
            } else {
                EXPECT_FALSE(content.original().has_url());
            }
            CheckFieldPlacementVersion(feedParsingTask, serviceOffer, seconds);
        }
    }
}

TEST(LogbrokerWriter, ConvertToUnitedInSelectiveModeInCheckFeedMode) {
    constexpr ui64 seconds = 123;
    const auto timestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(seconds);
    TFeedInfo feedInfo = CreateFeedInfo(seconds);
    TOfferCarrier offerCarrier = CreateOfferCarrier(feedInfo, seconds);

    TFeedParsingTask feedParsingTask;
    feedParsingTask.set_business_id(businessId);

    for (auto& taskType: { Market::DataCamp::API::FEED_CLASS_UPDATE,
                          Market::DataCamp::API::FEED_CLASS_SELECTIVE_BASIC_PATCH_UPDATE_SERVICE_FULL_COMPLETE,
                          Market::DataCamp::API::FEED_CLASS_SELECTIVE_BASIC_PATCH_UPDATE_SERVICE_PATCH_UPDATE}
    ) {
        feedParsingTask.set_type(taskType);

        auto datacampOffers = ConvertToUnitedInSelectiveMode<TVector<Market::DataCamp::Offer>>(feedParsingTask, offerCarrier, feedInfo);

        for (const auto& offer: datacampOffers) {
            EXPECT_TRUE(offer.identifiers().has_feed_id());
            EXPECT_EQ("offer1", offer.identifiers().offer_id());

            EXPECT_EQ(1, offer.content().binding().partner().market_sku_id());
            CheckMeta(offer.content().binding().partner().meta(), seconds);

            const auto& content = offer.content().partner();
            EXPECT_EQ("description", content.original().description().value());
            CheckMeta(content.original().description().meta(), seconds);
            EXPECT_EQ("manufacturer", content.original().manufacturer().value());
            CheckMeta(content.original().manufacturer().meta(), seconds);

            EXPECT_EQ(true, content.original_terms().seller_warranty().has_warranty());
            CheckMeta(content.original_terms().seller_warranty().meta(), seconds);
            EXPECT_TRUE(content.original_terms().has_sales_notes());
            EXPECT_TRUE(content.original_terms().has_transport_unit_size());
            EXPECT_EQ("url", offer.pictures().partner().original().source(0).url());
            CheckMeta(offer.pictures().partner().original().meta(), seconds);
        }
    }
}

TEST(LogbrokerWriter, EmptyPrice) {
    TFeedId feedId = 1069;
    TFeedId fulfillmentFeedId = 1000000;
    TBusinessId businessId = 10;
    TShopId shopId = 9601;
    TWarehouseId warehouseId = 100;

    const auto timestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(123);

    TFeedInfo feedInfo;
    feedInfo.FeedId = feedId;
    feedInfo.ShopId = shopId;
    feedInfo.WarehouseId = warehouseId;
    feedInfo.MarketColor = NMarket::EMarketColor::MC_BLUE;
    feedInfo.FulfillmentFeedId = fulfillmentFeedId;
    feedInfo.Timestamp = timestamp;

    TVector<IWriter::TMsg> items;
    {
        IWriter::TMsg item(feedInfo);
        item.DataCampOffer.mutable_identifiers()->set_offer_id("offerWithoutPrice");
        items.push_back(std::move(item));
    }

    auto writer = MakeAtomicShared<TLogBrokerMultiWriterMock>();
    TFeedParsingTask feedParsingTask;

    feedParsingTask.set_business_id(businessId);

    RunWriterTest<TLogBrokerWriter>(feedInfo, items, writer, feedParsingTask, CreateConfig(1));

    EXPECT_EQ(1, writer->GetWrittenData().size());

    {
        TDatacampMessage message;
        message.ParseFromStringOrThrow(writer->GetWrittenData()[0]);

        EXPECT_TRUE(message.offers().empty());
        EXPECT_FALSE(message.united_offers().empty());

        const auto& unitedBatch = message.united_offers(0);
        EXPECT_EQ(1, unitedBatch.offer().size());

        const auto& unitedOffer = unitedBatch.offer(0);
        EXPECT_EQ(1, unitedOffer.service().size());
        const auto& serviceOffer = unitedOffer.service().at(shopId);

        EXPECT_EQ(feedId, serviceOffer.identifiers().feed_id());
        EXPECT_EQ(businessId, serviceOffer.identifiers().business_id());
        EXPECT_EQ(shopId, serviceOffer.identifiers().shop_id());
        EXPECT_EQ(warehouseId, serviceOffer.identifiers().warehouse_id());
        EXPECT_EQ("offerWithoutPrice", serviceOffer.identifiers().offer_id());

        EXPECT_FALSE(serviceOffer.price().basic().has_binary_price());
    }
}

TEST(LogbrokerWriter, EmptyStocks) {
    TFeedId feedId = 1069;
    TFeedId fulfillmentFeedId = 1000000;
    TBusinessId businessId = 10;
    TShopId shopId = 9601;
    TWarehouseId warehouseId = 100;

    google::protobuf::Timestamp timestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(567);

    TFeedInfo feedInfo;
    feedInfo.FeedId = feedId;
    feedInfo.ShopId = shopId;
    feedInfo.WarehouseId = warehouseId;
    feedInfo.MarketColor = NMarket::EMarketColor::MC_BLUE;
    feedInfo.FulfillmentFeedId = fulfillmentFeedId;
    feedInfo.Timestamp = timestamp;

    TVector<IWriter::TMsg> items;
    {
        IWriter::TMsg item(feedInfo);
        item.DataCampOffer.mutable_identifiers()->set_offer_id("offerWithoutStocks");
        items.push_back(std::move(item));
    }

    auto writer = MakeAtomicShared<TLogBrokerMultiWriterMock>();
    TFeedParsingTask feedParsingTask;

    feedParsingTask.set_type(API::FEED_CLASS_STOCK);
    feedParsingTask.set_business_id(businessId);

    RunWriterTest<TLogBrokerWriter>(feedInfo, items, writer, feedParsingTask, CreateConfig());
    EXPECT_EQ(1, writer->GetWrittenData().size());

    {
        TDatacampMessage message;
        message.ParseFromStringOrThrow(writer->GetWrittenData()[0]);

        EXPECT_TRUE(message.offers().empty());
        EXPECT_FALSE(message.united_offers().empty());

        const auto& unitedBatch = message.united_offers(0);
        EXPECT_EQ(1, unitedBatch.offer().size());
    }
}

TEST(LogbrokerWriter, EmptyDsbsStocks) {
    TFeedId feedId = 1069;
    TFeedId fulfillmentFeedId = 1000000;
    TBusinessId businessId = 10;
    TShopId shopId = 9601;

    google::protobuf::Timestamp timestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(8910);

    TFeedInfo feedInfo;
    feedInfo.FeedId = feedId;
    feedInfo.ShopId = shopId;
    feedInfo.MarketColor = NMarket::EMarketColor::MC_WHITE;
    feedInfo.FulfillmentFeedId = fulfillmentFeedId;
    feedInfo.Timestamp = timestamp;

    TVector<IWriter::TMsg> items;
    {
        IWriter::TMsg item(feedInfo);
        item.DataCampOffer.mutable_identifiers()->set_offer_id("dsbsOfferWithoutStocks");
        items.push_back(std::move(item));
    }

    auto writer = MakeAtomicShared<TLogBrokerMultiWriterMock>();
    TFeedParsingTask feedParsingTask;

    feedParsingTask.set_type(API::FEED_CLASS_STOCK);
    feedParsingTask.set_business_id(businessId);

    RunWriterTest<TLogBrokerWriter>(feedInfo, items, writer, feedParsingTask, CreateConfig());
    EXPECT_EQ(1, writer->GetWrittenData().size());

    {
        TDatacampMessage message;
        message.ParseFromStringOrThrow(writer->GetWrittenData()[0]);

        EXPECT_TRUE(message.offers().empty());
        EXPECT_FALSE(message.united_offers().empty());

        const auto& unitedBatch = message.united_offers(0);
        EXPECT_EQ(1, unitedBatch.offer().size());
    }
}

TEST(LogbrokerWriter, QuickPipelineWriterFeedClassStock) {
    TFeedId feedId = 1069;
    TFeedId fulfillmentFeedId = 1000000;
    TBusinessId businessId = 10;
    TShopId shopId = 9601;
    TWarehouseId warehouseId = 100;

    constexpr ui64 seconds = 123;
    const auto timestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(seconds);

    TFeedInfo feedInfo;
    feedInfo.FeedId = feedId;
    feedInfo.ShopId = shopId;
    feedInfo.WarehouseId = warehouseId;
    feedInfo.MarketColor = NMarket::EMarketColor::MC_BLUE;
    feedInfo.FulfillmentFeedId = fulfillmentFeedId;
    feedInfo.Timestamp = timestamp;

    TVector<IWriter::TMsg> items;
    {
        IWriter::TMsg item(feedInfo);
        item.DataCampOffer.mutable_identifiers()->set_offer_id("offer1");

        auto* price = item.DataCampOffer.mutable_price()->mutable_basic();
        price->mutable_binary_price()->set_price(100);
        SetMeta(price->mutable_meta(), timestamp);

        auto* partnerStocks = item.DataCampOffer.mutable_stock_info()->mutable_partner_stocks();
        partnerStocks->set_count(1);
        SetMeta(partnerStocks->mutable_meta(), timestamp);

        items.push_back(std::move(item));
    }

    auto writer = MakeAtomicShared<TLogBrokerMultiWriterMock>();

    TFeedParsingTask feedParsingTask;
    feedParsingTask.set_type(Market::DataCamp::API::FEED_CLASS_STOCK);
    feedParsingTask.set_business_id(businessId);

    RunWriterTest<TLogBrokerQuickPipelineWriter>(feedInfo, items, writer, feedParsingTask, 1);

    EXPECT_EQ(0, writer->GetWrittenData().size());
}

TEST(LogbrokerWriter, ConvertToUnitedInAssortmentBasicFeed) {
    constexpr ui64 seconds = 123;
    const auto timestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(seconds);
    TFeedInfo feedInfo = CreateFeedInfo(seconds, NMarket::EMarketColor::MC_WHITE);
    TOfferCarrier offerCarrier = CreateOfferCarrier(feedInfo, seconds);

    auto taskType = Market::DataCamp::API::FEED_CLASS_ASSORTMENT_BASIC_PATCH_UPDATE;

    TFeedParsingTask feedParsingTask;
    feedParsingTask.set_business_id(businessId);
    feedParsingTask.set_type(taskType);

    auto unitedOffer = ConvertToUnitedInAssortmentBasicMode<TUnitedOffer>(feedParsingTask, offerCarrier, feedInfo);

    // Check basic fields in selective mode
    const auto& basicOffer = unitedOffer.basic();
    EXPECT_EQ(businessId, basicOffer.identifiers().business_id());
    EXPECT_FALSE(basicOffer.identifiers().has_feed_id());
    EXPECT_EQ("offer1", basicOffer.identifiers().offer_id());

    EXPECT_EQ(1, basicOffer.content().binding().partner().market_sku_id());
    CheckMeta(basicOffer.content().binding().partner().meta(), seconds);

    const auto& content = basicOffer.content().partner();
    EXPECT_EQ("description", content.original().description().value());
    CheckMeta(content.original().description().meta(), seconds);
    EXPECT_EQ("manufacturer", content.original().manufacturer().value());
    CheckMeta(content.original().manufacturer().meta(), seconds);

    EXPECT_EQ(true, content.original_terms().seller_warranty().has_warranty());
    CheckMeta(content.original_terms().seller_warranty().meta(), seconds);
    EXPECT_FALSE(content.original_terms().has_sales_notes());
    EXPECT_FALSE(content.original_terms().has_transport_unit_size());

    EXPECT_EQ("url", basicOffer.pictures().partner().original().source(0).url());
    CheckMeta(basicOffer.pictures().partner().original().meta(), seconds);

    EXPECT_TRUE(basicOffer.status().incomplete_wizard().has_flag());
    EXPECT_EQ(false, basicOffer.status().incomplete_wizard().flag());
    CheckMeta(basicOffer.status().incomplete_wizard().meta(), seconds);
    CheckFieldPlacementVersion(feedParsingTask, basicOffer, seconds);

    // Check no service parts
    EXPECT_EQ(0, unitedOffer.service().size());
    EXPECT_EQ(0, unitedOffer.actual().size());
}

TEST(LogbrokerWriter, ConvertToUnitedSaleTermsFeed) {
    constexpr ui64 seconds = 123;
    const auto timestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(seconds);
    TFeedInfo feedInfo = CreateFeedInfo(seconds);
    TOfferCarrier offerCarrier = CreateOfferCarrier(feedInfo, seconds);

    TFeedParsingTask feedParsingTask;
    feedParsingTask.set_business_id(businessId);

    for (auto taskType: {Market::DataCamp::API::FEED_CLASS_SALE_TERMS_SERVICE_FULL_COMPLETE,
                         Market::DataCamp::API::FEED_CLASS_SALE_TERMS_SERVICE_PATCH_UPDATE
                        }
    ) {
        feedParsingTask.set_type(taskType);
        auto unitedOffer = ConvertToUnitedSaleTerms<TUnitedOffer>(feedParsingTask, offerCarrier, feedInfo);

        // Проверяем, что в базовой части только идентификаторы и мета
        const auto& basicOffer = unitedOffer.basic();
        EXPECT_EQ(businessId, basicOffer.identifiers().business_id());
        EXPECT_EQ("offer1", basicOffer.identifiers().offer_id());
        EXPECT_EQ(seconds, basicOffer.meta().ts_created().seconds());
        EXPECT_FALSE(basicOffer.content().has_partner());

        EXPECT_EQ(1, unitedOffer.service().size());
        auto serviceOffer = (*unitedOffer.mutable_service())[shopId];

        EXPECT_EQ(businessId, serviceOffer.identifiers().business_id());
        EXPECT_EQ(shopId, serviceOffer.identifiers().shop_id());
        EXPECT_EQ(warehouseId, serviceOffer.identifiers().warehouse_id());
        EXPECT_EQ(feedId, serviceOffer.identifiers().feed_id());
        EXPECT_EQ("offer1", serviceOffer.identifiers().offer_id());

        EXPECT_TRUE(MarketColor::BLUE == serviceOffer.meta().rgb());
        EXPECT_EQ(seconds, serviceOffer.meta().ts_created().seconds());

        EXPECT_EQ("sales notes", serviceOffer.content().partner().original_terms().sales_notes().value());
        CheckMeta(serviceOffer.content().partner().original_terms().sales_notes().meta(), seconds);

        EXPECT_EQ(100, serviceOffer.price().basic().binary_price().price());
        CheckMeta(serviceOffer.price().basic().meta(), seconds);

        EXPECT_EQ(true, serviceOffer.delivery().partner().original().delivery().flag());
        CheckMeta(serviceOffer.delivery().partner().original().delivery().meta(), seconds);
    
        EXPECT_EQ(1, serviceOffer.stock_info().partner_stocks().count());
        CheckMeta(serviceOffer.stock_info().partner_stocks().meta(), seconds);

        if (taskType == Market::DataCamp::API::FEED_CLASS_SALE_TERMS_SERVICE_FULL_COMPLETE) {
            EXPECT_TRUE(serviceOffer.content().partner().original().has_url());
            CheckMeta(serviceOffer.content().partner().original().url().meta(), seconds);
        } else {
            EXPECT_FALSE(serviceOffer.content().partner().original().has_url());
        }
        CheckFieldPlacementVersion(feedParsingTask, serviceOffer, seconds);
    }
}
