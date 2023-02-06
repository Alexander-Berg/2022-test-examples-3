#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/white/csv/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>


using namespace NMarket;

// ПРОВЕРЬ, что в документации имя CSV именно такое
static const TString INPUT_CSV(R"wrap(id;price;pickup
    csv-with-pickup-t;1;true
    csv-with-pickup-f;1;false
    csv-with-pickup-t-ws;1;  true
    csv-without-pickup;1;
    csv-with-wrong-pickup;7;wrong-pickup
)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "csv-with-pickup-t",
        "Pickup": 1,
    },
    {
        "OfferId": "csv-with-pickup-f",
        "Pickup": 0,
    },
    {
        "OfferId": "csv-with-pickup-t-ws",
        "Pickup": 1,
    },
    {
        "OfferId": "csv-without-pickup"
    },
    {
        "OfferId": "csv-with-wrong-pickup"
    }
]
)wrap");


TEST(WhiteCsvParser, Pickup) {
    const auto actual = RunFeedParserWithTrace<TCsvFeedParser>(
        INPUT_CSV,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            if (item->GetOriginalPartnerDelivery().pickup().has_flag()) {
                result["Pickup"] = item->GetOriginalPartnerDelivery().pickup().flag();
            }
            return result;
        },
        GetDefaultWhiteFeedInfo(EFeedType::CSV),
        "offers-trace.log"
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
