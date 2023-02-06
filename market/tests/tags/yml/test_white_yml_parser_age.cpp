#include <market/idx/feeds/qparser/tests/blue_yml_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/blue/yml/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>
#include <market/idx/library/validators/age.h>


using namespace NMarket;


static const TString INPUT_XML(R"wrap(<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE yml_catalog SYSTEM 'shops.dtd'>
<yml_catalog date="2019-12-03 00:00">
  <shop>
    <offers>
      <offer>
        <shop-sku>yml-with-age</shop-sku>
        <price>7</price>
        <age unit="month">5</age>
      </offer>
      <offer>
        <shop-sku>yml-with-age-unknown-unit</shop-sku>
        <price>7</price>
        <age unit="something">5</age>
      </offer>
      <offer>
        <shop-sku>yml-with-age-empty-unit</shop-sku>
        <price>7</price>
        <age unit="">5</age>
      </offer>
      <offer>
        <shop-sku>yml-with-age-no-unit</shop-sku>
        <price>7</price>
        <age>5</age>
      </offer>
      <offer>
        <shop-sku>yml-without-age</shop-sku>
        <price>77</price>
      </offer>
      <offer>
        <age>sdf</age>
        <price>77</price>
        <shop-sku>yml-with-bad-age</shop-sku>
      </offer>
      <offer>
        <age unit="month">sdf</age>
        <price>77</price>
        <shop-sku>yml-with-bad-age-ok-unit</shop-sku>
      </offer>
    </offers>
  </shop>
</yml_catalog>)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "yml-with-age",
        "Age": "5",
        "RawUnit": "month",
        "Meta": "filled",
        "IsValid": 1
    },
    {
        "OfferId": "yml-with-age-unknown-unit",
        "Age": "5",
        "RawUnit": "something",
        "Meta": "filled",
        "IsValid": 1
    },
    {
        "OfferId": "yml-with-age-empty-unit",
        "Age": "5",
        "RawUnit": "_empty_",
        "Meta": "filled",
        "IsValid": 1
    },
    {
        "OfferId": "yml-with-age-no-unit",
        "Age": "5",
        "RawUnit": "_empty_",
        "Meta": "filled",
        "IsValid": 1
    },
    {
        "OfferId": "yml-without-age",
        "Age": "_empty_",
        "RawUnit": "_empty_",
        "Meta": "_empty_",
        "IsValid": 1
    },
    {
        "OfferId": "yml-with-bad-age",
        "Age": "_empty_",
        "RawUnit": "_empty_",
        "Meta": "filled",
        "IsValid": 1
    },
    {
        "OfferId": "yml-with-bad-age-ok-unit",
        "Age": "_empty_",
        "RawUnit": "month",
        "Meta": "filled",
        "IsValid": 1,
    }
]
)wrap");


TEST(BlueYmlParser, Age) {
    const auto actual = RunBlueYmlFeedParser<NBlue::TYmlFeedParser>(
        INPUT_XML,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();

            if (item->GetOriginalSpecification().age().has_value()) {
                result["Age"] = ToString(item->GetOriginalSpecification().age().value());
            } else {
                result["Age"] = "_empty_";
            }

            if (!item->RawAgeUnit.Empty()) {
                result["RawUnit"] = item->RawAgeUnit.GetRef();
            } else {
                result["RawUnit"] = "_empty_";
            }
            const auto& meta = item->GetOriginalSpecification().age().meta();
            if (meta.has_timestamp() &&
                meta.source() == Market::DataCamp::DataSource::PUSH_PARTNER_FEED &&
                meta.applier() == NMarketIndexer::Common::EComponent::QPARSER
            ) {
                result["Meta"] = "filled";
            } else {
                result["Meta"] = "_empty_";
            }
            result["IsValid"] = item->IsValid;
            return result;
        },
        GetDefaultBlueFeedInfo(EFeedType::YML)
);
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
