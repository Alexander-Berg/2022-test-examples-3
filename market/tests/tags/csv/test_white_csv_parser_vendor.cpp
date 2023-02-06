#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/white/csv/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket;


static const TString INPUT_CSV(R"wrap(id;price;vendor
    csv-with-vendor;1;Vasya Pupkin
    csv-without-vendor;2;
    csv-vendor-with-many-lines;2;"  Нозерн
ЛТД "
)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "csv-with-vendor",
        "Price": 1,
        "Vendor": "Vasya Pupkin",
        "HasWarning": 0,
    },
    {
        "OfferId": "csv-without-vendor",
        "Price": 2,
        "Vendor": "",
        "HasWarning": 0,
    },
    {
        "OfferId": "csv-vendor-with-many-lines",
        "Price": 2,
        "Vendor": "Нозерн ЛТД",
        "HasWarning": 1,
    },
]
)wrap");


TEST(WhiteCsvParser, Vendor) {
    const auto actual = RunFeedParserWithTrace<TCsvFeedParser>(
        INPUT_CSV,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            if (item->RawPrice) {
                result["Price"] = *item->RawPrice;
            }
            result["Vendor"] = item->DataCampOffer.content().partner().original().vendor().value();
            ASSERT_FALSE(item->HasError);
            result["HasWarning"] = item->HasWarning;
            return result;
        },
        GetDefaultWhiteFeedInfo(EFeedType::CSV),
        "offers-trace.log"
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
