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
#include <market/idx/datacamp/lib/drop_filter/drop_filter/drop_filter.h>
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

Y_UNIT_TEST_SUITE(Test_Drop_Filter) {
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
        feedParsingTask.set_is_synthetic(true);

        TFsPath deliveryDir(TEMP_DIR);
        if (!deliveryDir.Exists()) {
            deliveryDir.MkDir();
        }
        TFileOutput output(TEMP_DIR + "task");
        feedParsingTask.SerializeToArcadiaStream(&output);
    }

    Y_UNIT_TEST(YFilterSynth) {
        const bool isRealDataTest = false;
        TVector<Offer> offers(1);
        TYtEnv env(isRealDataTest);
        if(!isRealDataTest) {
            env.Initialize();
            auto color = MarketColor::BLUE;
            auto mcColor = EMarketColor::MC_BLUE;
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
                .IsSynthetic = true,
            };

            offers[0] = TYtEnv::CreateServiceOffer(feedInfo, "100256648194.wz", 1000'0000000, color);
            env.InsertServiceOffers(offers);
        }

        auto& stats = TParseStats::Instance();
        stats.PriceIncreaseHitThresholdCount = 0;
        stats.PriceDecreaseHitThresholdCount = 0;

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
            jsonTree.SetValueByPath("ydb.database_end_point", NJson::TJsonValue(GetEnv("YDB_ENDPOINT")));
            jsonTree.SetValueByPath("ydb.database_path", NJson::TJsonValue(GetEnv("YDB_DATABASE")));
            jsonTree.SetValueByPath("ydb.token_path", NJson::TJsonValue(""));
            jsonTree.SetValueByPath("ydb.drop_filter_coordination_node_path", NJson::TJsonValue("drop_filter"));
            jsonTree.SetValueByPath("ydb.drop_filter_publishing_semaphore_name", NJson::TJsonValue("publishSem"));
            jsonTree.SetValueByPath("ydb.drop_filter_blocking_semaphore_name", NJson::TJsonValue("blockSem"));
        });

        NMarket::NDatacamp::DropFilter dropFilter(GetEnv("YDB_ENDPOINT"), GetEnv("YDB_DATABASE"), 
                                                  "", "drop_filter", "publishSem", "blockSem");
        Market::DataCamp::DropFilter newDropFilter;
        newDropFilter.set_drop_synthetic(true);
        dropFilter.SetValue(newDropFilter);

        TVector<TString> sargv {
            "",
            "--feed", fullDataPath + "test-prices-changed.csv",
            "--feed-info", fullDataPath + "feed-info.json",
            "--feed-parsing-task-filepath", TEMP_DIR + "task",
            "--config", newConfigFilePath,
            "--is-check-feed-mode",
            "--output-dir", TEMP_DIR
        };
        const char* argv[sargv.size()];
        for(size_t i = 0; i < sargv.size(); i++)
            argv[i] = sargv[i].c_str();

        int qparserExitCode = RunQParser(sargv.size(), argv, false);
        INFO_LOG << "qparser exit code: " << qparserExitCode << Endl;
        UNIT_ASSERT_EQUAL(qparserExitCode, (int)EExitStatus::SyntheticTask);
    }
}
