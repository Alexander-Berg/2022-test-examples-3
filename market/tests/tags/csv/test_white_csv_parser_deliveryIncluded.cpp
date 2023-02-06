#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/white/csv/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>


using namespace NMarket;

// ПРОВЕРЬ, что в документации имя CSV именно такое
static const TString INPUT_CSV(R"wrap(id;price;deliveryIncluded
    csv-with-deliveryIncluded-t;1;true
    csv-with-deliveryIncluded-f;1;false
    csv-with-deliveryIncluded-t-ws;1;  true
    csv-without-deliveryIncluded;1;
    csv-with-wrong-deliveryIncluded;7;wrong-deliveryIncluded
)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "csv-with-deliveryIncluded-t",
        "DeliveryIncluded": 1,
    },
    {
        "OfferId": "csv-with-deliveryIncluded-f",
        "DeliveryIncluded": 0,
    },
    {
        "OfferId": "csv-with-deliveryIncluded-t-ws",
        "DeliveryIncluded": 1,
    },
    {
        "OfferId": "csv-without-deliveryIncluded"
    },
    {
        "OfferId": "csv-with-wrong-deliveryIncluded"
    }

]
)wrap");


TEST(WhiteCsvParser, DeliveryIncluded) {
    const auto actual = RunFeedParserWithTrace<TCsvFeedParser>(
        INPUT_CSV,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            if (item->GetOfferPrice().basic().has_delivery_included()) {
                result["DeliveryIncluded"] = item->GetOfferPrice().basic().delivery_included();
            }
            return result;
        },
        GetDefaultWhiteFeedInfo(EFeedType::CSV),
        "offers-trace.log"
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
