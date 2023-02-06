#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/white/csv/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>


using namespace NMarket;

// ПРОВЕРЬ, что в документации имя CSV именно такое
static const TString INPUT_CSV(R"wrap(id;price;delivery
    csv-with-delivery-t;1;true
    csv-with-delivery-f;1;false
    csv-with-delivery-t-ws;1;  true
    csv-without-delivery;1;
    csv-with-wrong-delivery;7;wrong-delivery
)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "csv-with-delivery-t",
        "Delivery": 1,
    },
    {
        "OfferId": "csv-with-delivery-f",
        "Delivery": 0,
    },
    {
        "OfferId": "csv-with-delivery-t-ws",
        "Delivery": 1,
    },
    {
        "OfferId": "csv-without-delivery",
        "Delivery": 0,
    },
    {
        "OfferId": "csv-with-wrong-delivery",
        "Delivery": 0,
    },

]
)wrap");


TEST(WhiteCsvParser, Delivery) {
    const auto actual = RunFeedParserWithTrace<TCsvFeedParser>(
        INPUT_CSV,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            if (item->GetOriginalPartnerDelivery().has_delivery()) {
                result["Delivery"] = item->GetOriginalPartnerDelivery().delivery().flag();
            }
            return result;
        },
        GetDefaultWhiteFeedInfo(EFeedType::CSV),
        "offers-trace.log"
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
