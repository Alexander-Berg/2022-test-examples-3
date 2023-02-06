#include <market/idx/feeds/qparser/tests/white_yml_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/white/yml/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

// WARNING!! IT IS GENERATED. IT IS TEMPLATE!!!


using namespace NMarket;


static const TString INPUT_XML(R"wrap(<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE yml_catalog SYSTEM 'shops.dtd'>
<yml_catalog date="2019-12-03 00:00">
  <shop>
    <offers>
      <offer id="yml-with-condition-1">
        <price>1</price>
        <condition type="used"><reason>reason</reason></condition>
      </offer>
      <offer id="yml-with-condition-2">
        <price>1</price>
        <condition type="likenew"><reason>reason</reason></condition>
      </offer>
      <offer id="yml-with-condition-bad-1">
        <price>1</price>
        <condition type="ololo"><reason>reason</reason></condition>
      </offer>
      <offer id="yml-with-condition-bad-2">
        <price>1</price>
        <condition type="likenew"><reason></reason></condition>
      </offer>
      <offer id="yml-without-condition">
        <price>1</price>
      </offer>
    </offers>
  </shop>
</yml_catalog>)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {"OfferId":"yml-with-condition-1","Reason":"reason","Type":"USED"},
    {"OfferId":"yml-with-condition-2","Reason":"reason","Type":"LIKENEW"},
    {"OfferId":"yml-with-condition-bad-1","Reason":"reason"},
    {"OfferId":"yml-with-condition-bad-2","Type":"LIKENEW"},
    {"OfferId":"yml-without-condition"}
]
)wrap");


TEST(WhiteYmlParser, Condition) {
    const auto actual = RunWhiteYmlFeedParser<TYmlFeedParser>(
        INPUT_XML,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            if(item->GetOriginalSpecification().condition().has_type()) {
              result["Type"] = Market::DataCamp::Condition_Type_Name(item->GetOriginalSpecification().condition().type());
            }
            if (item->GetOriginalSpecification().condition().has_reason()) {
              result["Reason"] = item->GetOriginalSpecification().condition().reason();
            }
            return result;
        },
        GetDefaultWhiteFeedInfo(EFeedType::YML)
);
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
