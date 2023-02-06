#include <market/idx/feeds/qparser/tests/datacamp_utils.h>
#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/white/csv/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket;

static const TString INPUT_CSV(R"wrap(id;price;cargo-types
    csv-with-cargo-1;7;cis_required
    csv-without-cargo;77;
    csv-with-unknown-cargo;77;something
)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {"OfferId":"csv-with-cargo-1","CargoTypes":"980"},
    {"OfferId":"csv-without-cargo","CargoTypes":""},
    {"OfferId":"csv-with-unknown-cargo","CargoTypes":""},
]
)wrap");


TEST(WhiteCsvParser, CargoTypes) {
    const auto actual = RunFeedParserWithTrace<TCsvFeedParser>(
        INPUT_CSV,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            if (item->GetOriginalSpecification().cargo_types().has_meta()) {
                result["CargoTypes"] = GetCargoTypesAsString(item->DataCampOffer);
            }
            return result;
        },
        GetDefaultWhiteFeedInfo(EFeedType::CSV),
        "offers-trace.log"
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
