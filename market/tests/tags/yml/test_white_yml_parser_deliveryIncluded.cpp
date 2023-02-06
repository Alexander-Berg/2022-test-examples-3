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
      <offer id="yml-with-deliveryIncluded-t">
        <price>1</price>
        <deliveryIncluded>true</deliveryIncluded>
      </offer>
      <offer id="yml-with-deliveryIncluded-f">
        <price>1</price>
        <deliveryIncluded>false</deliveryIncluded>
      </offer>
      <offer id="yml-with-deliveryIncluded-t-ws">
        <price>1</price>
        <deliveryIncluded>  true  </deliveryIncluded>
      </offer>
      <offer id="yml-without-deliveryIncluded">
        <price>1</price>
      </offer>
      <offer id="yml-with-bad-deliveryIncluded">
        <price>1</price>
        <deliveryIncluded>kakashka</deliveryIncluded>
      </offer>
    </offers>
  </shop>
</yml_catalog>)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "yml-with-deliveryIncluded-t",
        "DeliveryIncluded": 1,
    },
    {
        "OfferId": "yml-with-deliveryIncluded-f",
        "DeliveryIncluded": 0,
    },
    {
        "OfferId": "yml-with-deliveryIncluded-t-ws",
        "DeliveryIncluded": 1,
    },
    {
        "OfferId": "yml-without-deliveryIncluded"
    },
    {
        "OfferId": "yml-with-bad-deliveryIncluded"
    }

]
)wrap");


TEST(WhiteYmlParser, DeliveryIncluded) {
    const auto actual = RunWhiteYmlFeedParser<TYmlFeedParser>(
        INPUT_XML,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            if (item->GetOfferPrice().basic().has_delivery_included()) {
                result["DeliveryIncluded"] = item->GetOfferPrice().basic().delivery_included();
            }
            return result;
        },
        GetDefaultWhiteFeedInfo(EFeedType::YML)
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
