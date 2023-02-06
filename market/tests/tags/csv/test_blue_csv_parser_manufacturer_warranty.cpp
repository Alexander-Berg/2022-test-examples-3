#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/blue/csv/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket;


static const TString INPUT_CSV(R"wrap(shop-sku;price;manufacturer_warranty
    csv-with-manufacturer-warranty;1;false
    csv-without-manufacturer-warranty;2;
)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "csv-with-manufacturer-warranty",
        "Price": 1,
        "ManufacturerWarranty": "0",
    },
    {
        "OfferId": "csv-without-manufacturer-warranty",
        "Price": 2,
        "ManufacturerWarranty": "0",
    }
]
)wrap");


TEST(BlueCsvParser, ManufacturerWarranty) {
    const auto actual = RunFeedParserWithTrace<NBlue::TCsvFeedParser>(
        INPUT_CSV,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            if (item->RawPrice) {
                result["Price"] = *item->RawPrice;
            }
            result["ManufacturerWarranty"] = ToString(
                item->DataCampOffer.content().partner().original().manufacturer_warranty().flag());
            return result;
        },
        GetDefaultBlueFeedInfo(EFeedType::CSV),
        "offers-trace.log"
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
