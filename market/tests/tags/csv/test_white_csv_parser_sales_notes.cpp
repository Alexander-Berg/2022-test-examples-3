#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/white/csv/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

// WARNING!! IT IS GENERATED. IT IS TEMPLATE!!!


using namespace NMarket;

// ПРОВЕРЬ, что в документации имя CSV именно такое
static const TString INPUT_CSV(R"wrap(id;price;sales_notes
    csv-with-salesnotes;7;sales_notesабвгдабвгдабвгдабвгабвг
    csv-without-salesnotes;77;;
    csv-with-wrong-salesnotes;7;sales_notesабвгдабвгдабвгдабвгдабвгдабвгдабвгдабвгдабвгдабвгдабвгдабвгдsales_notesабвгдабвгдабвгдабвгдабвгдабвгдабвгдабвгдабвгдабвгдабвгдабвгд
)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "csv-with-salesnotes",
        "SalesNotes": "sales_notesабвгдабвгдабвгдабвгабвг",
    },
    {
        "OfferId": "csv-without-salesnotes",
        "SalesNotes": "",
    },
    {
        "OfferId": "csv-with-wrong-salesnotes",
        "SalesNotes": "sales_notesабвгдабвгдабвгдабвгдабвгдабвгдабвгдабвгдабвгдабвгдабвгдабвгдsales_notesабвгдабвгдабвгдабвгдабвгдабвгдабвгдабвгдабвгдабвгдабвгдабвгд",
    }
]
)wrap");


TEST(WhiteCsvParser, SalesNotes) {
const auto actual = RunFeedParserWithTrace<TCsvFeedParser>(
        INPUT_CSV,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            auto& originalTerms = item->DataCampOffer.content().partner().original_terms();
            if(originalTerms.has_sales_notes()) {
                result["SalesNotes"] = originalTerms.sales_notes().value();
            }
            return result;
        },
        GetDefaultWhiteFeedInfo(EFeedType::CSV),
        "offers-trace.log"
);
const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
ASSERT_EQ(actual, expected);
}
