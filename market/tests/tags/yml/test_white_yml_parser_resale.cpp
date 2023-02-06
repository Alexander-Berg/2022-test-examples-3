#include <market/idx/feeds/qparser/tests/white_yml_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/white/yml/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>


using namespace NMarket;


static const TString INPUT_XML(R"wrap(<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE yml_catalog SYSTEM 'shops.dtd'>
<yml_catalog date="2019-12-03 00:00">
  <shop>
    <offers>
      <offer id="yml-with-resale-1">
        <price>1</price>
        <condition type="preowned"><quality>perfect</quality><reason>description</reason></condition>
      </offer>
      <offer id="yml-with-resale-2">
        <price>1</price>
        <condition type="refurbished"><quality>excellent</quality><reason>description</reason></condition>
      </offer>
      <offer id="yml-with-resale-3">
        <price>1</price>
        <condition type="showcasesample"><quality>good</quality><reason>description</reason></condition>
      </offer>
      <offer id="yml-with-resale-4">
        <price>1</price>
        <condition type="reduction"><quality>good</quality><reason>description</reason></condition>
      </offer>
      <offer id="yml-with-resale-bad-1">
        <price>1</price>
        <condition type="ololo"><quality>perfect</quality><reason>description</reason></condition>
      </offer>
      <offer id="yml-with-resale-bad-2">
        <price>1</price>
        <condition type="preowned"><quality/><reason>description</reason></condition>
      </offer>
      <offer id="yml-with-resale-good-3">
        <price>1</price>
        <condition type="refurbished"><quality>excellent</quality><description/></condition>
      </offer>
      <offer id="yml-with-resale-bad-4">
        <price>1</price>
        <condition type="showcasesample"><quality>taksebe</quality><reason>description</reason></condition>
      </offer>
      <offer id="yml-with-resale-bad-5">
        <price>1</price>
        <condition type="used"><quality>good</quality><reason>description</reason></condition>
      </offer>
      <offer id="yml-clear-resale">
        <price>1</price>
        <condition/>
      </offer>
      <offer id="yml-without-resale">
        <price>1</price>
      </offer>
    </offers>
  </shop>
</yml_catalog>)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {"OfferId":"yml-with-resale-1","IsResale":true,"Reason":"USED","Condition":"PERFECT","Description":"description"},
    {"OfferId":"yml-with-resale-2","IsResale":true,"Reason":"RESTORED","Condition":"EXCELLENT","Description":"description"},
    {"OfferId":"yml-with-resale-3","IsResale":true,"Reason":"SHOWCASE_SAMPLE","Condition":"WELL","Description":"description"},
    {"OfferId":"yml-with-resale-4","IsResale":true,"Reason":"REDUCTION","Condition":"WELL","Description":"description"},
    {"OfferId":"yml-with-resale-bad-1","Condition":"PERFECT","Description":"description"},
    {"OfferId":"yml-with-resale-bad-2","IsResale":true,"Reason":"USED","Description":"description"},
    {"OfferId":"yml-with-resale-good-3","IsResale":true,"Reason":"RESTORED","Condition":"EXCELLENT"},
    {"OfferId":"yml-with-resale-bad-4","IsResale":true,"Reason":"SHOWCASE_SAMPLE","Description":"description"},
    {"OfferId":"yml-with-resale-bad-5","Condition":"WELL","Description":"description"},
    {"OfferId":"yml-clear-resale","IsResale":false},
    {"OfferId":"yml-without-resale"}
]
)wrap");


TEST(WhiteYmlParser, Resale) {
    const auto actual = RunWhiteYmlFeedParser<TYmlFeedParser>(
        INPUT_XML,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            const auto& spec = item->GetOriginalSpecification();
            if (spec.is_resale().has_flag()) {
              result["IsResale"] = spec.is_resale().flag();
            }
            if (spec.resale_reason().has_value()) {
              result["Reason"] = Market::DataCamp::ResaleReason_Type_Name(spec.resale_reason().value());
            }
            if (spec.resale_condition().has_value()) {
              result["Condition"] = Market::DataCamp::ResaleCondition_Type_Name(spec.resale_condition().value());
            }
            if (spec.resale_description().has_value()) {
              result["Description"] = spec.resale_description().value();
            }
            return result;
        },
        GetDefaultWhiteFeedInfo(EFeedType::YML)
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
