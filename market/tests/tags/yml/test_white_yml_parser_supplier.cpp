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
      <offer id="yml-with-trash-supplier">
        <price>7</price>
        <supplier>supplier</supplier>
        <url>supplier</url>
      </offer>
      <offer id="yml-with-name-ogrn">
        <price>7</price>
        <supplier name="name" ogrn="12345678901234"></supplier>
      </offer>
      <offer id="yml-with-minus-ogrn">
        <price>7</price>
        <supplier name="" ogrn="-1234153"></supplier>
      </offer>
      <offer id="yml-with-bad-ogrn">
        <price>7</price>
        <supplier ogrn="abcd1234"></supplier>
      </offer>
    </offers>
  </shop>
</yml_catalog>)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "yml-with-trash-supplier"
    },
    {
        "OfferId": "yml-with-name-ogrn",
        "Ogrn": "12345678901234",
        "Name": "name"
    },
    {
        "OfferId": "yml-with-minus-ogrn"
    },
    {
        "OfferId": "yml-with-bad-ogrn"
    }
]
)wrap");


TEST(WhiteYmlParser, Supplier) {
    const auto actual = RunWhiteYmlFeedParser<TYmlFeedParser>(
        INPUT_XML,
        [](const TQueueItem& item) {
              NSc::TValue result;
              result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
              auto& info = item->DataCampOffer.content().partner().original().Getsupplier_info();
              if(info.has_ogrn())
                  result["Ogrn"] = info.ogrn();
              if(info.has_name())
                  result["Name"] = info.name();
              return result;
        },
        GetDefaultWhiteFeedInfo(EFeedType::YML)
);
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
