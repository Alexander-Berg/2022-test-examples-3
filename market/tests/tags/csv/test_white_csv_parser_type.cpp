#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/white/csv/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>


using namespace NMarket;

static const TString INPUT_CSV(R"wrap(id;price;type
    white-csv-default;100;
    white-csv-unknown;100;qazwsxedc
    white-csv-vendormodel;100;vendor.model
    white-csv-book;100;book
    white-csv-audiobook;100;audiobook
    white-csv-artisttitle;100;artist.title
    white-csv-general;100;general
    white-csv-medicine;100;medicine
    white-csv-alcohol;100;alco
    white-csv-ondemand;100;on.demand
)wrap");

static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "white-csv-default",
        "Type": 1
    },
    {
        "OfferId": "white-csv-unknown",
        "Type": 1
    },
    {
        "OfferId": "white-csv-vendormodel",
        "Type": 2
    },
    {
        "OfferId": "white-csv-book",
        "Type": 4
    },
    {
        "OfferId": "white-csv-audiobook",
        "Type": 5
    },
    {
        "OfferId": "white-csv-artisttitle",
        "Type": 6
    },
    {
        "OfferId": "white-csv-general",
        "Type": 1
    },
    {
        "OfferId": "white-csv-medicine",
        "Type": 3
    },
    {
        "OfferId": "white-csv-alcohol",
        "Type": 9
    },
    {
        "OfferId": "white-csv-ondemand",
        "Type": 10
    }
]
)wrap");


TEST(WhiteCsvParser, Type) {
const auto actual = RunFeedParserWithTrace<TCsvFeedParser>(
        INPUT_CSV,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            result["Type"] = item->GetOriginalSpecification().type().value();
            return result;
        },
        GetDefaultWhiteFeedInfo(EFeedType::CSV),
        "offers-trace.log"
);
const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
ASSERT_EQ(actual, expected);
}
