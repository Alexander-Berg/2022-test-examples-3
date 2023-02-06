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
    <categories>
        <category id="1">cat1</category>
        <category id="2"></category>
        <category id="blabla">cat bla</category>
        <category id="1234567890123456789">cat big num</category>
        <category id="3" parentId="1">same name parent</category>
        <category id="4" parentId="1">same name parent</category>
        <category id="5" parentId="3">same name child</category>
        <category id="6" parentId="4">same name child</category>
    </categories>
    <offers>
      <offer id="11">
        <price>1000</price>
        <categoryId>2</categoryId>
      </offer>
      <offer id="12">
        <price>1000</price>
        <categoryId>1234567890123456789</categoryId>
      </offer>
      <offer id="1">
        <price>7</price>
        <categoryId>1</categoryId>
      </offer>
      <offer id="2">
        <price>77</price>
      </offer>
      <offer id="3">
        <price>150</price>
        <categoryId>0</categoryId>
      </offer>
      <offer id="4">
        <price>150</price>
        <categoryId>cat1</categoryId>
      </offer>
      <offer id="5">
        <price>150</price>
        <categoryId></categoryId>
      </offer>
      <offer id="300">
        <price>300</price>
        <categoryId>3</categoryId>
      </offer>
      <offer id="301">
        <price>301</price>
        <categoryId>4</categoryId>
      </offer>
      <offer id="302">
        <price>302</price>
        <categoryId>5</categoryId>
      </offer>
      <offer id="303">
        <price>303</price>
        <categoryId>6</categoryId>
      </offer>
    </offers>
  </shop>
</yml_catalog>)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "11",
        "Category": 2,
        "IsValid": 1,
    },
    {
        "OfferId": "12",
        "IsValid": 1,
    },
    {
        "OfferId": "1",
        "Category": 1,
        "IsValid": 1,
    },
    {
        "OfferId": "2",
        "IsValid": 1,
    },
    {
        "OfferId": "3",
        "IsValid": 1,
    },
    {
        "OfferId": "4",
        "IsValid": 1,
    },
    {
        "OfferId": "5",
        "IsValid": 1,
    },
    {
        "OfferId": "300",
        "Category": 3,
        "IsValid": 1,
    },
    {
        "OfferId": "301",
        "Category": 3,
        "IsValid": 1,
    },
    {
        "OfferId": "302",
        "Category": 5,
        "IsValid": 1,
    },
    {
        "OfferId": "303",
        "Category": 5,
        "IsValid": 1,
    },
]
)wrap");


TEST(WhiteYmlParser, CategoryId) {
    auto feedInfo = GetDefaultWhiteFeedInfo(EFeedType::YML);
    feedInfo.EnableDeduplicateCategories = true;
    const auto actual = RunWhiteYmlFeedParser<TYmlFeedParser>(
        INPUT_XML,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            if (item->GetOriginalSpecification().category().has_id()) {
                result["Category"] = item->GetOriginalSpecification().category().id();
            }
            result["IsValid"] = item->IsValid;
            return result;
        },
        feedInfo
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
