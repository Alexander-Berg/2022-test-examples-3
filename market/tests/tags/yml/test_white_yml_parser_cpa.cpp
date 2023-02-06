#include <market/idx/feeds/qparser/tests/white_yml_test_runner.h>
#include <market/idx/feeds/qparser/src/feed_parsers/white/yml/feed_parser.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket;


static const TString INPUT_XML(R"wrap(<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE yml_catalog SYSTEM 'shops.dtd'>
<yml_catalog date="2019-12-03 00:00">
  <shop>
    <offers>
      <offer id="yml-with-cpa">
        <price>7</price>
        <cpa>1</cpa>
      </offer>
      <offer id="yml-with-cpa-0">
        <price>7</price>
        <cpa>0</cpa>
      </offer>
      <offer id="yml-with-cpa-2">
        <price>7</price>
        <cpa>2</cpa>
      </offer>
      <offer id="yml-without-cpa">
        <price>77</price>
      </offer>
    </offers>
  </shop>
</yml_catalog>)wrap");



static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "yml-with-cpa",
        "cpa": 1
    },
    {
        "OfferId": "yml-with-cpa-0",
        "cpa": 0
    },
    {
        "OfferId": "yml-with-cpa-2"
    },
    {
        "OfferId": "yml-without-cpa"
    }
]
)wrap");

void TestIt(const TString& ej, const TString& inp, const ECpaOption& option) {
    THolder<IFeedParser> feedParser;

    const auto actual = RunWhiteYmlFeedParser<TYmlFeedParser>(
        inp,
        [](const TQueueItem& item) {
          NSc::TValue result;
          result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
          if (item->Cpa != ECpaOption::NOTSET) {
              result["cpa"] = static_cast<int>(item->Cpa);
          }
          return result;
        },
        feedParser,
        GetDefaultWhiteFeedInfo(EFeedType::YML));
    const auto expected = NSc::TValue::FromJson(ej);
    ASSERT_EQ(actual, expected);
    ASSERT_EQ(feedParser->GetFeedShopInfo().CpaOption, option);
}

TEST(WhiteYmlParser, cpa)
{
    TestIt(EXPECTED_JSON, INPUT_XML, ECpaOption::NOTSET);
    TestIt(EXPECTED_JSON, SubstGlobalCopy(INPUT_XML,"<shop>","<shop><cpa>1</cpa>"), ECpaOption::ENABLED);
    TestIt(EXPECTED_JSON, SubstGlobalCopy(INPUT_XML,"<shop>","<shop><cpa>0</cpa>"), ECpaOption::DISABLED);
    TestIt(EXPECTED_JSON, SubstGlobalCopy(INPUT_XML,"<shop>","<shop><cpa>cpa</cpa>"), ECpaOption::NOTSET);
}
