#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/blue/csv/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket;


static const TString INPUT_CSV_1(R"wrap(shop-sku;price;vat
    csv-with-vat-18;1;VAT_18
    csv-with-invalid-vat;2;invalid
    csv-with-invalid-int-vat;3;-1
    csv-without-vat;4;
    csv-with-no-vat;5;NO_VAT
    csv-with-vat-repr;6;VAT_10
    csv-with-vat-int;7;3
)wrap");


static const TString EXPECTED_JSON_1 = TString(R"wrap(
[
    {
        "OfferId": "csv-with-vat-18",
        "Price": 1,
        "Vat": "1",
        "RawVatSource": "VAT_18",
    },
    {
        "OfferId": "csv-with-invalid-vat",
        "Price": 2,
        "Vat": "7",
        "RawVatSource": "invalid",
    },
    {
        "OfferId": "csv-with-invalid-int-vat",
        "Price": 3,
        "Vat": "7",
        "RawVatSource": "-1",
    },
    {
        "OfferId": "csv-without-vat",
        "Price": 4,
        "Vat": "7",
        "RawVatSource": "(empty maybe)",
    },
    {
        "OfferId": "csv-with-no-vat",
        "Price": 5,
        "Vat": "6",
        "RawVatSource": "NO_VAT",
    },
    {
        "OfferId": "csv-with-vat-repr",
        "Price": 6,
        "Vat": "2",
        "RawVatSource": "VAT_10",
    },
    {
        "OfferId": "csv-with-vat-int",
        "Price": 7,
        "Vat": "8",
        "RawVatSource": "3",
    },
]
)wrap");


TEST(BlueCsvParser, VatFromBasicPrice) {
    auto feedInfo = GetDefaultBlueFeedInfo(EFeedType::CSV);
    feedInfo.Vat = 7;
    const auto actual = RunFeedParserWithTrace<NBlue::TCsvFeedParser>(
        INPUT_CSV_1,
        [&feedInfo](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            if (item->RawPrice) {
                result["Price"] = *item->RawPrice;
            }
            // берем ндс из базовой цены
            const auto vat = item->DataCampOffer.price().basic().has_vat()
                ? item->DataCampOffer.price().basic().vat()
                : feedInfo.Vat;
            result["Vat"] = ToString(vat);
            result["RawVatSource"] = ToString(item->RawVatSource);
            return result;
        },
        feedInfo,
        "offers-trace.log"
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON_1);
    ASSERT_EQ(actual, expected);
}


static const TString INPUT_CSV_2(R"wrap(shop-sku;price;vat
    csv-with-vat-18;1;VAT_18
    csv-without-vat;4;
)wrap");


static const TString EXPECTED_JSON_2 = TString(R"wrap(
[
    {
        "OfferId": "csv-with-vat-18",
        "Price": 1,
        "Vat": 1,
        "RawVatSource": "VAT_18",
    },
    {
        "OfferId": "csv-without-vat",
        "Price": 4,
        "RawVatSource": "(empty maybe)",
    },
]
)wrap");


TEST(BlueCsvParser, VatFromOriginalPriceFields) {
    // проверяем, что ндс также кладется в поле original_price_fields
    auto feedInfo = GetDefaultBlueFeedInfo(EFeedType::CSV);
    const auto actual = RunFeedParserWithTrace<NBlue::TCsvFeedParser>(
        INPUT_CSV_2,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            if (item->RawPrice) {
                result["Price"] = *item->RawPrice;
            }
            // берем ндс из оригинальных ценовых полей
            if (item->DataCampOffer.price().original_price_fields().vat().has_value()) {
                const auto vat = item->DataCampOffer.price().original_price_fields().vat().value();
                result["Vat"] = vat;
            }
            result["RawVatSource"] = ToString(item->RawVatSource);
            return result;
        },
        feedInfo,
        "offers-trace.log"
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON_2);
    ASSERT_EQ(actual, expected);
}
