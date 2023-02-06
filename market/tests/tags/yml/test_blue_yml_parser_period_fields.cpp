#include <market/idx/feeds/qparser/tests/blue_yml_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/blue/yml/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket;

namespace {
    void TestPeriodYmlTag(TString tagName, std::function<TMaybe<Market::DataCamp::Duration>(const NMarket::TOfferCarrier&)> extractValue){
        TStringBuilder inputCsv;
        const auto inputXML = SubstGlobalCopy<TString,TString>(R"www(<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE yml_catalog SYSTEM 'shops.dtd'>
<yml_catalog date="2019-12-03 00:00">
  <shop>
    <offers>
      <offer>
        <shop-sku>yml-with-{{tagName}}</shop-sku>
        <price>7</price>
        <{{tagName}}>P2Y2M10DT2H30M</{{tagName}}>
      </offer>
      <offer>
        <shop-sku>yml-with-empty-{{tagName}}</shop-sku>
        <price>77</price>
        <{{tagName}}></{{tagName}}>
      </offer>
      <offer>
        <shop-sku>yml-without-{{tagName}}</shop-sku>
        <price>77</price>
      </offer>
      <offer>
        <shop-sku>yml-with-wrong-{{tagName}}</shop-sku>
        <price>77</price>
        <{{tagName}}>P2YE2M10DT2H30EM</{{tagName}}>
      </offer>
      <offer>
        <shop-sku>yml-with-rus-{{tagName}}</shop-sku>
        <price>77</price>
        <{{tagName}}>1 день</{{tagName}}>
      </offer>
      <offer>
        <shop-sku>yml-p101d-{{tagName}}</shop-sku>
        <price>77</price>
        <{{tagName}}>P101D</{{tagName}}>
      </offer>
    </offers>
  </shop>
</yml_catalog>)www","{{tagName}}", tagName);

        const TString expectedJson = SubstGlobalCopy<TString,TString>(R"wrap(
[
    {
        "OfferId": "yml-with-{{tagName}}",
        "Value": "{ years: 2 months: 2 days: 10 hours: 2 minutes: 30 }"
    },
    {
        "OfferId": "yml-with-empty-{{tagName}}"
    },
    {
        "OfferId": "yml-without-{{tagName}}"
    },
    {
        "OfferId": "yml-with-wrong-{{tagName}}"
    },
    {
        "OfferId": "yml-with-rus-{{tagName}}",
        "Value": "{ days: 1 }"
    },
    {
        "OfferId": "yml-p101d-{{tagName}}",
        "Value": "{ months: 3 days: 11 }"
    },
]
)wrap", "{{tagName}}", tagName);

        const auto actual = RunBlueYmlFeedParser<NBlue::TYmlFeedParser>(
            inputXML,
            [&extractValue](const TQueueItem& item) {
              NSc::TValue result;
              result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
              if(extractValue(*item).Defined())
                  result["Value"] = ToString(*extractValue(*item));
              return result;
            });
        const auto expected = NSc::TValue::FromJson(expectedJson);
        ASSERT_EQ(actual, expected);
    };

}


TEST(BlueYmlParser, WarrantyDays) {
    TestPeriodYmlTag("warranty-days", [](const TOfferCarrier& item) {
        const auto& warranty = item.GetOriginalTerms().seller_warranty();
        return warranty.has_warranty_period()
            ? TMaybe<Market::DataCamp::Duration>(warranty.warranty_period())
            : Nothing();
    });
}

TEST(BlueYmlParser, ValidityDays) {
    TestPeriodYmlTag("period-of-validity-days", [](const TOfferCarrier& item) {
        const auto& expiry = item.GetOriginalSpecification().expiry();
        return expiry.has_validity_period()
            ? TMaybe<Market::DataCamp::Duration>(expiry.validity_period())
            : Nothing();
    });
}

TEST(BlueYmlParser, ServiceLifeDays) {
    TestPeriodYmlTag("service-life-days",  [](const TOfferCarrier& item) {
        const auto& warranty = item.GetOriginalSpecification().lifespan();
        return warranty.has_service_life_period()
            ? TMaybe<Market::DataCamp::Duration>(warranty.service_life_period())
            : Nothing();
    });
}
