#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/blue/csv/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket;

static const TString INPUT_CSV(R"wrap(shop_sku;price
    ;100
    csv-qwer.,/\()[]-=1;100
    csv-qwerОдин;100
    csv-too-long-0000000000000000000000000000000000000000000000000000000000000000000000000;100
    csv-qwerЁ;100
    csv-qwer~;100
    csv qwe;100
)wrap");

static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "csv-qwer.,/\\()[]-=1",
        "IsValid": 1,
    },
    {
        "OfferId": "csv-qwerОдин",
        "IsValid": 1,
    },
]
)wrap");

static const TString INPUT_CSV_OFFER_ID(R"wrap(id;price
    csv-blue-check-id-1;100
    csv-blue-check-id-1;100
    csv-blue-check-id-2;100
)wrap");

static const TString INPUT_CSV_SHOP_SKU(R"wrap(shop_sku;price
    csv-blue-check-shop-sku-1;100
    csv-blue-check-shop-sku-2;100
)wrap");

static const TString INPUT_CSV_SHOP_SKU_AND_OFFER_ID(R"wrap(shop_sku;id;price
    csv-blue-check-shop-sku-1;csv-blue-check-id-1;100
    csv-blue-check-shop-sku-2;csv-blue-check-id-2;100
)wrap");

static const TString INPUT_CSV_OFFER_ID_AND_SHOP_SKU(R"wrap(id;shop_sku;price
    csv-blue-check-id-1;csv-blue-check-shop-sku-1;100
    csv-blue-check-id-2;csv-blue-check-shop-sku-2;100
)wrap");

static const TString EXPECTED_ID_JSON = TString(R"wrap(
[
    {
        "OfferId": "csv-blue-check-id-1",
        "IsValid": 1,
    },
    {
        "OfferId": "csv-blue-check-id-2",
        "IsValid": 1,
    },
]
)wrap");

static const TString EXPECTED_SKU_JSON = TString(R"wrap(
[
    {
        "OfferId": "csv-blue-check-shop-sku-1",
        "IsValid": 1,
    },
    {
        "OfferId": "csv-blue-check-shop-sku-2",
        "IsValid": 1,
    },
]
)wrap");

static NSc::TValue ExtractOfferId(const TQueueItem& item) {
    NSc::TValue result;
    result["IsValid"] = item->IsValid;
    if (item->IsValid) {
        result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
    }
    return result;
}

TEST(BlueCsvParser, Id) {
    const auto actual = RunFeedParserWithTrace<NBlue::TCsvFeedParser>(
        INPUT_CSV_OFFER_ID,
        ExtractOfferId,
        GetDefaultBlueFeedInfo(EFeedType::CSV),
        "offers-trace.log"
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_ID_JSON);
    ASSERT_EQ(actual, expected);
}

TEST(BlueCsvParser, OnlyShopSku) {
    const auto actual = RunFeedParserWithTrace<NBlue::TCsvFeedParser>(
        INPUT_CSV_SHOP_SKU,
        ExtractOfferId,
        GetDefaultBlueFeedInfo(EFeedType::CSV),
        "offers-trace.log"
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_SKU_JSON);
    ASSERT_EQ(actual, expected);
}

TEST(BlueCsvParser, SskuAndId) {
    const auto actual = RunFeedParserWithTrace<NBlue::TCsvFeedParser>(
        INPUT_CSV_SHOP_SKU_AND_OFFER_ID,
        ExtractOfferId,
        GetDefaultBlueFeedInfo(EFeedType::CSV),
        "offers-trace.log"
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_SKU_JSON);
    ASSERT_EQ(actual, expected);
}

TEST(BlueCsvParser, IdAndSsku) {
    const auto actual = RunFeedParserWithTrace<NBlue::TCsvFeedParser>(
        INPUT_CSV_OFFER_ID_AND_SHOP_SKU,
        ExtractOfferId,
        GetDefaultBlueFeedInfo(EFeedType::CSV),
        "offers-trace.log"
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_SKU_JSON);
    ASSERT_EQ(actual, expected);
}

TEST(BlueCsvParser, ShopSku) {
    const auto actual = RunFeedParserWithTrace<NBlue::TCsvFeedParser>(
        INPUT_CSV,
        ExtractOfferId,
        GetDefaultBlueFeedInfo(EFeedType::CSV),
        "offers-trace.log"
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
