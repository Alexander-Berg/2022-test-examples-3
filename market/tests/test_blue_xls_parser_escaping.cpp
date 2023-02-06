#include "parser_test_runner.h"
#include "test_utils.h"

#include <market/idx/feeds/qparser/src/feed_parsers/blue/csv/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

#include <util/generic/string.h>

using namespace NMarket;

static const TString INPUT_CSV(R"wrap(shop_sku;price
    KEM3\\\\220;100
)wrap");

//MARKETINDEXER-29845: до исправлений в expected_json было бы 8 слешей (сейчас 4, а не 2, тк json => экранирование)
static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "IsValid": 1,
        "OfferId": "KEM3\\\\220"
    }
]
)wrap");

TEST(BlueXlsParser, Escaping) {
    const auto actual = RunFeedParser<NBlue::TCsvFeedParser>(
        INPUT_CSV,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["IsValid"] = item->IsValid;
            if (item->IsValid) {
                result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            }
            return result;
        },
        GetDefaultBlueFeedInfo(EFeedType::XLS)
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
