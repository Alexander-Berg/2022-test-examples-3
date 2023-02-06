#include <market/idx/feeds/qparser/tests/blue_yml_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/blue/yml/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

#include <util/string/join.h>

using namespace NMarket;

static const TString INPUT_XML(R"wrap(<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE yml_catalog SYSTEM 'shops.dtd'>
<yml_catalog>
  <shop>
    <offers>
      <offer>
        <shop-sku>no-pricelabs-params</shop-sku>
      </offer>
      <offer>
        <shop-sku>one-pricelabs-param</shop-sku>
        <pricelabs_param name="Остаток на складе">15</pricelabs_param>
      </offer>
      <offer>
        <shop-sku>many-pricelabs-params</shop-sku>
        <pricelabs_param name="Остаток на складе">20</pricelabs_param>
        <pricelabs_param name="Процент скидки">30</pricelabs_param>
        <pricelabs_param name="Кол-во в упаковке">5</pricelabs_param>
      </offer>
      <offer>
        <shop-sku>without-name</shop-sku>
        <pricelabs_param>100</pricelabs_param>
      </offer>
      <offer>
        <shop-sku>without-value</shop-sku>
        <pricelabs_param name="Остаток на складе"></pricelabs_param>
      </offer>
    </offers>
  </shop>
</yml_catalog>)wrap");

static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "no-pricelabs-params",
        "PricaLabsParams": ""
    },
    {
        "OfferId": "one-pricelabs-param",
        "PricaLabsParams": "Остаток на складе:15"
    },
    {
        "OfferId": "many-pricelabs-params",
        "PricaLabsParams": "Кол-во в упаковке:5 Остаток на складе:20 Процент скидки:30"
    },
    {
        "OfferId": "without-name",
        "PricaLabsParams": ""
    },
    {
        "OfferId": "without-value",
        "PricaLabsParams": ""
    }
]
)wrap");

TEST(BlueYmlParser, PricaLabsParams) {
    const auto actual = RunBlueYmlFeedParser<NBlue::TYmlFeedParser>(
        INPUT_XML,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            const auto& pricelabsParams = item->DataCampOffer.content().partner().original().pricelabs_params().params();
            TVector<TString> params;
            for (const auto& [name, value]: pricelabsParams) {
                params.emplace_back(name + ':' + value);
            }
            Sort(params.begin(), params.end());
            result["PricaLabsParams"] = JoinSeq(" ", params);
            return result;
        },
        GetDefaultBlueFeedInfo(EFeedType::YML));

    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
