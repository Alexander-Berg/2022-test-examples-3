#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/blue/csv/feed_parser.h>
#include <market/idx/feeds/qparser/src/feed_parsers/white/csv/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket;


static const TString INPUT_CSV(R"wrap(shop-sku;price;dynamicpricing-type;dynamicpricing-unit;dynamicpricing-value
    csv-with-dynamicpricing;1;MINIMAL;%;15.34
    csv-without-dynamicpricing;2;;;
    csv-with-dynamicpricing-recommended;3;Рекомендованные цены;Руб.;230.5
    csv-with-dynamicpricing-minimal;4;Минимальные цены на маркетплейсе;%;99.9
    csv-with-unknown-type;5;UNKNOWN;Руб.;16
    csv-with-wrong-options;6;MINIMAL;abc;efd
)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "csv-with-dynamicpricing",
        "Price": 1,
        "DynamicPricingType": 2,
        "DynamicPricingUnit": "%",
        "DynamicPricingValue": 15.34,
    },
    {
        "OfferId": "csv-without-dynamicpricing",
        "Price": 2,
    },
    {
        "OfferId": "csv-with-dynamicpricing-recommended",
        "Price": 3,
        "DynamicPricingType": 1,
        "DynamicPricingUnit": "fixed",
        "DynamicPricingValue": 230.5,
    },
    {
        "OfferId": "csv-with-dynamicpricing-minimal",
        "Price": 4,
        "DynamicPricingType": 2,
        "DynamicPricingUnit": "%",
        "DynamicPricingValue": 99.9,
    },
    {
        "OfferId": "csv-with-unknown-type",
        "Price": 5,
        "DynamicPricingUnit": "fixed",
        "DynamicPricingValue": 16,
    },
    {
        "OfferId": "csv-with-wrong-options",
        "Price": 6,
        "DynamicPricingType": 2
    }

]
)wrap");

namespace {

    TMaybe<NSc::TValue> ProcessItem(const TQueueItem& item) {
        NSc::TValue result;
        result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
        if (item->RawPrice) {
            result["Price"] = *item->RawPrice;
        }
        if (item->RawDynamicPricing.ThresholdValue) {
            result["DynamicPricingValue"] = *item->RawDynamicPricing.ThresholdValue;
        }
        if (item->RawDynamicPricing.ThresholdUnit) {
            result["DynamicPricingUnit"] = (*item->RawDynamicPricing.ThresholdUnit == TDynamicPricing::PERCENT) ? "%" : "fixed";
        }
        if (item->DataCampOffer.price().dynamic_pricing().has_type()) {
            result["DynamicPricingType"] = int(item->DataCampOffer.price().dynamic_pricing().type());
        }
        return result;
    }

}

TEST(BlueCsvParser, DynamicPricing) {
    const auto actual = RunFeedParserWithTrace<NBlue::TCsvFeedParser>(
        INPUT_CSV,
        ProcessItem,
        GetDefaultBlueFeedInfo(EFeedType::CSV),
        "offers-trace.log"
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}

TEST(WhiteCsvParser, DynamicPricing) {
    const auto actual = RunFeedParserWithTrace<TCsvFeedParser>(
        INPUT_CSV,
        ProcessItem,
        GetDefaultWhiteFeedInfo(EFeedType::CSV),
        "offers-trace.log"
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
