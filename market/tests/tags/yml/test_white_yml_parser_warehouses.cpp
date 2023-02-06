#include <market/idx/feeds/qparser/tests/white_yml_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/white/yml/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket;


static const TString INPUT_XML(R"wrap(<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE yml_catalog SYSTEM 'shops.dtd'>
<yml_catalog date="2021-05-19 00:00">
  <shop>
    <offers>
      <offer>
        <shop-sku>white-yml-with-warehouses</shop-sku>
        <price>7</price>
        <warehouses>
          <warehouse id="123">
            <count>10</count>
          </warehouse>
          <warehouse id="124">
            <count>20</count>
          </warehouse>
          <warehouse id="125">
          </warehouse>
        </warehouses>
      </offer>
      <offer>
        <shop-sku>white-yml-with-empty-warehouses</shop-sku>
        <price>77</price>
        <warehouses>
        </warehouses>
      </offer>
      <offer>
        <shop-sku>white-yml-without-warehouses</shop-sku>
        <price>777</price>
      </offer>
    </offers>
  </shop>
</yml_catalog>)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "white-yml-with-warehouses",
        "Price": 7,
        "Warehouses": {"3": 10, "4": 20}
    },
    {
        "OfferId": "white-yml-with-empty-warehouses",
        "Price": 77,
        "Warehouses": {}
    },
    {
        "OfferId": "white-yml-without-warehouses",
        "Price": 777,
        "Warehouses": null
    }
]
)wrap");


TEST(WhiteYmlParser, Warehouses) {
    auto feedInfo = GetDefaultWhiteFeedInfo(EFeedType::YML);
    for (size_t i = 3; i < 6; ++i) {
        PartnerWarehouseInfo info;
        info.set_warehouse_id(i);
        info.set_feed_id(i + 30);

        feedInfo.PartnerWarehousesMapping[ToString(i + 120)] = info;
    }

    const auto actual = RunWhiteYmlFeedParser<TYmlFeedParser>(
            INPUT_XML,
            [](const TQueueItem& item) {
                NSc::TValue result;
                result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
                if (item->RawPrice) {
                    result["Price"] = *item->RawPrice;
                }
                result["Warehouses"] = NSc::TValue();
                if (item->Warehouses.Defined()) {
                    result["Warehouses"].SetDict();
                    for (const auto& [whId, whInfo] : (*item->Warehouses)) {
                        result["Warehouses"][ToString(whId)] = whInfo.Instock;
                    }
                }
                return result;
            },
            std::move(feedInfo)
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
