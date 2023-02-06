#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/blue/csv/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket;

static const TString INPUT_CSV(R"wrap(shop-sku;price;disabled;currencyId
    csv-without-currencyId;100;;
    csv-currencyId-USD;100;;USD
    csv-currencyId-LOL;100;;LOL
)wrap");

static const TString EXPECTED_JSON_FORCE_RUR = TString(R"wrap(
[
    {
        "OfferId": "csv-without-currencyId",
        "Currency": "RUR",
    },
    {
        "OfferId": "csv-currencyId-USD",
        "Currency": "RUR",
    },
    {
        "OfferId": "csv-currencyId-LOL",
        "Currency": "RUR",
    },
]
)wrap");

static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "csv-without-currencyId",
        "Currency": "RUR",
        "Disabled": 0
    },
    {
        "OfferId": "csv-currencyId-USD",
        "Currency": "USD",
        "Disabled": 1
    },
    {
        "OfferId": "csv-currencyId-LOL",
        "Currency": "LOL",
        "Disabled": 1
    },
]
)wrap");

TEST(BlueCsvParser, CurrencyIdWithForceRur) {
    /**
     * Cиним проставляем везде RUR, даже в случае невалидных значений валюты (LOL -> RUR)
     * Работает, когда включен фича-флажок ForceRurCurrencyForBlueMode (убрать в MARKETINDEXER-37256)
     */

    auto feedInfo = GetDefaultBlueFeedInfo(EFeedType::CSV);
    feedInfo.ForceRurCurrencyForBlueMode = true;

    const auto actual = RunFeedParserWithTrace<NBlue::TCsvFeedParser>(
        INPUT_CSV,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            if (item->IsValid) {
                result["Currency"] = item->Currency;
            }
            ASSERT_TRUE(item->IsValid);
            return result;
        },
        feedInfo,
        "offers-trace.log"
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON_FORCE_RUR);
    ASSERT_EQ(actual, expected);
}

TEST(BlueCsvParser, CurrencyId) {
    /**
     * Cиним разрешен только рубль, остальные офферы скрываем (текущее бизнес-требование)
     */
    auto feedInfo = GetDefaultBlueFeedInfo(EFeedType::CSV);
    feedInfo.ForceRurCurrencyForBlueMode = false;

    const auto actual = RunFeedParserWithTrace<NBlue::TCsvFeedParser>(
        INPUT_CSV,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            if (item->IsValid) {
                result["Currency"] = item->Currency;
            }
            ASSERT_TRUE(item->IsValid);

            bool disabled = false;
            for (const auto& flag : item->DataCampOffer.status().disabled()) {
                disabled |= flag.flag();
            }
            result["Disabled"] = disabled;
            return result;
        },
        feedInfo,
        "offers-trace.log"
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
