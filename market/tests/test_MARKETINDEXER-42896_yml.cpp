#include <market/idx/feeds/qparser/tests/test_utils.h>
#include <market/idx/feeds/qparser/tests/blue_yml_test_runner.h>
#include <market/idx/feeds/qparser/tests/white_yml_test_runner.h>

#include <market/idx/feeds/qparser/src/feed_parsers/blue/yml/feed_parser.h>
#include <market/idx/feeds/qparser/src/feed_parsers/white/yml/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

#include <util/folder/path.h>
#include <util/stream/file.h>

using namespace NMarket;

namespace {

TString BoolToString(bool value) {
    return value ? "true" : "false";
}

const auto fname = JoinFsPaths(
    ArcadiaSourceRoot(),
    "market/idx/feeds/qparser/tests/data/MARKETINDEXER-42896-feed.xml"
);
auto fStream = TUnbufferedFileInput(fname);
const auto inputYml = fStream.ReadAll();

NSc::TValue QueueItemSelector(const TQueueItem& item) {
    NSc::TValue result;
    result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
    result["IsValid"] = BoolToString(item->IsValid);

    ASSERT_FALSE(item->RawPrice.Defined());
    return result;
}

}

static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "price-feed-link-no-price",
        "IsValid": "true"
    },
]
)wrap");

static const TString EXPECTED_CHECK_FEED_JSON = TString(R"wrap(
[
    {
        "OfferId": "price-feed-link-no-price",
        "IsValid": "false"
    },
]
)wrap");

TEST(BlueYmlParser, MARKETINDEXER_42896) {
    const auto [actual, _] = RunBlueYmlFeedParserWithCheckFeed<NBlue::TYmlFeedParser>(
        inputYml,
        QueueItemSelector,
        GetDefaultBlueFeedInfo(EFeedType::YML)
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}

TEST(BlueYmlParser, MARKETINDEXER_42896_CheckFeedMode) {
    auto feedInfo = GetDefaultBlueFeedInfo(EFeedType::YML);
    feedInfo.CheckFeedMode = true;
    const auto [actual, _] = RunBlueYmlFeedParserWithCheckFeed<NBlue::TYmlFeedParser>(
        inputYml,
        QueueItemSelector,
        feedInfo
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_CHECK_FEED_JSON);
    ASSERT_EQ(actual, expected);
}

TEST(WhiteYmlParser, MARKETINDEXER_42896) {
    const auto actual = RunWhiteYmlFeedParserWithCheckFeed<TYmlFeedParser>(
        inputYml,
        QueueItemSelector,
        GetDefaultWhiteFeedInfo(EFeedType::YML)
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}

TEST(WhiteYmlParser, MARKETINDEXER_42896_CheckFeedMode) {
    auto feedInfo = GetDefaultWhiteFeedInfo(EFeedType::YML);
    feedInfo.CheckFeedMode = true;
    const auto actual = RunWhiteYmlFeedParserWithCheckFeed<TYmlFeedParser>(
        inputYml,
        QueueItemSelector,
        feedInfo
    );
    ASSERT_TRUE(actual.IsNull());
}

