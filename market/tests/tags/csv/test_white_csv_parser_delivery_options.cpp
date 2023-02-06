#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/white/csv/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>


using namespace NMarket;


static const TString INPUT_CSV(R"wrap(id;price;local_delivery_cost;local_delivery_days
    correct-days;1500;100.0;1-2
    empty-days;2500;200.0;
    incorrect-days;2500;200.0;incorrect
)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "offerId": "correct-days",
        "deliveryCost": 100.0,
        "deliveryDaysMin": 1,
        "deliveryDaysMax": 2,
        "isValid": 1
    },
    {
        "offerId": "empty-days",
        "deliveryCost": 200.0,
        "isValid": 1
    },
    {
        "offerId": "incorrect-days",
        "deliveryCost": 200.0,
        "isValid": 1
    }
]
)wrap");


TEST(WhiteCsvParser, DeliveryOptions) {
    const auto actual = RunFeedParserWithTrace<TCsvFeedParser>(
        INPUT_CSV,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["offerId"] = item->DataCampOffer.identifiers().offer_id();
            const auto& delivery = item->GetOriginalPartnerDelivery().delivery_options();
            if (delivery.options_size()) {
                result["deliveryCost"] = delivery.options(0).GetCost();
                if (delivery.options(0).HasDaysMin() && delivery.options(0).HasDaysMax()) {
                    result["deliveryDaysMin"] = delivery.options(0).GetDaysMin();
                    result["deliveryDaysMax"] = delivery.options(0).GetDaysMax();
                }
                if (delivery.options(0).HasOrderBeforeHour()) {
                    result["orderBeforeHour"] = delivery.options(0).GetOrderBeforeHour();
                }
            }
            result["isValid"] = item->IsValid;
            return result;
        },
        GetDefaultWhiteFeedInfo(EFeedType::CSV),
        "offers-trace.log"
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
