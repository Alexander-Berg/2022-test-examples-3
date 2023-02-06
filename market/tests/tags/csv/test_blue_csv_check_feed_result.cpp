#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/blue/csv/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket;

static const TString INPUT_CSV(R"wrap(shop-sku;price;oldprice;dimensions;weight;vat;disabled;vendor;name;count
    CsvCheckFeed1;1;1;1.1/1.1/1.1;1.1;VAT_10;false;First Vendor;odin;1
    CsvCheckFeed1;1;1;1.1/1.1/1.1;1.1;VAT_10;false;First Vendor;odin;1
    CsvCheckFeed2;-2;1;2.2/2.2/2.2;2.2;VAT_18;false;Second Vendor;dva;2
    ;3;1;3.3/3.3/3.3;3.3;VAT_18;false;Third Vendor;tri;0
    CsvCheckFeed2;1;1;1.1/1.1/1.1;1.1;VAT_10;false;First Vendor;tri;lalala
)wrap");

static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "CsvCheckFeed1",
        "Price": 1,
        "IsValid": 1,
        "Position": "2:0",
    },
    {
        "OfferId": "CsvCheckFeed1",
        "IsValid": 0,
        "Position": "3:0",
        "Price": 1,
    },
    {
        "OfferId": "CsvCheckFeed2",
        "IsValid": 0,
        "Position": "4:0",
    },
    {
        "OfferId": "",
        "IsValid": 0,
        "Position": "5:0",
        "Price": 3,
    },
    {
        "OfferId": "CsvCheckFeed2",
        "Price": 1,
        "IsValid": 0,
        "Position": "6:0",
    }
]
)wrap");

TEST(BlueCsvParser, CheckCsvFeed) {
    auto feedInfo = GetDefaultBlueFeedInfo(EFeedType::CSV);
    feedInfo.FeedId = 12345;
    feedInfo.CheckFeedMode = true;
    feedInfo.XlS2CsvOutput = "X-MARKET-TEMPLATE: SUPPLIER";

    const auto [actual, checkResult] = RunFeedParserWithCheckFeed<NBlue::TCsvFeedParser>(
            INPUT_CSV,
            [](const TQueueItem& item) {
                NSc::TValue result;
                result["OfferId"] = item->DataCampOffer.identifiers().offer_id();

                if (item->RawPrice) {
                    result["Price"] = *item->RawPrice;
                }
                result["Position"] = item->Position;
                result["IsValid"] = item->IsValid;

                return result;
            }
            ,
            feedInfo
    );

    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);

    ASSERT_STREQ(checkResult.xls2csv_output(), "X-MARKET-TEMPLATE: SUPPLIER");
    ASSERT_STREQ(checkResult.feed_type(), "CSV");

    ASSERT_EQ(checkResult.log_message().size(), 4);

    // test error for offer with not unique shop_sku
    const auto& dupSku = checkResult.log_message()[0];
    ASSERT_EQ(dupSku.code(), "45d");
    ASSERT_NE(dupSku.details().find("\"reason\":\"shop-sku is non unique\""), std::string::npos);
    ASSERT_EQ(dupSku.level(), 3);
    ASSERT_EQ(dupSku.offer_supplier_sku(), "CsvCheckFeed1");
    ASSERT_EQ(dupSku.position(), "3:0");
    ASSERT_EQ(dupSku.feed_id(), 12345);

    // test warning for offer with wrong price
    const auto& invalidPriceForCsvCheckFeed2 = checkResult.log_message()[1];
    ASSERT_EQ(invalidPriceForCsvCheckFeed2.code(), "453");
    ASSERT_EQ(invalidPriceForCsvCheckFeed2.level(), 3);
    ASSERT_EQ(invalidPriceForCsvCheckFeed2.offer_supplier_sku(), "CsvCheckFeed2");
    ASSERT_EQ(invalidPriceForCsvCheckFeed2.position(), "4:0");
    ASSERT_EQ(invalidPriceForCsvCheckFeed2.feed_id(), 12345);

    // test error for offer without shop_sku
    const auto& emptySku = checkResult.log_message()[2];
    ASSERT_EQ(emptySku.code(), "45e");
    ASSERT_EQ(emptySku.level(), 3);
    ASSERT_EQ(emptySku.offer_supplier_sku(), "");
    ASSERT_EQ(emptySku.position(), "5:0");
    ASSERT_EQ(emptySku.feed_id(), 12345);

    // test warning for offer with invalid stock
    const auto& invalidStockSku = checkResult.log_message()[3];
    ASSERT_EQ(invalidStockSku.code(), "358");
    ASSERT_EQ(invalidStockSku.level(), 2);
    ASSERT_EQ(invalidStockSku.position(), "6:0");
    ASSERT_EQ(invalidStockSku.feed_id(), 12345);
}

TEST(BlueCsvParser, CheckXlsFeed) {
    auto feedInfo = GetDefaultBlueFeedInfo(EFeedType::XLS);
    feedInfo.FeedId = 12345;
    feedInfo.CheckFeedMode = true;
    feedInfo.XlS2CsvOutput = "X-MARKET-TEMPLATE: SUPPLIER";

    const auto [actual, checkResult] = RunFeedParserWithCheckFeed<NBlue::TCsvFeedParser>(
            INPUT_CSV,
            [](const TQueueItem& item) {
                NSc::TValue result;
                result["OfferId"] = item->DataCampOffer.identifiers().offer_id();

                if (item->RawPrice) {
                    result["Price"] = *item->RawPrice;
                }
                result["Position"] = item->Position;
                result["IsValid"] = item->IsValid;

                return result;
            },
            feedInfo
    );

    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);

    ASSERT_STREQ(checkResult.xls2csv_output(), "X-MARKET-TEMPLATE: SUPPLIER");
    ASSERT_STREQ(checkResult.feed_type(), "XLS");

    ASSERT_EQ(checkResult.log_message().size(), 4);

    // test error for offer with not unique shop_sku
    const auto& dupSku = checkResult.log_message()[0];
    ASSERT_EQ(dupSku.code(), "45d");
    ASSERT_NE(dupSku.details().find("\"reason\":\"shop-sku is non unique\""), std::string::npos);
    ASSERT_EQ(dupSku.level(), 3);
    ASSERT_EQ(dupSku.offer_supplier_sku(), "CsvCheckFeed1");
    ASSERT_EQ(dupSku.position(), "3:0");
    ASSERT_EQ(dupSku.feed_id(), 12345);

    // test warning for offer with wrong price
    const auto& invalidPriceForCsvCheckFeed2 = checkResult.log_message()[1];
    ASSERT_EQ(invalidPriceForCsvCheckFeed2.code(), "453");
    ASSERT_EQ(invalidPriceForCsvCheckFeed2.level(), 3);
    ASSERT_EQ(invalidPriceForCsvCheckFeed2.offer_supplier_sku(), "CsvCheckFeed2");
    ASSERT_EQ(invalidPriceForCsvCheckFeed2.position(), "4:0");
    ASSERT_EQ(invalidPriceForCsvCheckFeed2.feed_id(), 12345);

    // test error for offer without shop_sku
    const auto& emptySku = checkResult.log_message()[2];
    ASSERT_EQ(emptySku.code(), "45e");
    ASSERT_EQ(emptySku.level(), 3);
    ASSERT_EQ(emptySku.offer_supplier_sku(), "");
    ASSERT_EQ(emptySku.position(), "5:0");
    ASSERT_EQ(emptySku.feed_id(), 12345);

    // test warning for offer with invalid stock
    const auto& invalidStockSku = checkResult.log_message()[3];
    ASSERT_EQ(invalidStockSku.code(), "358");
    ASSERT_EQ(invalidStockSku.level(), 2);
    ASSERT_EQ(invalidStockSku.position(), "6:0");
    ASSERT_EQ(invalidStockSku.feed_id(), 12345);
}
