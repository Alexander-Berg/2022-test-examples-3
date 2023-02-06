#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/blue/csv/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>


using namespace NMarket;

static const TString INPUT_CSV(R"wrap(shop-sku;price;param
    csv-with-param;7;"Алмазы|27|карат;Материал корпуса|платина;NFS;"
    csv-without-param;77;
    csv-with-empty-params;7;"Цвет||;Материал корпуса|"
    csv-with-multiline-params;777;"Материал корпуса|платина;
Число деталей|100500;
Цвет|металлик;"
)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "csv-with-param",
        "Param": "{ meta { source: PUSH_PARTNER_FEED timestamp { seconds: 1000 } applier: QPARSER } param { name: \"Алмазы\" unit: \"карат\" value: \"27\" } param { name: \"Материал корпуса\" unit: \"\" value: \"платина\" } }",
    },
    {
        "OfferId": "csv-without-param",
        "Param": "{ meta { source: PUSH_PARTNER_FEED timestamp { seconds: 1000 } applier: QPARSER } }",
    },
    {
        "OfferId": "csv-with-empty-params",
        "Param": "{ meta { source: PUSH_PARTNER_FEED timestamp { seconds: 1000 } applier: QPARSER } }",
    },
    {
        "OfferId": "csv-with-multiline-params",
        "Param": "{ meta { source: PUSH_PARTNER_FEED timestamp { seconds: 1000 } applier: QPARSER } param { name: \"Материал корпуса\" unit: \"\" value: \"платина\" } param { name: \"Число деталей\" unit: \"\" value: \"100500\" } param { name: \"Цвет\" unit: \"\" value: \"металлик\" } }",
    },
]
)wrap");


TEST(BlueCsvParser, Param) {
const auto actual = RunFeedParserWithTrace<NBlue::TCsvFeedParser>(
        INPUT_CSV,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            result["Param"] = ToString(item->GetOriginalSpecification().offer_params());
            return result;
        },
        GetDefaultBlueFeedInfo(EFeedType::CSV),
        "offers-trace.log"
);
const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
ASSERT_EQ(actual, expected);
}
