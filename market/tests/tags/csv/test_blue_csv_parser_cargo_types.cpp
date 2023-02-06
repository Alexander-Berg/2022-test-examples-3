#include <market/idx/feeds/qparser/tests/datacamp_utils.h>
#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/blue/csv/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket;

static const TString INPUT_CSV(R"wrap(id;price;cargo-types
    csv-with-cargo-1;7;cis_required
    csv-without-cargo;77;
)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {"OfferId":"csv-with-cargo-1","CargoTypes":"980"},
    {"OfferId":"csv-without-cargo","CargoTypes":""},
]
)wrap");


TEST(BlueCsvParser, CargoTypes) {
    const auto actual = RunFeedParserWithTrace<NBlue::TCsvFeedParser>(
        INPUT_CSV,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            if (item->GetOriginalSpecification().cargo_types().has_meta()) {
                result["CargoTypes"] = GetCargoTypesAsString(item->DataCampOffer);
            }
            return result;
        },
        GetDefaultBlueFeedInfo(EFeedType::CSV),
        "offers-trace.log"
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
