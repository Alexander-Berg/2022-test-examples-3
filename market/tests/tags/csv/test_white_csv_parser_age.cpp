#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/blue/csv/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>
#include <market/idx/library/validators/age.h>


using namespace NMarket;

static const TString INPUT_CSV(R"wrap(shop-sku;price;age;age_unit
    csv-with-age-year;7;5;year
    csv-with-age-month;7;5;month
    csv-with-age-no-unit;7;5;
    csv-no-age-with-unit;7;;month
    csv-no-age-no-unit;77;;
    csv-with-wrong-age;7;wrong-age;
)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "csv-with-age-year",
        "Age": "5",
        "RawUnit": "year",
        "Meta": "filled",
        "IsValid": 1
    },
    {
        "OfferId": "csv-with-age-month",
        "Age": "5",
        "RawUnit": "month",
        "Meta": "filled",
        "IsValid": 1
    },
    {
        "OfferId": "csv-with-age-no-unit",
        "Age": "5",
        "RawUnit": "_empty_",
        "Meta": "filled",
        "IsValid": 1
    },
    {
        "OfferId": "csv-no-age-with-unit",
        "Age": "_empty_",
        "RawUnit": "month",
        "Meta": "filled",
        "IsValid": 1
    },
    {
        "OfferId": "csv-no-age-no-unit",
        "Age": "_empty_",
        "RawUnit": "_empty_",
        "Meta": "filled",
        "IsValid": 1
    },
    {
        "OfferId": "csv-with-wrong-age",
        "Age": "_empty_",
        "RawUnit": "_empty_",
        "Meta": "filled",
        "IsValid": 1
    }
]
)wrap");


TEST(BlueCsvParser, CsvAgeValueAndUnit) {
const auto actual = RunFeedParserWithTrace<NBlue::TCsvFeedParser>(
        INPUT_CSV,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            if (item->GetOriginalSpecification().age().has_value()) {
                result["Age"] = ToString(item->GetOriginalSpecification().age().value());
            } else {
                result["Age"] = "_empty_";
            }
            if (!item->RawAgeUnit.Empty()) {
                result["RawUnit"] = item->RawAgeUnit.GetRef();
            } else {
                result["RawUnit"] = "_empty_";
            }
            const auto& meta = item->GetOriginalSpecification().age().meta();
            if (meta.has_timestamp() &&
                meta.source() == Market::DataCamp::DataSource::PUSH_PARTNER_FEED &&
                meta.applier() == NMarketIndexer::Common::EComponent::QPARSER
            ) {
                result["Meta"] = "filled";
            } else {
                result["Meta"] = "_empty_";
            }
            result["IsValid"] = item->IsValid;
            return result;
        },
        GetDefaultBlueFeedInfo(EFeedType::CSV),
        "offers-trace.log"
);
const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
ASSERT_EQ(actual, expected);
}
