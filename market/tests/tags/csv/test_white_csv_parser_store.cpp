#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/white/csv/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>


using namespace NMarket;

// ПРОВЕРЬ, что в документации имя CSV именно такое
static const TString INPUT_CSV(R"wrap(id;price;store
    csv-with-store-t;1;true
    csv-with-store-f;1;false
    csv-with-store-t-ws;1;  true
    csv-without-store;1;
    csv-with-wrong-store;7;wrong-store
)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "csv-with-store-t",
        "Store": 1,
    },
    {
        "OfferId": "csv-with-store-f",
        "Store": 0,
    },
    {
        "OfferId": "csv-with-store-t-ws",
        "Store": 1,
    },
    {
        "OfferId": "csv-without-store",
        "Store": 0,
    },
    {
        "OfferId": "csv-with-wrong-store",
        "Store": 0,
    },
]
)wrap");


TEST(WhiteCsvParser, Store) {
    const auto actual = RunFeedParserWithTrace<TCsvFeedParser>(
        INPUT_CSV,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            if (item->GetOriginalPartnerDelivery().has_store()) {
                result["Store"] = item->GetOriginalPartnerDelivery().store().flag();
            }
            return result;
        },
        GetDefaultWhiteFeedInfo(EFeedType::CSV),
        "offers-trace.log"
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
