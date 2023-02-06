#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/white/csv/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket;

static const TString INPUT_CSV(R"wrap(id;price;purchase_price
    csv-without-purchase-price;100;
    csv-purchase-price-8990;100;8990;
    csv-purchase-price-8990-01;100;8990.01;
    csv-purchase-price-8999-99;100;  8999,99  ;
    csv-purchase-price-lalala;100;lalala;
    csv-purchase-price--9001;100;-9001;
)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "csv-without-purchase-price",
    },
    {
        "OfferId": "csv-purchase-price-8990",
        "PurchasePrice": 8990,
    },
    {
        "OfferId": "csv-purchase-price-8990-01",
        "PurchasePrice": 8990.01,
    },
    {
        "OfferId": "csv-purchase-price-8999-99",
        "PurchasePrice": 8999.99,
    },
    {
        "OfferId": "csv-purchase-price-lalala",
    },
    {
        "OfferId": "csv-purchase-price--9001",
    },
]
)wrap");


TEST(WhiteCsvParser, PurchasePrice) {
const auto actual = RunFeedParserWithTrace<TCsvFeedParser>(
        INPUT_CSV,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();

            const auto& purchase_price = item->DataCampOffer.price().purchase_price();

            if(purchase_price.Hasbinary_price()) {
                result["PurchasePrice"] = TFixedPointNumber::CreateFromRawValue(purchase_price.binary_price().price()).AsDouble();
            }

            // for CSV all meta's should be filled in
            ASSERT_TRUE(purchase_price.Hasmeta());

            ASSERT_TRUE(item->IsValid);

            return result;
        },
        GetDefaultWhiteFeedInfo(EFeedType::CSV),
        "offers-trace.log"
);
const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
ASSERT_EQ(actual, expected);
}
