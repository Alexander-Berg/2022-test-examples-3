#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/blue/csv/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket;


static const TString INPUT_CSV(R"wrap(shop-sku;price;vendorCode
    csv-with-vendor-code;7;123
    csv-without-vendor-code;77;
)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "csv-with-vendor-code",
        "Price": 7,
        "VendorCode": "123",
    },
    {
        "OfferId": "csv-without-vendor-code",
        "Price": 77,
        "VendorCode": "",
    }
]
)wrap");


TEST(BlueCsvParser, VendorCode) {
const auto actual = RunFeedParserWithTrace<NBlue::TCsvFeedParser>(
        INPUT_CSV,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            if (item->RawPrice) {
                result["Price"] = *item->RawPrice;
            }
            result["VendorCode"] = item->DataCampOffer.Getcontent().Getpartner().Getoriginal().Getvendor_code().Getvalue();
            return result;
        },
        GetDefaultBlueFeedInfo(EFeedType::CSV),
        "offers-trace.log"
);
const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
ASSERT_EQ(actual, expected);
}


