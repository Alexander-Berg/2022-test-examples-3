#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <market/idx/feeds/qparser/src/feed_parsers/blue/csv/feed_parser.h>
using namespace NMarket;

namespace {
    void TestPeriodCsvTag(TString tagName, std::function<TMaybe<Market::DataCamp::Duration>(const NMarket::TOfferCarrier&)> extractValue) {
        TString idKey = SubstGlobalCopy(tagName, '_', '-');
        const auto inputCsv = SubstGlobalCopy<TString, TString>(SubstGlobalCopy<TString, TString>(
R"www(shop-sku;price;{{tagName}}
csv-with-{{id}};1;P2Y2M10DT2H30M
csv-with-{{id}}-rus;1;1 год
csv-with-empty-{{id}};1;
csv-with-wrong-{{id}};1;P2YE2M10DT2H30EM)www", "{{tagName}}", tagName), "{{id}}", idKey);

        const TString expectedJson = SubstGlobalCopy<TString, TString>(SubstGlobalCopy<TString, TString>(R"wrap(
[
    {
        "OfferId": "csv-with-{{id}}",
        "Value": "{ years: 2 months: 2 days: 10 hours: 2 minutes: 30 }",
    },
    {
        "OfferId": "csv-with-{{id}}-rus",
        "Value": "{ years: 1 }",
    },
    {
        "OfferId": "csv-with-empty-{{id}}"
    },
    {
        "OfferId": "csv-with-wrong-{{id}}"
    }
]
)wrap", "{{tagName}}", tagName),
        "{{id}}", idKey);

        const auto actual = RunFeedParserWithTrace<NBlue::TCsvFeedParser>(
            inputCsv,
            [&extractValue](const TQueueItem& item) {
                NSc::TValue result;
                result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
                if (extractValue(*item).Defined())
                    result["Value"] = ToString(*extractValue(*item));
                return result;
            },
            GetDefaultBlueFeedInfo(EFeedType::CSV),
            "offers-trace.log");
        const auto expected = NSc::TValue::FromJson(expectedJson);
        ASSERT_EQ(actual, expected);
    };

} // namespace

TEST(BlueCsvParser, WarrantyDays) {
    TestPeriodCsvTag("warranty_days", [](const TOfferCarrier& item) {
        const auto& warranty = item.GetOriginalTerms().seller_warranty();
        return warranty.has_warranty_period() ? TMaybe<Market::DataCamp::Duration>(warranty.warranty_period()) : Nothing();
    });
}

TEST(BlueCsvParser, ValidityDays) {
    TestPeriodCsvTag("period_of_validity_days", [](const TOfferCarrier& item) {
        const auto& expiry = item.GetOriginalSpecification().expiry();
        return expiry.has_validity_period() ? TMaybe<Market::DataCamp::Duration>(expiry.validity_period()) : Nothing();
    });
}

TEST(BlueCsvParser, ServiceLifeDays) {
    TestPeriodCsvTag("service_life_days",  [](const TOfferCarrier& item) {
      const auto& warranty = item.GetOriginalSpecification().lifespan();
      return warranty.has_service_life_period() ? TMaybe<Market::DataCamp::Duration>(warranty.service_life_period()): Nothing();
    });
}
