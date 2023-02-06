#include <market/idx/feeds/qparser/tests/test_utils.h>
#include <market/idx/feeds/qparser/tests/parser_test_runner.h>

#include <market/idx/feeds/qparser/src/feed_parsers/white/csv/feed_parser.h>
#include <market/idx/feeds/qparser/src/feed_parsers/blue/csv/feed_parser.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

#include <util/folder/path.h>
#include <util/stream/file.h>

using namespace NMarket;


namespace {

const auto fname = JoinFsPaths(
    ArcadiaSourceRoot(),
    "market/idx/feeds/qparser/tests/data/available-unknown.csv"
);
auto fStream = TUnbufferedFileInput(fname);
const auto input_feed = fStream.ReadAll();

}

static const TString EXPECTED_JSON_WHITE = TString(R"wrap(
[
    {
        "OfferId": "42",
    },
]
)wrap");

static const TString EXPECTED_JSON_BLUE = TString(R"wrap(
[
    {
        "OfferId": "59",
    },
    {
        "OfferId": "42",
    },
]
)wrap");



TEST(WhiteCsvParser, AvailableUnknown) {
    const auto actual = RunFeedParserWithTrace<TCsvFeedParser>(
        input_feed,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            return result;
        },
        GetDefaultWhiteFeedInfo(EFeedType::CSV),
        "offers-trace.log"
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON_WHITE);
    ASSERT_EQ(actual, expected);
}


TEST(BlueCsvParser, AvailableUnknown) {
    const auto actual = RunFeedParserWithTrace<NBlue::TCsvFeedParser>(
        input_feed,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            return result;
        },
        GetDefaultBlueFeedInfo(EFeedType::CSV),
        "offers-trace.log"
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON_BLUE);
    ASSERT_EQ(actual, expected);
}
