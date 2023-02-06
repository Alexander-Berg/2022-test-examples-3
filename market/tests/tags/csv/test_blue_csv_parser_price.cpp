#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/blue/csv/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket;

static const TString INPUT_CSV(R"wrap(shop-sku;price;disabled
    csv-without-price;;
    csv-with-zero-price;0;
    csv-price-8990;8990;
    csv-price-8990-01;8990.01;
    csv-price-8999-99;  8999,99  ;
    csv-price-lalala;lalala;
    csv-price--9001;-9001;
)wrap");

static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "csv-without-price",
        "RawPrice": "",
        "IsValid": 1,
    },
    {
        "OfferId": "csv-with-zero-price",
        "RawPrice": "0",
        "IsValid": 1,
    },
    {
        "OfferId": "csv-price-8990",
        "Price": 8990,
        "IsValid": 1,
        "RawPrice": "8990"
    },
    {
        "OfferId": "csv-price-8990-01",
        "Price": 8990.01,
        "IsValid": 1,
        "RawPrice": "8990.01"
    },
    {
        "OfferId": "csv-price-8999-99",
        "Price": 8999.99,
        "IsValid": 1,
        "RawPrice": "8999,99"
    },
    {
        "OfferId": "csv-price-lalala",
        "RawPrice": "lalala",
        "IsValid": 1,
    },
    {
        "OfferId": "csv-price--9001",
        "RawPrice": "-9001",
        "IsValid": 1,
    },
]
)wrap");

TEST(BlueCsvParser, Price) {
    const auto actual = RunFeedParserWithTrace<NBlue::TCsvFeedParser>(
        INPUT_CSV,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["IsValid"] = item->IsValid;
            if (item->IsValid) {
                result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
                if (item->RawPrice) {
                    result["Price"] = *item->RawPrice;
                }
            }
            result["RawPrice"] = ToString(item->RawOriginalPrice);
            return result;
        },
        GetDefaultBlueFeedInfo(EFeedType::CSV),
        "offers-trace.log"
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
