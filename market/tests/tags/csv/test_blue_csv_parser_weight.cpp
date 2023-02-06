#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/blue/csv/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket;


static const TString INPUT_CSV(R"wrap(shop-sku;price;weight
    csv-with-weight;1;123.45
    csv-without-weight;2;
    csv-with-invalid-weight;3;-0.1
)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "csv-with-weight",
        "Price": 1,
        "Weight": 123450000,
    },
    {
        "OfferId": "csv-without-weight",
        "Price": 2,
        "Weight": 0,
    },
    {
        "OfferId": "csv-with-invalid-weight",
        "Price": 3,
        "Weight": 0,
    },
]
)wrap");


TEST(BlueCsvParser, Weight) {

    const auto actual = RunFeedParserWithTrace<NBlue::TCsvFeedParser>(
        INPUT_CSV,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            if (item->RawPrice) {
                result["Price"] = *item->RawPrice;
            }
            result["Weight"] = item->DataCampOffer.content().partner().original().weight().value_mg();
            return result;
        },
        GetDefaultBlueFeedInfo(EFeedType::CSV),
        "offers-trace.log"
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}

TEST(BlueCsvParser, NetWeight) {

    const auto actual = RunFeedParserWithTrace<NBlue::TCsvFeedParser>(
        SubstGlobalCopy(INPUT_CSV, "weight", "net-weight"),
        [](const TQueueItem& item) {
          NSc::TValue result;
          result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
          if (item->RawPrice) {
              result["Price"] = *item->RawPrice;
          }
          result["Weight"] = item->DataCampOffer.content().partner().original().weight_net().value_mg();
          return result;
        },
        GetDefaultBlueFeedInfo(EFeedType::CSV),
        "offers-trace.log"
    );
    const auto expected = NSc::TValue::FromJson(SubstGlobalCopy(EXPECTED_JSON, "weight", "net-weight"));
    ASSERT_EQ(actual, expected);
}
