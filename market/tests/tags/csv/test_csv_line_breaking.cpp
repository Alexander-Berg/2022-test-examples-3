#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/blue/csv/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket;

static const TString INPUT_CSV(R"wrap(shop-sku;price;description
    csv-with-description-one-line;10;Маленькое описание
    csv-with-description-one-line-with-quotes;20;"Описание""с кавычками"""
    csv-with-description-many-lines;30;"Очень
длинное""
описание"
)wrap");

static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "csv-with-description-one-line",
        "Price": 10,
        "Description": "Маленькое описание",
    },
    {
        "OfferId": "csv-with-description-one-line-with-quotes",
        "Price": 20,
        "Description": "Описание\"с кавычками\"",
    },
    {
        "OfferId": "csv-with-description-many-lines",
        "Price": 30,
        "Description": "Очень\nдлинное\"\nописание",
    }
]
)wrap");

TEST(CsvParser, LineBreaking) {
    const auto actual = RunFeedParserWithTrace<NBlue::TCsvFeedParser>(
            INPUT_CSV,
            [](const TQueueItem& item) {
                NSc::TValue result;
                result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
                if (item->RawPrice) {
                    result["Price"] = *item->RawPrice;
                }
                result["Description"] = item->DataCampOffer.content().partner().original().description().value();
                return result;
            },
            GetDefaultBlueFeedInfo(EFeedType::CSV),
            "offers-trace.log"
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
