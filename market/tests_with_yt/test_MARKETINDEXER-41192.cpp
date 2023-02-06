#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/json/json_reader.h>
#include <library/cpp/json/json_writer.h>
#include <util/stream/file.h>
#include <util/folder/dirut.h>
#include <util/system/env.h>

#include <market/idx/feeds/qparser/bin/qparser/lib/qparser.h>
#include <market/idx/feeds/qparser/lib/logger.h>
#include <market/idx/feeds/qparser/bin/qparser/lib/exit_status.h>
#include <market/idx/datacamp/proto/api/GeneralizedMessage.pb.h>
#include <market/idx/datacamp/proto/api/UpdateTask.pb.h>
#include <market/library/snappy-protostream/proto_snappy_stream.h>
#include <market/proto/indexer/BlueAssortment.pb.h>

#include "yt_env.h"

using namespace Market::DataCamp;
using namespace Market::DataCamp::API;
using namespace NMarket;
using namespace NMarket::NBlue;

const TString DATA_PATH = "/market/idx/feeds/qparser/tests_with_yt/data/MARKETINDEXER-41192-test-feed/";
const TString TEMP_DIR = GetSystemTempDir() + "/";

namespace {
    void UpdateConfig(
        const TString& fullConfigInputPath,
        const TString& fullConfigOutputPath,
        std::function<void(NJson::TJsonValue& jsonTree)> const& updateFunc
    ) {
        TIFStream configReader(fullConfigInputPath);
        NJson::TJsonValue jsonTree;
        ReadJsonTree(&configReader, &jsonTree, true);
        updateFunc(jsonTree);
        TOFStream configWriter(fullConfigOutputPath);
        WriteJson(&configWriter, &jsonTree, true);
    }
}

Y_UNIT_TEST_SUITE(Test_MARKETINDEXER_41192) {
    Y_UNIT_TEST(GenTask) {
        ShopsDatParameters shopParams;
        shopParams.set_ignore_stocks(false);
        shopParams.set_is_discounts_enabled(false);
        shopParams.set_ignore_stocks(false);
        shopParams.set_ignore_stocks(false);
        shopParams.set_is_discounts_enabled(true);
        shopParams.set_enable_auto_discounts(false);
        shopParams.set_direct_shipping(true);
        shopParams.set_vat(7);
        shopParams.set_color(MarketColor::BLUE);

        CheckFeedTaskIdentifiers checkFeedTaskIds;
        checkFeedTaskIds.set_validation_id(88005553535);

        CheckFeedTaskParameters checkFeedTaskParameters;
        checkFeedTaskParameters.set_format("pbsn");
        checkFeedTaskParameters.set_dontclean(true);

        FeedParsingTask feedParsingTask;
        feedParsingTask.set_business_id(10785678);
        feedParsingTask.set_shop_id(10783476);
        feedParsingTask.mutable_timestamp()->set_seconds(1000);
        feedParsingTask.set_feed_id(0);
        feedParsingTask.set_feedurl("https://a.yandex-team.ru/api/v1/repos/arc/tree/blob/trunk/arcadia/market/idx/feeds/qparser/tests_with_yt/data/MARKETINDEXER-41192-test-feed/feed-info.json");
        feedParsingTask.mutable_shops_dat_parameters()->Swap(&shopParams);
        feedParsingTask.set_is_check_task(true);
        feedParsingTask.set_type(FeedClass::FEED_CLASS_SELECTIVE_BASIC_PATCH_UPDATE_SERVICE_FULL_COMPLETE);
        feedParsingTask.mutable_check_feed_task_identifiers()->Swap(&checkFeedTaskIds);
        feedParsingTask.mutable_check_feed_task_parameters()->Swap(&checkFeedTaskParameters);

        TFileOutput output(TEMP_DIR + "task");
        feedParsingTask.SerializeToArcadiaStream(&output);
    }

    Y_UNIT_TEST(RunQParser) {
        const bool isRealDataTest = false;
        if(!isRealDataTest) {
            TYtEnv env(isRealDataTest);
            env.Initialize();

            auto color = MarketColor::BLUE;
            auto mcColor = EMarketColor::MC_BLUE;
            const int OFFER_COUNT = 3;
            ui64 oldPrices[OFFER_COUNT] = { 1000'0000000, 10001'0000000, 5660'0000000 };
            TString offerIds[OFFER_COUNT] = { "100256648194.wz", "100256648196.wz", "100256648198.wz" };
            TFeedInfo feedInfo {
                .ShopId = 10783476,
                .BusinessId = 10785678,
                .FeedId = 0,
                .FeedType = EFeedType::CSV,
                .MarketColor = mcColor,
                .SessionId = 123456,
                .WarehouseId = 0,
                .IsDiscountsEnabled = true,
                .EnableAutoDiscounts = false,
                .Vat = 7,
                .Cpa = ECpa::REAL,
            };

            TVector<Offer> offers(OFFER_COUNT);
            for(int i = 0; i < OFFER_COUNT; i++)
                offers[i] = TYtEnv::CreateServiceOffer(feedInfo, offerIds[i], oldPrices[i], color);
            env.InsertServiceOffers(offers);
        }

        auto& stats = TParseStats::Instance();
        stats.PriceIncreaseHitThresholdCount = 0;
        stats.PriceDecreaseHitThresholdCount = 0;

        const int argc = 12;
        TString fullDataPath = ArcadiaSourceRoot() + DATA_PATH;
        TString configFileName = isRealDataTest ? "common-debugging.json" : "common.json";
        TString configFilePath = fullDataPath + configFileName;
        TString newConfigFilePath = TEMP_DIR + configFileName;
        TString currencyExchangeRatesPath = fullDataPath + "currency_rates.xml";
        UpdateConfig(configFilePath, newConfigFilePath, [currencyExchangeRatesPath](NJson::TJsonValue &jsonTree) {
            TString cluster = GetEnv("YT_PROXY");
            jsonTree.SetValueByPath("yt.vla_proxy", NJson::TJsonValue(cluster));
            jsonTree.SetValueByPath("yt.sas_proxy", NJson::TJsonValue(cluster));
            jsonTree.SetValueByPath("yt.meta_proxy", NJson::TJsonValue(cluster));
            jsonTree.SetValueByPath("data.rates", NJson::TJsonValue(currencyExchangeRatesPath));
        });

        TString sargv[argc] {
            "",
            "--feed", fullDataPath + "test-prices-changed.csv",
            "--feed-info", fullDataPath + "feed-info.json",
            "--feed-parsing-task-filepath", TEMP_DIR + "task",
            "--config", newConfigFilePath,
            "--is-check-feed-mode",
            "--output-dir", TEMP_DIR
        };
        const char* argv[argc];
        for(int i = 0; i < argc; i++)
            argv[i] = sargv[i].c_str();

        int qparserExitCode = RunQParser(argc, argv, false);
        INFO_LOG << "qparser exit code: " << qparserExitCode << Endl;
        UNIT_ASSERT_LE(qparserExitCode, (int)EExitStatus::Warnings);

        CheckResult result;
        TSnappyProtoReader protoReader(TEMP_DIR + "check-result.pbuf.sn");
        UNIT_ASSERT_EQUAL(protoReader.Load(result), true);

        UNIT_ASSERT_EQUAL(result.parse_stats().price_increase_hit_threshold_count(), 1);
        UNIT_ASSERT_EQUAL(result.parse_stats().price_decrease_hit_threshold_count(), 1);

        INFO_LOG << "result: " << result << Endl;

        int priceWarningCount = 0;
        for (const auto& msg: result.log_message()) {
            if (msg.code() == NMarket::NOfferError::OW398_PRICE_DIFFERS_TOO_MUCH.GetCode()) {
                NJson::TJsonValue details;
                UNIT_ASSERT(ReadJsonTree(msg.details(), &details, false));

                TString offerId, comment;
                UNIT_ASSERT(details["offer"].GetString(&offerId));
                UNIT_ASSERT(details["comment"].GetString(&comment));

                if (offerId == "100256648194.wz") {
                    ++priceWarningCount;
                    UNIT_ASSERT_UNEQUAL(comment.find("(decrease)"), std::string::npos);
                } else if (offerId == "100256648198.wz") {
                    ++priceWarningCount;
                    UNIT_ASSERT_UNEQUAL(comment.find("(increase)"), std::string::npos);
                } else {
                    UNIT_FAIL("Unexpected price diff warning with offer_id " + offerId);
                }
            }
        }
        UNIT_ASSERT_EQUAL(priceWarningCount, 2);
    }
}
