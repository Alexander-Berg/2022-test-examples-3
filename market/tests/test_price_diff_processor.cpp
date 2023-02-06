#include <market/idx/datacamp/controllers/lib/utils/yt_storage/schema.h>
#include <market/idx/datacamp/lib/pictures/utils.h>
#include <market/idx/datacamp/proto/offer/DataCampOffer.pb.h>
#include <market/idx/datacamp/proto/tables/offers_storage_schema.pb.h>
#include <market/idx/feeds/qparser/lib/processors/price_diff_processor.h>
#include <market/library/libyt/YtHelpers.h>
#include <ydb/public/sdk/cpp/client/ydb_coordination/coordination.h>
#include <ydb/public/sdk/cpp/client/ydb_driver/driver.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/json/json_writer.h>

#include <util/system/env.h>
#include <util/stream/output.h>

#include <google/protobuf/util/message_differencer.h>

#include "yt_env.h"

using namespace Market::DataCamp;
using namespace NMarket;
using namespace NMarket::NDataCamp;

Y_UNIT_TEST_SUITE(TestPriceDiffProcessor) {
    Y_UNIT_TEST(CheckPriceDiff) {
        auto& stats = TParseStats::Instance();
        stats.PriceIncreaseHitThresholdCount = 0;
        stats.PriceDecreaseHitThresholdCount = 0;

        auto color = MarketColor::WHITE;
        TFeedInfo feedInfo {
            .ShopId = 21,
            .BusinessId = 1,
            .WarehouseId = 0
        };
        TFeedShopInfo feedShopInfo;
        THashMap<TString, ui64> oldPrices = { { "o1", 10 }, { "o2", 20 }, { "o3", 30 }, { "o4", 0 }, { "o5", 5 } };
        THashMap<TString, ui64> newPrices = { { "o1", 11 }, { "o2", 10 }, { "o3", 60 }, { "o4", 1 }, { "o5", 0 }, { "o6", 100 } };
        THashMap<TString, bool> expResults = { { "o1", true }, { "o2", false }, { "o3", false }, { "o4", true }, { "o5", true }, { "o6", true } };
        // 10 -> 11 10% increase - OK
        // 20 -> 10 50% decrease - not OK
        // 30 -> 60 50% increase - not OK
        // 0  -> 1 - OK
        // 10 -> 0 - OK (!)
        // !has_price() -> 100 - OK

        TVector<TDatacampOffer> offers;
        offers.reserve(newPrices.size());
        for (const auto& [id, price]: oldPrices) {
            offers.push_back(TYtEnv::CreateServiceOffer(feedInfo, id, price, color));
        }
        offers.push_back(TYtEnv::CreateServiceOffer(feedInfo, "o6", newPrices["o6"], color));

        TPriceDiffProcessor proc(
            TPriceDiffProcessor::TConfig {
                .MaxPriceDiffPercentage = 50.0,
                .MaxBatchSize = 1000
            },
            feedInfo,
            feedShopInfo,
            GetEnv("YT_PROXY"),
            "",
            ""
        );

        for (const auto& datacampOffer: offers) {
            auto offer = TOfferCarrier(feedInfo)
                .WithDataCampOffer(datacampOffer);
            bool result = proc.ProcessCheck(newPrices, MakeAtomicShared<IFeedParser::TMsg>(offer));
            UNIT_ASSERT_EQUAL(result, expResults[datacampOffer.identifiers().offer_id()]);
        }

        UNIT_ASSERT_EQUAL(stats.PriceIncreaseHitThresholdCount, 1);
        UNIT_ASSERT_EQUAL(stats.PriceDecreaseHitThresholdCount, 1);
    }
}
