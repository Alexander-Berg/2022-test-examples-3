#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/white/csv/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket;

static const TString INPUT_CSV0(R"wrap(id;price;age;available;bid;cargo-types
    csv1;100;18;false;12;cis_required
    csv2;100;18;false;12;cis_required
)wrap");

static const TString INPUT_CSV1(R"wrap(id;price;age;cargo-types
    csv4;;18;cis_required
)wrap");

static const TString INPUT_CSV2(R"wrap(id;price;age;available;bid;cargo-types
    csv3;100;;wrong-available;12;cis_required
    csv6;100;18;false;wrong-bid;unknown_type
)wrap");

TEST(WhiteCsvParser, HasNoErrorOrWarning) {
const auto actual = RunFeedParserWithTrace<TCsvFeedParser>(
        INPUT_CSV0,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            ASSERT_FALSE(item->HasError);
            ASSERT_FALSE(item->HasWarning);
            return result;
        },
        GetDefaultWhiteFeedInfo(EFeedType::CSV),
        "offers-trace.log"
);
}

TEST(WhiteCsvParser, HasError) {
const auto actual = RunFeedParserWithTrace<TCsvFeedParser>(
        INPUT_CSV1,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            ASSERT_TRUE(item->HasError);
            return result;
        },
        GetDefaultWhiteFeedInfo(EFeedType::CSV),
        "offers-trace.log"
);
}

TEST(WhiteCsvParser, HasWarning) {
const auto actual = RunFeedParserWithTrace<TCsvFeedParser>(
        INPUT_CSV2,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            ASSERT_TRUE(item->HasWarning);
            return result;
        },
        GetDefaultWhiteFeedInfo(EFeedType::CSV),
        "offers-trace.log"
);
}
