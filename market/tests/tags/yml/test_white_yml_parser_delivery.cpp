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
      <offer id="yml-with-delivery-t">
        <price>1</price>
        <delivery>true</delivery>
      </offer>
      <offer id="yml-with-delivery-f">
        <price>1</price>
        <delivery>false</delivery>
      </offer>
      <offer id="yml-with-delivery-t-ws">
        <price>1</price>
        <delivery>  true  </delivery>
      </offer>
      <offer id="yml-without-delivery">
        <price>1</price>
      </offer>
      <offer id="yml-with-bad-delivery">
        <price>1</price>
        <delivery>kakashka</delivery>
      </offer>
    </offers>
  </shop>
</yml_catalog>)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "yml-with-delivery-t",
        "Delivery": 1,
    },
    {
        "OfferId": "yml-with-delivery-f",
        "Delivery": 0,
    },
    {
        "OfferId": "yml-with-delivery-t-ws",
        "Delivery": 1,
    },
    {
        "OfferId": "yml-without-delivery"
    },
    {
        "OfferId": "yml-with-bad-delivery",
    },
]
)wrap");


TEST(WhiteYmlParser, Delivery) {
    const auto actual = RunWhiteYmlFeedParser<TYmlFeedParser>(
        INPUT_XML,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            if (item->GetOriginalPartnerDelivery().delivery().has_flag()) {
                result["Delivery"] = item->GetOriginalPartnerDelivery().delivery().flag();
            }
            return result;
        },
        GetDefaultWhiteFeedInfo(EFeedType::YML)
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
