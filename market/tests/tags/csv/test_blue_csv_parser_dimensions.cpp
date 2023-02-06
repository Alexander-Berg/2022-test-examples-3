#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/blue/csv/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

#include <util/string/join.h>


using namespace NMarket;


static const TString INPUT_CSV(R"wrap(shop-sku;price;dimensions
    csv-valid-dimensions;1;65.55/50.7/20.0
    csv-invalid-value-dimensions;2;65.55/-50.7/20.0
    csv-invalid-dimensions;3;invalid
    csv-a-lot-of-dimensions;4;65.55/50.7/20.0/10.12
    csv-zero-dimension;5;65.55/0.0/20.0
    csv-invalid-dimensions-delimiter;6;65.55,12.0,20.0
    csv-zpt-value;100;10/10.5/10,7
)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "csv-valid-dimensions",
        "Price": 1,
        "Dimensions": "655500,507000,200000",
    },
    {
        "OfferId": "csv-invalid-value-dimensions",
        "Price": 2,
        "Dimensions": "(empty maybe)",
    },
    {
        "OfferId": "csv-invalid-dimensions",
        "Price": 3,
        "Dimensions": "(empty maybe)",
    },
    {
        "OfferId": "csv-a-lot-of-dimensions",
        "Price": 4,
        "Dimensions": "(empty maybe)",
    },
    {
        "OfferId": "csv-zero-dimension",
        "Price": 5,
        "Dimensions": "(empty maybe)",
    },
    {
        "OfferId": "csv-invalid-dimensions-delimiter",
        "Price": 6,
        "Dimensions": "(empty maybe)",
    },
    {
        "OfferId": "csv-zpt-value",
        "Price": 100,
        "Dimensions": "100000,105000,107000",
    }
]
)wrap");


TEST(BlueCsvParser, Dimensions) {
    const auto actual = RunFeedParserWithTrace<NBlue::TCsvFeedParser>(
        INPUT_CSV,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            if (item->RawPrice) {
                result["Price"] = *item->RawPrice;
            }
            if (item->DataCampOffer.content().partner().original().dimensions().Hasheight_mkm()) {
                const auto& dimensions = item->DataCampOffer.content().partner().original().dimensions();
                result["Dimensions"] = JoinSeq(
                    ",", {dimensions.length_mkm(), dimensions.width_mkm(), dimensions.height_mkm()});
            } else {
                result["Dimensions"] = ToString(Nothing());
            }
            return result;
        },
        GetDefaultBlueFeedInfo(EFeedType::CSV),
        "offers-trace.log"
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
