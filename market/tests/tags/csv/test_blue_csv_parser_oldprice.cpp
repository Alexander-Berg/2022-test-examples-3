#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/blue/csv/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket;

static const TString INPUT_CSV(R"wrap(shop-sku;price;oldprice
    csv-without-oldprice;100;
    csv-oldprice-8990;100;8990;
    csv-oldprice-8990-01;100;8990.01;
    csv-oldprice-8999-99;100;  8999,99  ;
    csv-oldprice-lalala;100;lalala;
    csv-oldprice--9001;100;-9001;
)wrap");

static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "csv-without-oldprice",
        "OldPrice": "(empty maybe)",
        "IsValid": 1,
    },
    {
        "OfferId": "csv-oldprice-8990",
        "OldPrice": "8990",
        "IsValid": 1,
    },
    {
        "OfferId": "csv-oldprice-8990-01",
        "OldPrice": "8990.01",
        "IsValid": 1,
    },
    {
        "OfferId": "csv-oldprice-8999-99",
        "OldPrice": "8999.99",
        "IsValid": 1,
    },
    {
        "OfferId": "csv-oldprice-lalala",
        "OldPrice": "(empty maybe)",
        "IsValid": 1,
    },
    {
        "OfferId": "csv-oldprice--9001",
        "OldPrice": "(empty maybe)",
        "IsValid": 1,
    },
]
)wrap");

TEST(BlueCsvParser, OldPrice) {
    const auto actual = RunFeedParserWithTrace<NBlue::TCsvFeedParser>(
        INPUT_CSV,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["IsValid"] = item->IsValid;
            if (item->IsValid) {
                result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
                result["OldPrice"] = ToString(item->RawOldPrice);
            }
            return result;
        },
        GetDefaultBlueFeedInfo(EFeedType::CSV),
        "offers-trace.log"
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
