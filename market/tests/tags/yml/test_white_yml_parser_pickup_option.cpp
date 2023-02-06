#include <market/idx/feeds/qparser/tests/white_yml_test_runner.h>
#include <market/idx/feeds/qparser/src/feed_parsers/white/yml/feed_parser.h>
#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

#include <util/folder/tempdir.h>
#include <util/system/fs.h>

using namespace NMarket;

TEST(WhiteYmlParser, OfferPickupOption) {
    static const TString INPUT_XML(R"wrap(<?xml version="1.0" encoding="UTF-8"?>
    <!DOCTYPE yml_catalog SYSTEM 'shops.dtd'>
    <yml_catalog date="2019-12-03 00:00">
        <shop>
            <offers>
                <offer id="offer1">
                    <price>300</price>
                    <pickup-options>
                        <option cost="300" days="3-4" order-before="18"/>
                        <option cost="400" days="2" order-before="5"/>
                        <option cost="500" order-before="5"/>
                        <option cost="600" days="incorrect" order-before="5"/>
                        <option cost="700" days="2"/>
                    </pickup-options>
                </offer>
            </offers>
        </shop>
    </yml_catalog>)wrap");

    static const TString EXPECTED_JSON = TString(R"wrap([{
        "offerId": "offer1",
        "options": [{
            "cost": 300.00,
            "daysMin": 3,
            "daysMax": 4,
            "orderBeforeHour": 18
        }, {
            "cost": 400.00,
            "daysMin": 2,
            "daysMax": 2,
            "orderBeforeHour": 5
        }, {
            "cost": 500.00,
            "orderBeforeHour": 5
        }, {
            "cost": 600.00,
            "orderBeforeHour": 5
        }, {
            "cost": 700.00,
            "daysMin": 2,
            "daysMax": 2
        }]
    }]
    )wrap");

    const auto actual = RunWhiteYmlFeedParser<TYmlFeedParser>(
        INPUT_XML,
        [](const TQueueItem& item) {
            NSc::TValue result;
            const auto& delivery = item->GetOriginalPartnerDelivery();
            for (const auto& option : delivery.pickup_options().options()) {
                NSc::TValue value;
                value["cost"] = option.GetCost();
                if (option.HasDaysMin() && option.HasDaysMax()) {
                    value["daysMin"] = option.GetDaysMin();
                    value["daysMax"] = option.GetDaysMax();
                }
                if (option.HasOrderBeforeHour()) {
                    value["orderBeforeHour"] = option.GetOrderBeforeHour();
                }
                result["options"].GetArrayMutable().push_back(std::move(value));
            }
            result["offerId"] = item->DataCampOffer.identifiers().offer_id();
            return result;
        }
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
