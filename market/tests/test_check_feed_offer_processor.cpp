#include "processor_test_runner.h"

#include <market/idx/feeds/qparser/lib/processors/check_feed_offer_processor.h>

#include <market/idx/feeds/lib/feed_parsing_task_utils.h>

#include <market/library/currency_exchange/proto_price_expression.h>
#include <market/library/process_log/checker_process_log.h>
#include <market/library/process_log/process_log.h>

#include <google/protobuf/util/time_util.h>
#include <google/protobuf/util/json_util.h>

#include <library/cpp/testing/unittest/gtest.h>

#include <util/folder/tempdir.h>
#include <util/system/fs.h>

using namespace NMarket;

using TTestCheckFeedOfferBlueProcessor = TestProcessor<TCheckFeedOfferBlueProcessor>;
using TTestCheckFeedOfferWhiteProcessor = TestProcessor<TCheckFeedOfferWhiteProcessor>;

template<typename TTestingProcessor>
void RunCheckFeedOfferProcessor(
    TFeedInfo& feedInfo,
    const TFeedShopInfo& feedShopInfo,
    TVector<IFeedParser::TMsgPtr>& offers,
    const TMaybe<TFeedParsingTask>& feedParsingTask,
    TString outputDir = ""
) {
    TString failsDumpFile = JoinFsPaths(outputDir, "check_feed_log_failures.log");
    NMarket::NProcessLog::TGlobalProcessLogRAII<NMarket::NProcessLog::TCheckerProcessLog> checkProcessLog(
        true,
        NMarket::NProcessLog::TProcessLogOptions()
            .CrashOnFailure(false)
            .ErrorDumpFile(failsDumpFile)
            .OutputDir(outputDir)
    );

    TTestingProcessor processor(feedParsingTask);
    for (const auto& offer : offers) {
        processor.Process(feedInfo, feedShopInfo, offer);
    }
}

template<typename TTestingProcessor>
void RunBasicCheckFeedProcess(
    TFeedInfo& feedInfo,
    const TFeedShopInfo& feedShopInfo,
    TVector<IFeedParser::TMsgPtr>& offers,
    NMarket::NBlue::CheckResult& checkResult
) {
    ui64 seconds = 123;
    TFeedParsingTask feedParsingTask;
    feedParsingTask.mutable_timestamp()->CopyFrom(google::protobuf::util::TimeUtil::SecondsToTimestamp(seconds));

    RunCheckFeedOfferProcessor<TTestingProcessor>(
        feedInfo,
        feedShopInfo,
        offers,
        feedParsingTask
    );

    const auto& processLog = NMarket::NProcessLog::TCheckerProcessLog::Instance();

    TString outputFile = processLog.GetOutputFileName();
    ASSERT_TRUE(NFs::Exists(outputFile));

    NMarket::TSnappyProtoReader reader(outputFile, processLog.GetOutputMagic());
    reader.Load(checkResult);
    ASSERT_TRUE(checkResult.has_input_feed());
    ASSERT_EQ(checkResult.input_feed().offer().size(), offers.size());

    auto expectedDatacampOffersSize = feedInfo.FeedType == EFeedType::YML ? 0 : offers.size();
    ASSERT_EQ(checkResult.input_feed().datacamp_offer().size(), expectedDatacampOffersSize);
}

TEST(TTestCheckFeedOfferBlueProcessor, CheckFeedOfferBasic) {
    for (const auto& feedType : {EFeedType::YML, EFeedType::CSV, EFeedType::XLS}) {
        TFeedInfo feedInfo{
            .ShopId = 1231,
            .FeedType = feedType,
            .CheckFeedMode = true,
        };
        TFeedShopInfo feedShopInfo{};

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
        processed1->DataCampOffer.mutable_content()->mutable_partner()->mutable_original_terms()->mutable_seller_warranty()->set_comment_warranty("Test Warranty Comment");
        processed1->DataCampOffer.mutable_content()->mutable_partner()->mutable_original_terms()->mutable_sales_notes()->set_value("Test Sales Notes");
        Market::NCurrency::FillPriceExpression(&*price1, 1050, "RUR");

        // disabled offer
        IWriter::TMsgPtr processed2 = MakeAtomicShared<IWriter::TMsg>(feedInfo);
        processed2->DataCampOffer.mutable_identifiers()->set_offer_id("CsvCheckFeed2");
        processed2->IsDisabledByPartner = true;
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

        NMarket::NBlue::CheckResult checkResult;
        RunBasicCheckFeedProcess<NMarket::TCheckFeedOfferBlueProcessor>(feedInfo, feedShopInfo, offers, checkResult);

        if (feedInfo.FeedType != EFeedType::YML) {
            const auto datacampOffer = checkResult.input_feed().datacamp_offer()[0];

            const auto actual_name = datacampOffer.content().partner().original().name().value();
            ASSERT_EQ(actual_name, "odin");

            const auto actual_warranty_comment = datacampOffer.content().partner().original_terms().seller_warranty().comment_warranty();
            ASSERT_EQ(actual_warranty_comment, "Test Warranty Comment");

            const auto actual_sales_notes = datacampOffer.content().partner().original_terms().sales_notes().value();
            ASSERT_EQ(actual_sales_notes, "Test Sales Notes");
        }

        const auto offerCsvCheckFeed1 = checkResult.input_feed().offer()[0];
        ASSERT_TRUE(offerCsvCheckFeed1.has_shop_sku());
        ASSERT_EQ(offerCsvCheckFeed1.shop_sku(), "CsvCheckFeed1");
        ASSERT_TRUE(offerCsvCheckFeed1.has_title());
        ASSERT_EQ(offerCsvCheckFeed1.title(), "odin");
        ASSERT_TRUE(offerCsvCheckFeed1.has_price());
        ASSERT_EQ(offerCsvCheckFeed1.price(), "RUR 1050");
        ASSERT_TRUE(offerCsvCheckFeed1.has_vat());
        ASSERT_EQ(offerCsvCheckFeed1.vat(), "VAT_10");
        ASSERT_TRUE(offerCsvCheckFeed1.has_currency());
        ASSERT_EQ(offerCsvCheckFeed1.currency(), "RUR");
        ASSERT_TRUE(offerCsvCheckFeed1.has_disabled());
        ASSERT_EQ(offerCsvCheckFeed1.disabled(), false);
        ASSERT_TRUE(offerCsvCheckFeed1.has_position());
        ASSERT_EQ(offerCsvCheckFeed1.position(), "2:0");
        ASSERT_TRUE(offerCsvCheckFeed1.has_url());
        ASSERT_EQ(offerCsvCheckFeed1.url(), "http://my_test_url.ru");
        ASSERT_TRUE(offerCsvCheckFeed1.has_market_sku());
        ASSERT_EQ(offerCsvCheckFeed1.market_sku(), "12345");

        const auto offerCsvCheckFeed2 = checkResult.input_feed().offer()[1];
        ASSERT_TRUE(offerCsvCheckFeed2.has_shop_sku());
        ASSERT_EQ(offerCsvCheckFeed2.shop_sku(), "CsvCheckFeed2");
        ASSERT_TRUE(offerCsvCheckFeed2.has_disabled());
        ASSERT_EQ(offerCsvCheckFeed2.disabled(), true);
        ASSERT_EQ(offerCsvCheckFeed2.stock_count(), 10);

        const auto offerCsvCheckFeed3 = checkResult.input_feed().offer()[2];
        ASSERT_TRUE(offerCsvCheckFeed3.has_shop_sku());
        ASSERT_EQ(offerCsvCheckFeed3.shop_sku(), "CsvCheckFeed3");
        ASSERT_TRUE(offerCsvCheckFeed3.has_currency());
        ASSERT_EQ(offerCsvCheckFeed3.currency(), "USD");
    }
}

TEST(TTestCheckFeedOfferWhiteProcessor, CheckFeedOfferBasic) {
    for (const auto& feedType : {EFeedType::YML, EFeedType::CSV, EFeedType::XLS}) {
        TFeedInfo feedInfo{
            .ShopId = 1231,
            .FeedType = feedType,
            .CheckFeedMode = true,
        };
        TFeedShopInfo feedShopInfo{};

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

        NMarket::NBlue::CheckResult checkResult;
        RunBasicCheckFeedProcess<NMarket::TCheckFeedOfferWhiteProcessor>(feedInfo, feedShopInfo, offers, checkResult);

        const auto offerCsvCheckFeed1 = checkResult.input_feed().offer()[0];
        // ASSERT_TRUE(offerCsvCheckFeed1.has_shop_sku());
        // ASSERT_EQ(offerCsvCheckFeed1.shop_sku(), "CsvCheckFeed1");
        ASSERT_TRUE(offerCsvCheckFeed1.has_title());
        ASSERT_EQ(offerCsvCheckFeed1.title(), "odin");
        ASSERT_TRUE(offerCsvCheckFeed1.has_price());
        ASSERT_EQ(offerCsvCheckFeed1.price(), "RUR 1050");
        ASSERT_TRUE(offerCsvCheckFeed1.has_vat());
        ASSERT_EQ(offerCsvCheckFeed1.vat(), "VAT_10");
        ASSERT_TRUE(offerCsvCheckFeed1.has_currency());
        ASSERT_EQ(offerCsvCheckFeed1.currency(), "RUR");
        ASSERT_TRUE(offerCsvCheckFeed1.has_disabled());
        ASSERT_EQ(offerCsvCheckFeed1.disabled(), false);
        ASSERT_TRUE(offerCsvCheckFeed1.has_position());
        ASSERT_EQ(offerCsvCheckFeed1.position(), "2:0");
        ASSERT_TRUE(offerCsvCheckFeed1.has_url());
        ASSERT_EQ(offerCsvCheckFeed1.url(), "http://my_test_url.ru");
        ASSERT_TRUE(offerCsvCheckFeed1.has_market_sku());
        ASSERT_EQ(offerCsvCheckFeed1.market_sku(), "12345");

        const auto offerCsvCheckFeed2 = checkResult.input_feed().offer()[1];
        // ASSERT_TRUE(offerCsvCheckFeed2.has_shop_sku());
        // ASSERT_EQ(offerCsvCheckFeed2.shop_sku(), "CsvCheckFeed2");
        ASSERT_TRUE(offerCsvCheckFeed2.has_disabled());
        ASSERT_EQ(offerCsvCheckFeed2.disabled(), false);
        ASSERT_EQ(offerCsvCheckFeed2.stock_count(), 10);

        const auto offerCsvCheckFeed3 = checkResult.input_feed().offer()[2];
        // ASSERT_TRUE(offerCsvCheckFeed3.has_shop_sku());
        // ASSERT_EQ(offerCsvCheckFeed3.shop_sku(), "CsvCheckFeed3");
        ASSERT_TRUE(offerCsvCheckFeed3.has_currency());
        ASSERT_EQ(offerCsvCheckFeed3.currency(), "USD");

        TString output;
        google::protobuf::util::JsonPrintOptions opt;
        opt.add_whitespace = true;
        opt.preserve_proto_field_names = true;
        google::protobuf::util::MessageToJsonString(checkResult, &output, opt);
        Cerr << "result for feed with type " << feedType << Endl;
        Cerr << output << Endl;
    }
}
