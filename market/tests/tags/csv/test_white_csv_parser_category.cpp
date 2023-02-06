#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/white/csv/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>


using namespace NMarket;

static const TString INPUT_CSV(R"wrap(id;price;category
    csv-with-category;1000;some category
    csv-with-category1;100500;category1
    csv-without-category;200;
    csv-with-category2;300;category1
)wrap");

TEST(WhiteCsvParser, Category) {
    const auto actual = RunFeedParserWithTrace<TCsvFeedParser>(
            INPUT_CSV,
            [](const TQueueItem& item) {
                NSc::TValue result;
                result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
                result["CategoryId"] = item->GetOriginalSpecification().category().id();
                result["IsValid"] = item->IsValid;
                return result;
            },
            GetDefaultWhiteFeedInfo(EFeedType::CSV),
            "offers-trace.log"
    );

    THashMap<TString, TString> offers;
    for (const NSc::TValue& value : actual.GetArray()) {
        offers[value["OfferId"]] = value["CategoryId"];
    }

    ASSERT_EQ(offers.size(), 4);
    ASSERT_EQ(offers.at("csv-with-category1"), offers.at("csv-with-category2"));
}

static const TString INPUT_CSV_MARKETINDEXER_36993(R"wrap(id;available;delivery;local_delivery_cost;local_delivery_days;pickup;local-pickup-cost;local-pickup-days;store;url;vendor;name;category;price;oldprice;currencyId;picture;description;param;sales_notes;manufacturer_warranty;country_of_origin;barcode;bid;condition-type;condition-reason;credit-template-ids
59push;1;1;300;3;1;300;3;0;test.ru;;Вафельница;новая категория через эксель;3100;;RUR;;Отличный подарок для любителей венских вафель.;;Предоплата 50%;1;Россия;4606224016467;80;;;
)wrap");

TEST(WhiteCsvParser, Category_MARKETINDEXER_36993) {
    const auto actual = RunFeedParserWithTrace<TCsvFeedParser>(
            INPUT_CSV_MARKETINDEXER_36993,
            [](const TQueueItem& item) {
                NSc::TValue result;
                result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
                result["CategoryId"] = item->GetOriginalSpecification().category().id();
                result["CategoryName"] = item->GetOriginalSpecification().category().name();
                result["IsValid"] = item->IsValid;
                return result;
            },
            GetDefaultWhiteFeedInfo(EFeedType::CSV),
            "offers-trace.log"
    );

    THashMap<TString, TString> offers;
    for (const NSc::TValue& value : actual.GetArray()) {
        offers[value["OfferId"]] = value["CategoryName"];
    }

    ASSERT_EQ(offers.size(), 1);
    ASSERT_EQ(offers.at("59push"), "новая категория через эксель");
}
