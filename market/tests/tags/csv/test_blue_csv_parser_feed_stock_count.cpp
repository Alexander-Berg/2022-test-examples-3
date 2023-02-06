#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/blue/csv/feed_parser.h>

#include <market/idx/partners/lib/partners_program_types.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket;


TEST(BlueCsvParser, FeedStockCountCheckCorrectFieldParsing) {
    static const TString INPUT_CSV(R"wrap(shop-sku;count;price
        csv-without-feed-stock-count;;10
        csv-feed-stock-count-100;100;10
        csv-feed-stock-count-0;0;10
        csv-feed-stock-count-100.1;100.1;10
        csv-feed-stock-count-lalala;lalala;10
        csv-feed-stock-count--100;-100;10
    )wrap");


    static const TString EXPECTED_JSON = TString(R"wrap(
    [
        {
            "OfferId": "csv-without-feed-stock-count",
            "IsValid": 1,
            "FeedStockCount": "0"
        },
        {
            "OfferId": "csv-feed-stock-count-100",
            "IsValid": 1,
            "FeedStockCount": "100",
        },
        {
            "OfferId": "csv-feed-stock-count-0",
            "IsValid": 1,
            "FeedStockCount": "0",
        },
        {
            "OfferId": "csv-feed-stock-count-100.1",
            "IsValid": 1,
            "FeedStockCount": "0"
        },
        {
            "OfferId": "csv-feed-stock-count-lalala",
            "IsValid": 1,
            "FeedStockCount": "0"
        },
        {
            "OfferId": "csv-feed-stock-count--100",
            "IsValid": 1,
            "FeedStockCount": "0"
        },
    ]
    )wrap");

    auto feedInfo = GetDefaultBlueFeedInfo(EFeedType::CSV);
    const auto actual = RunFeedParserWithTrace<NBlue::TCsvFeedParser>(
            INPUT_CSV,
            [](const TQueueItem &item) {
                NSc::TValue result;
                result["IsValid"] = item->IsValid;
                if (item->IsValid) {
                    result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
                    result["FeedStockCount"] = ToString(item->DataCampOffer.stock_info().partner_stocks().count());
                }
                return result;
            },
            feedInfo,
            "offers-trace.log"
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}

TEST(BlueCsvParser, TestParsingStockFeed) {
    /// Провряем, что для стоковых фидов будут обрабатываться тольк поля shop-sku и count
    static const TString INPUT_CSV(R"wrap(shop-sku;count;price
        csv-stock-feed-without-feed-stock-count;;10
        csv-stock-feed-feed-stock-count-100;100;10
        csv-stock-feed-feed-stock-count-0;0;10
    )wrap");


    static const TString EXPECTED_JSON = TString(R"wrap(
    [
        {
            "OfferId": "csv-stock-feed-without-feed-stock-count",
            "IsValid": 1,
            "FeedStockCount": "0",
            "Price": "(empty maybe)"
        },
        {
            "OfferId": "csv-stock-feed-feed-stock-count-100",
            "IsValid": 1,
            "FeedStockCount": "100",
            "Price": "(empty maybe)"
        },
        {
            "OfferId": "csv-stock-feed-feed-stock-count-0",
            "IsValid": 1,
            "FeedStockCount": "0",
            "Price": "(empty maybe)"
        },
    ]
    )wrap");

    auto feedInfo = GetDefaultBlueFeedInfo(EFeedType::CSV);
    feedInfo.PushFeedClass = Market::DataCamp::API::FEED_CLASS_STOCK;
    const auto actual = RunFeedParserWithTrace<NBlue::TCsvStockFeedParser>(
            INPUT_CSV,
            [](const TQueueItem &item) {
                NSc::TValue result;
                result["IsValid"] = item->IsValid;
                if (item->IsValid) {
                    result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
                    result["FeedStockCount"] = ToString(item->DataCampOffer.stock_info().partner_stocks().count());
                    result["Price"] = ToString(item->RawPrice);
                }
                return result;
            },
            feedInfo,
            "offers-trace.log"
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
