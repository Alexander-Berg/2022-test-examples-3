#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/blue/csv/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

#include <util/string/join.h>

using namespace NMarket;

static const TString INPUT_CSV(R"wrap(shop-sku;price;delivery_weekdays
    csv-empty-delivery-weekdays;1;
    csv-valid-single-delivery-weekdays;2;1
    csv-valid-multi-delivery-weekdays;3;1,2
    csv-valid-multi-with-spaces-delivery-weekdays;4;1 , 2 , 3,4
    csv-valid-short-text-delivery-weekdays;5;пн,ср
    csv-valid-long-text-delivery-weekdays;6;вторник,четверг
    csv-valid-long-text-strange-capitalization-delivery-weekdays;7;пЯтнИцА,Четверг
    csv-valid-numeric-and-text-delivery-weekdays;8;1,воскресенье,пт
    csv-invalid-delivery-weekdays;9;invalid
    csv-partially-valid-delivery-weekdays;10;2,4,8
    csv-invalid-delimiter-delivery-weekdays;10;6 3
)wrap");

static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "csv-empty-delivery-weekdays",
        "Price": 1,
        "SupplyWeekdays": "(empty maybe)"
    },
    {
        "OfferId": "csv-valid-single-delivery-weekdays",
        "Price": 2,
        "SupplyWeekdays": "1"
    },
    {
        "OfferId": "csv-valid-multi-delivery-weekdays",
        "Price": 3,
        "SupplyWeekdays": "1,2"
    },
    {
        "OfferId": "csv-valid-multi-with-spaces-delivery-weekdays",
        "Price": 4,
        "SupplyWeekdays": "1,2,3,4"
    },
    {
        "OfferId": "csv-valid-short-text-delivery-weekdays",
        "Price": 5,
        "SupplyWeekdays": "1,3"
    },
    {
        "OfferId": "csv-valid-long-text-delivery-weekdays",
        "Price": 6,
        "SupplyWeekdays": "2,4"
    },
    {
        "OfferId": "csv-valid-long-text-strange-capitalization-delivery-weekdays",
        "Price": 7,
        "SupplyWeekdays": "5,4"
    },
    {
        "OfferId": "csv-valid-numeric-and-text-delivery-weekdays",
        "Price": 8,
        "SupplyWeekdays": "1,7,5"
    },
    {
        "OfferId": "csv-invalid-delivery-weekdays",
        "Price": 9,
        "SupplyWeekdays": "(empty maybe)"
    },
    {
        "OfferId": "csv-partially-valid-delivery-weekdays",
        "Price": 10,
        "SupplyWeekdays": "(empty maybe)"
    },
    {
        "OfferId": "csv-invalid-delimiter-delivery-weekdays",
        "Price": 10,
        "SupplyWeekdays": "(empty maybe)"
    }
]
)wrap");

TEST(BlueCsvParser, DeliveryWeekdays) {
    const auto actual = RunFeedParserWithTrace<NBlue::TCsvFeedParser>(
        INPUT_CSV,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            if (item->RawPrice) {
                result["Price"] = *item->RawPrice;
            }
            const auto& originalTerms = item->DataCampOffer.content().partner().original_terms();
            if (originalTerms.supply_weekdays().days_size() > 0) {
                result["SupplyWeekdays"] = JoinSeq(",", originalTerms.supply_weekdays().days());
            } else {
                result["SupplyWeekdays"] = ToString(Nothing());
            }
            return result;
        },
        GetDefaultBlueFeedInfo(EFeedType::CSV),
        "offers-trace.log");
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
