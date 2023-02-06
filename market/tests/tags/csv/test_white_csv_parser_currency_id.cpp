#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/white/csv/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket;

static const TString INPUT_CSV(R"wrap(id;price;currencyId
    csv-without-currencyId;100;
    csv-currencyId-USD;100;USD
    csv-currencyId-LOL;100;LOL
)wrap");

static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "csv-without-currencyId",
        "Currency": "RUR",
        "Disabled": 0,
    },
    {
        "OfferId": "csv-currencyId-USD",
        "Currency": "USD",
        "Disabled": 0,
    },
    {
        "OfferId": "csv-currencyId-LOL",
        "Currency": "LOL",
        "Disabled": 1,
    },
]
)wrap");

TEST(WhiteCsvParser, CurrencyId) {
    /**
     * Скрываем офер с неизвестной валютой
     */
    const auto actual = RunFeedParserWithTrace<TCsvFeedParser>(
        INPUT_CSV,
        [](const TQueueItem& item) {
            NSc::TValue result;
            bool disabled = false;
            for (const auto& flag : item->DataCampOffer.status().disabled()) {
                disabled |= flag.flag();
            }
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            if (item->IsValid) {
                result["Currency"] = item->Currency;
            }
            result["Disabled"] = disabled;
            ASSERT_TRUE(item->IsValid);
            return result;
        },
        GetDefaultWhiteFeedInfo(EFeedType::CSV),
        "offers-trace.log"
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
