#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/white/csv/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>


using namespace NMarket;


static const TString INPUT_CSV(R"wrap(id;shop-sku;price;author;name;publisher;series;year;isbn;volume;part;language;table-of-contents;page-extent;binding;barcode
    csv-book1-id;csv-book1-shop-sku;500;Автор;Название;Издательство;Серия;1901;111-1-1111-1111-1;1;1;Русский;Оглавление;100;Твердая обложка;8-800-555-35-35
)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "name" : "Название",
        "author" : "Автор",
        "barcode_value" : "8-800-555-35-35, 111-1-1111-1111-1",
        "params_param[0]" : "{ name: \"author\" value: \"Автор\" }",
        "params_param[1]" : "{ name: \"binding\" value: \"Твердая обложка\" }",
        "params_param[2]" : "{ name: \"language\" value: \"Русский\" }",
        "params_param[3]" : "{ name: \"page_extent\" value: \"100\" }",
        "params_param[4]" : "{ name: \"part\" value: \"1\" }",
        "params_param[5]" : "{ name: \"publisher\" value: \"Издательство\" }",
        "params_param[6]" : "{ name: \"series\" value: \"Серия\" }",
        "params_param[7]" : "{ name: \"table_of_contents\" value: \"Оглавление\" }",
        "params_param[8]" : "{ name: \"volume\" value: \"1\" }",
        "params_param[9]" : "{ name: \"year\" value: \"1901\" }",
    },
]
)wrap");


TEST(WhiteCsvParser, BookAll) {
const auto actual = RunFeedParserWithTrace<TCsvFeedParser>(
        INPUT_CSV,
        [](const TQueueItem& item) {
            NSc::TValue result;

            result["name"] = item->GetOriginalSpecification().name().value();
            result["author"] = *item->TitlePrefixForSpecificTypes.Get();

            if (item->GetOriginalSpecification().has_barcode()) {
                result["barcode_value"] = ToString(item->GetOriginalSpecification().barcode().value());
            }

            if (item->GetOriginalSpecification().has_offer_params()) {
                result["params_param[0]"] = ToString(item->GetOriginalSpecification().offer_params().param()[0]);
                result["params_param[1]"] = ToString(item->GetOriginalSpecification().offer_params().param()[1]);
                result["params_param[2]"] = ToString(item->GetOriginalSpecification().offer_params().param()[2]);
                result["params_param[3]"] = ToString(item->GetOriginalSpecification().offer_params().param()[3]);
                result["params_param[4]"] = ToString(item->GetOriginalSpecification().offer_params().param()[4]);
                result["params_param[5]"] = ToString(item->GetOriginalSpecification().offer_params().param()[5]);
                result["params_param[6]"] = ToString(item->GetOriginalSpecification().offer_params().param()[6]);
                result["params_param[7]"] = ToString(item->GetOriginalSpecification().offer_params().param()[7]);
                result["params_param[8]"] = ToString(item->GetOriginalSpecification().offer_params().param()[8]);
                result["params_param[9]"] = ToString(item->GetOriginalSpecification().offer_params().param()[9]);
            }

            return result;
        },
        GetDefaultWhiteFeedInfo(EFeedType::CSV),
        "offers-trace.log"
);
const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
ASSERT_EQ(actual, expected);
}
