#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/blue/csv/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket;

static const TString INPUT_CSV(R"wrap(shop_sku;price;type
    blue-csv-default;100;
    blue-csv-unknown;100;qazwsxedc
    blue-csv-vendormodel;100;vendor.model
    blue-csv-book;100;book
    blue-csv-audiobook;100;audiobook
    blue-csv-artisttitle;100;artist.title
    blue-csv-general;100;general
    blue-csv-medicine;100;medicine
    blue-csv-alcohol;100;alco
    blue-csv-tour;100;tour
    blue-csv-event-ticket;100;event-ticket
)wrap");

static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "blue-csv-default",
        "Type": 1
    },
    {
        "OfferId": "blue-csv-unknown",
        "Type": 1
    },
    {
        "OfferId": "blue-csv-vendormodel",
        "Type": 2
    },
    {
        "OfferId": "blue-csv-book",
        "Type": 4
    },
    {
        "OfferId": "blue-csv-audiobook",
        "Type": 5
    },
    {
        "OfferId": "blue-csv-artisttitle",
        "Type": 6
    },
    {
        "OfferId": "blue-csv-general",
        "Type": 1
    },
    {
        "OfferId": "blue-csv-medicine",
        "Type": 3
    },
    {
        "OfferId": "blue-csv-alcohol",
        "Type": 9
    },
    {
        "OfferId": "blue-csv-tour",
        "Type": 1
    },
    {
        "OfferId": "blue-csv-event-ticket",
        "Type": 1
    }
]
)wrap");

TEST(BlueCsvParser, Type) {
    const auto actual = RunFeedParser<NBlue::TCsvFeedParser>(
        INPUT_CSV,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            if (item->DataCampOffer.content().partner().original().has_type()) {
                result["Type"] = static_cast<int>(item->DataCampOffer.content().partner().original().type().value());
            }
            return result;
        },
        GetDefaultBlueFeedInfo(EFeedType::CSV)
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}

TEST(BlueCsvParser, NoTypeColumn) {
    const TString content(R"wrap(shop_sku;price
        someuniqid;100
    )wrap");

    const auto expected = NSc::TValue::FromJson(R"wrap(
    [
        {
            "OfferId": "someuniqid",
        },
    ]
    )wrap");

    const auto actual = RunFeedParser<NBlue::TCsvFeedParser>(
        content,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            if (item->DataCampOffer.content().partner().original().has_type()) {
                result["Type"] = static_cast<int>(item->DataCampOffer.content().partner().original().type().value());
            }
            return result;
        },
        GetDefaultBlueFeedInfo(EFeedType::CSV)
    );

    ASSERT_EQ(actual, expected);
}
