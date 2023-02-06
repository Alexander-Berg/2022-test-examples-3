#include <market/idx/feeds/qparser/tests/test_utils.h>
#include <market/idx/feeds/qparser/tests/blue_yml_test_runner.h>
#include <market/idx/feeds/qparser/tests/white_yml_test_runner.h>

#include <market/idx/feeds/qparser/src/feed_parsers/white/yml/feed_parser.h>
#include <market/idx/feeds/qparser/src/feed_parsers/blue/yml/feed_parser.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

#include <util/folder/path.h>
#include <util/stream/file.h>

using namespace NMarket;


namespace {

const auto fname = JoinFsPaths(
    ArcadiaSourceRoot(),
    "market/idx/feeds/qparser/tests/data/MARKETINDEXER-36290-feed.xml"
);
auto fStream = TUnbufferedFileInput(fname);
const auto input_xml = fStream.ReadAll();

}

static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "0206298",
    },
    {
        "OfferId": "1430492",
    },
    {
        "OfferId": "1430494",
    },
    {
        "OfferId": "0000198",
    },
]
)wrap");


TEST(WhiteYmlParser, MARKETINDEXER_36290) {
    const auto actual = RunWhiteYmlFeedParser<TYmlFeedParser>(
        input_xml,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            return result;
        },
        GetDefaultWhiteFeedInfo(EFeedType::YML)
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}


TEST(BlueYmlParser, MARKETINDEXER_36290) {
    const auto actual = RunBlueYmlFeedParser<NBlue::TYmlFeedParser>(
        input_xml,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            return result;
        },
        GetDefaultBlueFeedInfo(EFeedType::YML)
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
