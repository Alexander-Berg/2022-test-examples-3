#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/blue/csv/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket;


static const TString INPUT_CSV(R"wrap(shop-sku;price;enable_auto_discounts
    csv-with-enable-auto-discounts-flag;1;false
    csv-without-enable-auto-discounts-flag;2;
    csv-with-invalid-enable-auto-discounts-flag;3;-1
    csv-with-enable-auto-discounts-flag-yes;4;yes
)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "csv-with-enable-auto-discounts-flag",
        "Price": 1,
        "EnableAutoDiscounts": "0",
    },
    {
        "OfferId": "csv-without-enable-auto-discounts-flag",
        "Price": 2,
        "EnableAutoDiscounts": "1",
    },
    {
        "OfferId": "csv-with-invalid-enable-auto-discounts-flag",
        "Price": 3,
        "EnableAutoDiscounts": "1",
    },
    {
        "OfferId": "csv-with-enable-auto-discounts-flag-yes",
        "Price": 4,
        "EnableAutoDiscounts": "1",
    },
]
)wrap");


TEST(BlueCsvParser, EnableAutoDiscounts) {
    auto feedInfo = GetDefaultBlueFeedInfo(EFeedType::CSV);
    feedInfo.EnableAutoDiscounts = true;
    const auto actual = RunFeedParserWithTrace<NBlue::TCsvFeedParser>(
        INPUT_CSV,
        [&feedInfo](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            if (item->RawPrice) {
                result["Price"] = *item->RawPrice;
            }
            const auto enableAutoDiscounts =
                    item->DataCampOffer.price().enable_auto_discounts().has_flag()
                        ? item->DataCampOffer.price().enable_auto_discounts().flag()
                        : feedInfo.EnableAutoDiscounts;
            result["EnableAutoDiscounts"] = ToString(enableAutoDiscounts);
            return result;
        },
        feedInfo,
        "offers-trace.log"
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
