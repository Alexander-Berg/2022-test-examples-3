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
      <offer id="white-yml-with-valid-url"><price>1</price><url>http://www.test.com/test1</url></offer>
      <offer id="white-yml-with-invalid-url"><price>2</price><url>invalid</url></offer>
      <offer id="white-yml-with-empty-url"><price>3</price><url></url></offer>
      <offer id="white-yml-no-url"><price>4</price></offer>
    </offers>
  </shop>
</yml_catalog>
)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {"OfferId":"white-yml-with-valid-url","Url":"http://www.test.com/test1"},
    {"OfferId":"white-yml-with-invalid-url", "Url":"invalid"},
    {"OfferId":"white-yml-with-empty-url", "Url":""},
    {"OfferId":"white-yml-no-url"}
]
)wrap");


TEST(WhiteYmlParser, Url) {
    const auto actual = RunWhiteYmlFeedParser<TYmlFeedParser>(
        INPUT_XML,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            if (item->GetOriginalSpecification().has_url()) {
              result["Url"] = item->GetOriginalSpecification().url().value();
            }
            return result;
        },
        GetDefaultWhiteFeedInfo(EFeedType::YML)
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
