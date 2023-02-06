#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/white/csv/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket;


static const TString INPUT_CSV(R"wrap(id;price;url
    white-csv-with-valid-url;1;http://www.test.com/test1
    white-csv-with-invalid-url;2;httttttp://invalid
    white-csv-no-url;3;
)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {"OfferId":"white-csv-with-valid-url","Url":"http://www.test.com/test1"},
    {"OfferId":"white-csv-with-invalid-url","Url":"httttttp://invalid"},
    {"OfferId":"white-csv-no-url","Url":""}
]
)wrap");


TEST(WhiteCsvParser, Url) {
    const auto actual = RunFeedParserWithTrace<TCsvFeedParser>(
        INPUT_CSV,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            if (item->GetOriginalSpecification().has_url()) {
                result["Url"] = item->GetOriginalSpecification().url().value();
            }
            return result;
        },
        GetDefaultWhiteFeedInfo(EFeedType::CSV),
        "offers-trace.log"
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
