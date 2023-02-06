#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/blue/csv/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket;

static const TString INPUT_CSV(R"wrap(shop-sku;name;count
10193;"""Стул Конёк Горбунек КОМФОРТ с комплектом подушек, цвет: белый/графит""";5
10194;"""Стул Конёк Горбунек КОМФОРТ с комплектом подушек, цвет: сандал/графит""";0
10211;"""Стул Конёк Горбунек КОМФОРТ, цвет: Белый""";10
10214;"""Стул Конёк Горбунек КОМФОРТ, цвет: слоновая кость""";5
KF-ЛОФТ-2;"""Стул Конёк Горбунек КОМФОРТ, цвет: ЛОФТ-2""";5
KF-ЛОФТ-2-ограничитель;"""Стул Конёк Горбунек КОМФОРТ в комплекте с жестким ограничителем, цвет: Лофт-2""";0
)wrap");

static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "10193",
        "Count": 5,
        "IsValid": 1,
        "IsDisabled": 0,
        "IsFeedStockCountInvalid": 0,
    },
    {
        "OfferId": "10194",
        "Count": 0,
        "IsValid": 1,
        "IsDisabled": 0,
        "IsFeedStockCountInvalid": 0,
    },
    {
        "OfferId": "10211",
        "Count": 10,
        "IsValid": 1,
        "IsDisabled": 0,
        "IsFeedStockCountInvalid": 0,
    },
    {
        "OfferId": "10214",
        "Count": 5,
        "IsValid": 1,
        "IsDisabled": 0,
        "IsFeedStockCountInvalid": 0,
    },
    {
        "OfferId": "KF-ЛОФТ-2",
        "Count": 5,
        "IsValid": 1,
        "IsDisabled": 0,
        "IsFeedStockCountInvalid": 0,
    },
    {
        "OfferId": "KF-ЛОФТ-2-ограничитель",
        "Count": 0,
        "IsValid": 1,
        "IsDisabled": 0,
        "IsFeedStockCountInvalid": 0,
    },
]
)wrap");

TEST(BlueCsvStockFeedParser, TEST) {
    const auto actual = RunFeedParserWithTrace<NBlue::TCsvStockFeedParser>(
        INPUT_CSV,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            result["Count"] = item->DataCampOffer.stock_info().partner_stocks().count();
            result["IsValid"] = item->IsValid;
            result["IsDisabled"] = item->IsDisabled;
            result["IsFeedStockCountInvalid"] = item->FeedStockCountInvalid;
            return TMaybe<NSc::TValue>{result};
        },
        GetDefaultBlueFeedInfo(EFeedType::XLS),
        "offers-trace.log"
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
