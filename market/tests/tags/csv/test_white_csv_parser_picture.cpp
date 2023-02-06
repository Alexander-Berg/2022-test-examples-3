#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/white/csv/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>


using namespace NMarket;

static const TString INPUT_CSV(R"wrap(id;price;picture
    csv-with-picture;7;http://test.picture.yandex.ru/100
    csv-without-picture;77;
    csv-with-many-picture;7;    ,  http://test.picture.yandex.ru/1 , http://test.picture.yandex.ru/2,255,255,255,https://test.picture.yandex.ru/3
    csv-with-wrong-picture;7;file://wrong-picture
)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "csv-with-picture",
        "Picture": "{ meta { source: PUSH_PARTNER_FEED timestamp { seconds: 1000 } applier: QPARSER } source { url: \"test.picture.yandex.ru/100\" source: DIRECT_LINK } }",
    },
    {
        "OfferId": "csv-without-picture",
        "Picture": "{ meta { source: PUSH_PARTNER_FEED timestamp { seconds: 1000 } applier: QPARSER } }",
    },
    {
        "OfferId": "csv-with-many-picture",
        "Picture": "{ meta { source: PUSH_PARTNER_FEED timestamp { seconds: 1000 } applier: QPARSER } source { url: \"test.picture.yandex.ru/1\" source: DIRECT_LINK } source { url: \"test.picture.yandex.ru/2,255,255,255\" source: DIRECT_LINK } source { url: \"https://test.picture.yandex.ru/3\" source: DIRECT_LINK } }",
    },
    {
        "OfferId": "csv-with-wrong-picture",
        "Picture": "{ meta { source: PUSH_PARTNER_FEED timestamp { seconds: 1000 } applier: QPARSER } }",
    }
]
)wrap");


TEST(WhiteCsvParser, Picture) {
const auto actual = RunFeedParserWithTrace<TCsvFeedParser>(
        INPUT_CSV,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            result["Picture"] = ToString(item->DataCampOffer.pictures().partner().original());
            return result;
        },
        GetDefaultWhiteFeedInfo(EFeedType::CSV),
        "offers-trace.log"
);
const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
ASSERT_EQ(actual, expected);
}
