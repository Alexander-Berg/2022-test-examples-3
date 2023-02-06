#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/blue/csv/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket;

static const TString INPUT_CSV(R"wrap(shop-sku;price;disabled
    csv-without-disabled;100;
    csv-disabled-0;100;1
    csv-disabled-1;100;true
    csv-disabled-2;100;True
    csv-disabled-3;100;On
    csv-disabled-4;100;Да
    csv-disabled-5;100;dada
)wrap");

static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "csv-without-disabled",
        "IsDisabled": 0,
        "IsValid": 1,
    },
    {
        "OfferId": "csv-disabled-0",
        "IsDisabled": 1,
        "IsValid": 1,
    },
    {
        "OfferId": "csv-disabled-1",
        "IsDisabled": 1,
        "IsValid": 1,
    },
    {
        "OfferId": "csv-disabled-2",
        "IsDisabled": 1,
        "IsValid": 1,
    },
    {
        "OfferId": "csv-disabled-3",
        "IsDisabled": 1,
        "IsValid": 1,
    },
    {
        "OfferId": "csv-disabled-4",
        "IsDisabled": 1,
        "IsValid": 1,
    },
    {
        "OfferId": "csv-disabled-5",
        "IsDisabled": 0,
        "IsValid": 1,
    },
]
)wrap");

TEST(BlueCsvParser, IsDisabled) {
    const auto actual = RunFeedParserWithTrace<NBlue::TCsvFeedParser>(
        INPUT_CSV,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            result["IsValid"] = item->IsValid;
            result["IsDisabled"] = item->IsDisabled;
            return result;
        },
        GetDefaultBlueFeedInfo(EFeedType::CSV),
        "offers-trace.log"
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
