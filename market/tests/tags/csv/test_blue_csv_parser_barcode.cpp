#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/blue/csv/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket;

static const TString INPUT_CSV(R"wrap(shop-sku;price;barcode
    csv-with-barcode;7;12345678901234567890,1234567890123456789x
    csv-with-empty-barcode;77;
    csv-with-wrong-barcode;7;123
)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "csv-with-barcode",
        "Barcode": "12345678901234567890, 1234567890123456789x",
    },
    {
        "OfferId": "csv-with-empty-barcode",
        "Barcode": "",
    },
    {
        "OfferId": "csv-with-wrong-barcode",
        "Barcode": "",
    }
]
)wrap");


TEST(BlueCsvParser, Barcode) {
const auto actual = RunFeedParserWithTrace<NBlue::TCsvFeedParser>(
        INPUT_CSV,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            if (item->GetOriginalSpecification().has_barcode())  {
                result["Barcode"] = ToString(item->GetOriginalSpecification().barcode().value());
            }
            return result;
        },
        GetDefaultBlueFeedInfo(EFeedType::CSV),
        "offers-trace.log"
);
const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
ASSERT_EQ(actual, expected);
}
