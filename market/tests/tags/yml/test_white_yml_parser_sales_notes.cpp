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
      <offer id="yml-with-sales-notes">
        <price>7</price>
        <sales_notes>sales_notesабвгдабвгдабвгдабвгдабвг</sales_notes>
      </offer>
      <offer id="yml-without-sales-notes">
        <price>77</price>
      </offer>
      <offer id="yml-with-wrong-sales-notes">
        <price>77</price>
        <sales_notes>sales_notesабвгдабвгдабвгдабвгдабвгдабвгдабвгдабвгдабвгдабвгдабвгдабвгдsales_notesабвгдабвгдабвгдабвгдабвгдабвгдабвгдабвгдабвгдабвгдабвгдабвгд</sales_notes>
      </offer>
    </offers>
  </shop>
</yml_catalog>)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "yml-with-sales-notes",
        "SalesNotes": "sales_notesабвгдабвгдабвгдабвгдабвг",
    },
    {
        "OfferId": "yml-without-sales-notes",
    },
    {
        "OfferId": "yml-with-wrong-sales-notes",
        "SalesNotes": "sales_notesабвгдабвгдабвгдабвгдабвгдабвгдабвгдабвгдабвгдабвгдабвгдабвгдsales_notesабвгдабвгдабвгдабвгдабвгдабвгдабвгдабвгдабвгдабвгдабвгдабвгд",
    }
]
)wrap");


TEST(WhiteYmlParser, SalesNotes) {
    const auto actual = RunWhiteYmlFeedParser<TYmlFeedParser>(
        INPUT_XML,
        [](const TQueueItem& item) {
          NSc::TValue result;
          result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
          auto& originalTerms = item->DataCampOffer.content().partner().original_terms();
          if(originalTerms.has_sales_notes()) {
              result["SalesNotes"] = originalTerms.sales_notes().value();
          }
          return result;
        },
        GetDefaultWhiteFeedInfo(EFeedType::YML)
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
