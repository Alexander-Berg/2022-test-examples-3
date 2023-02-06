#include <market/idx/feeds/qparser/tests/blue_yml_test_runner.h>
#include <market/idx/feeds/qparser/tests/datacamp_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/blue/yml/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>


using namespace NMarket;


static const TString INPUT_XML(R"wrap(<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE yml_catalog SYSTEM 'shops.dtd'>
<yml_catalog date="2021-03-26 13:10">
  <shop>
    <offers>
      <offer>
         <shop-sku>without-cargo-types</shop-sku>
      </offer>
      <offer>
         <shop-sku>with-empty-cargo-types</shop-sku>
         <cargo-types></cargo-types>
      </offer>
      <offer>
         <shop-sku>simple</shop-sku>
         <cargo-types>cis_required</cargo-types>
      </offer>
      <offer>
         <shop-sku>wrong-cargo-type</shop-sku>
         <cargo-types>something;unknown</cargo-types>
      </offer>
      <offer>
         <shop-sku>upper-case-cargo-types</shop-sku>
         <cargo-types>CIS_REQUIRED</cargo-types>
      </offer>
    </offers>
  </shop>
</yml_catalog>)wrap");

static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "without-cargo-types",
    },
    {
        "OfferId": "with-empty-cargo-types",
        "CargoTypes": "",
    },
    {
        "OfferId": "simple",
        "CargoTypes": "980",
    },
    {
        "OfferId": "wrong-cargo-type",
        "CargoTypes": "",
    },
    {
        "OfferId": "upper-case-cargo-types",
        "CargoTypes": "980",
    },
]
)wrap");


TEST(BlueYmlParser, CargoTypes) {
    const auto actual = RunBlueYmlFeedParser<NBlue::TYmlFeedParser>(
        INPUT_XML,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();

            if (item->GetOriginalSpecification().cargo_types().has_meta()) {
                result["CargoTypes"] = GetCargoTypesAsString(item->DataCampOffer);
            }

            return result;
        }
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
