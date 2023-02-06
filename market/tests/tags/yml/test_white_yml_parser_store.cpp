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
      <offer id="yml-with-store-t">
        <price>1</price>
        <store>true</store>
      </offer>
      <offer id="yml-with-store-f">
        <price>1</price>
        <store>false</store>
      </offer>
      <offer id="yml-with-store-t-ws">
        <price>1</price>
        <store>  true  </store>
      </offer>
      <offer id="yml-without-store">
        <price>1</price>
      </offer>
      <offer id="yml-with-bad-store">
        <price>1</price>
        <store>kakashka</store>
      </offer>
    </offers>
  </shop>
</yml_catalog>)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "yml-with-store-t",
        "Store": 1,
    },
    {
        "OfferId": "yml-with-store-f",
        "Store": 0,
    },
    {
        "OfferId": "yml-with-store-t-ws",
        "Store": 1,
    },
    {
        "OfferId": "yml-without-store"
    },
    {
        "OfferId": "yml-with-bad-store",
        "Store": 0,
    },
]
)wrap");


TEST(WhiteYmlParser, Store) {
    const auto actual = RunWhiteYmlFeedParser<TYmlFeedParser>(
        INPUT_XML,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            if (item->GetOriginalPartnerDelivery().has_store()) {
                result["Store"] = item->GetOriginalPartnerDelivery().store().flag();
            }
            return result;
        },
        GetDefaultWhiteFeedInfo(EFeedType::YML)
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
