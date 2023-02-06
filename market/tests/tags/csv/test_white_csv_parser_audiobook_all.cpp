#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/white/csv/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>


using namespace NMarket;


static const TString INPUT_CSV(R"wrap(id;shop-sku;price;author;name;publisher;series;year;isbn;volume;part;language;table-of-contents;format;performed_by;performance_type;storage;recording_length;barcode
    csv-book1-id;csv-book1-shop-sku;500;Автор;Название;Издательство;Серия;1901;111-1-1111-1111-1;1;1;Русский;Оглавление;Формат;Исполнитель;Тип аудиокниги;Носитель;13.37;8-800-555-35-35
)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "author" : "Автор",
        "barcode_value" : "8-800-555-35-35, 111-1-1111-1111-1",
        "params_param[00]" : "{ name: \"author\" value: \"Автор\" }",
        "params_param[01]" : "{ name: \"language\" value: \"Русский\" }",
        "params_param[02]" : "{ name: \"part\" value: \"1\" }",
        "params_param[03]" : "{ name: \"publisher\" value: \"Издательство\" }",
        "params_param[04]" : "{ name: \"series\" value: \"Серия\" }",
        "params_param[05]" : "{ name: \"table_of_contents\" value: \"Оглавление\" }",
        "params_param[06]" : "{ name: \"volume\" value: \"1\" }",
        "params_param[07]" : "{ name: \"year\" value: \"1901\" }",
        "params_param[08]" : "{ name: \"format\" value: \"Формат\" }",
        "params_param[09]" : "{ name: \"performance_type\" value: \"Тип аудиокниги\" }",
        "params_param[10]" : "{ name: \"performed_by\" value: \"Исполнитель\" }",
        "params_param[11]" : "{ name: \"recording_length\" value: \"13.37\" }",
        "params_param[12]" : "{ name: \"storage\" value: \"Носитель\" }",
    },
]
)wrap");


TEST(WhiteCsvParser, AudiobookAll) {
const auto actual = RunFeedParserWithTrace<TCsvFeedParser>(
        INPUT_CSV,
        [](const TQueueItem& item) {
            NSc::TValue result;

            result["author"] = *item->TitlePrefixForSpecificTypes.Get();

            if (item->GetOriginalSpecification().has_barcode()) {
                result["barcode_value"] = ToString(item->GetOriginalSpecification().barcode().value());
            }

            if (item->GetOriginalSpecification().has_offer_params()) {
                result["params_param[00]"] = ToString(item->GetOriginalSpecification().offer_params().param()[0]);
                result["params_param[01]"] = ToString(item->GetOriginalSpecification().offer_params().param()[1]);
                result["params_param[02]"] = ToString(item->GetOriginalSpecification().offer_params().param()[2]);
                result["params_param[03]"] = ToString(item->GetOriginalSpecification().offer_params().param()[3]);
                result["params_param[04]"] = ToString(item->GetOriginalSpecification().offer_params().param()[4]);
                result["params_param[05]"] = ToString(item->GetOriginalSpecification().offer_params().param()[5]);
                result["params_param[06]"] = ToString(item->GetOriginalSpecification().offer_params().param()[6]);
                result["params_param[07]"] = ToString(item->GetOriginalSpecification().offer_params().param()[7]);
                result["params_param[08]"] = ToString(item->GetOriginalSpecification().offer_params().param()[8]);
                result["params_param[09]"] = ToString(item->GetOriginalSpecification().offer_params().param()[9]);
                result["params_param[10]"] = ToString(item->GetOriginalSpecification().offer_params().param()[10]);
                result["params_param[11]"] = ToString(item->GetOriginalSpecification().offer_params().param()[11]);
                result["params_param[12]"] = ToString(item->GetOriginalSpecification().offer_params().param()[12]);
            }

            return result;
        },
        GetDefaultWhiteFeedInfo(EFeedType::CSV),
        "offers-trace.log"
);
const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
ASSERT_EQ(actual, expected);
}
