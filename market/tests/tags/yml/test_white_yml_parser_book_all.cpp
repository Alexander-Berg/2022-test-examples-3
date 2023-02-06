#include <market/idx/feeds/qparser/tests/white_yml_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/white/yml/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket;


static const TString INPUT_XML(R"wrap(<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE yml_catalog SYSTEM 'shops.dtd'>
<yml_catalog date="2021-06-16 00:00">
  <shop>
    <offers>
      <offer id="409282" type="book" available="true">
        <param name="Параметр 1" unit="Юнит 1">Значение 1</param>
        <price>440</price>
        <currencyId>RUR</currencyId>
        <categoryId>9094</categoryId>
        <author>Тестовый автор</author>
        <name>Атлас животных Русич</name>
        <publisher>Русич</publisher>
        <year>2019</year>
        <barcode>8-800-555-35-35</barcode>
        <ISBN>978-5-8138-1382-5, 978-5-8138-1187-6, 978-5-88590-680-7</ISBN>
        <age unit="year">6</age>
        <binding>Твердая глянцевая</binding>
        <page_extent>48</page_extent>
        <weight>0.790</weight>
        <param name="Параметр 2">Значение 2</param>
        <series>Тестовая серия</series>
        <volume>Тестовое количество томов</volume>
        <part>Тестовый том</part>
        <table_of_contents>Тестовое оглавление</table_of_contents>
        <language>Тестовый язык</language>
        <param name="Параметр 3" unit="Юнит 3">Значение 3</param>
      </offer>
    </offers>
  </shop>
</yml_catalog>)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "name" : "Атлас животных Русич",
        "title_prefix" : "Тестовый автор",
        "barcode_value" : "8-800-555-35-35, 978-5-8138-1382-5, 978-5-8138-1187-6, 978-5-88590-680-7",
        "params_param[0]" : "{ name: \"Параметр 1\" unit: \"Юнит 1\" value: \"Значение 1\" }",
        "params_param[1]" : "{ name: \"author\" value: \"Тестовый автор\" }",
        "params_param[2]" : "{ name: \"publisher\" value: \"Русич\" }",
        "params_param[3]" : "{ name: \"year\" value: \"2019\" }",
        "params_param[4]" : "{ name: \"binding\" value: \"Твердая глянцевая\" }",
        "params_param[5]" : "{ name: \"page_extent\" value: \"48\" }",
        "params_param[6]" : "{ name: \"Параметр 2\" unit: \"\" value: \"Значение 2\" }",
        "params_param[7]" : "{ name: \"series\" value: \"Тестовая серия\" }",
        "params_param[8]" : "{ name: \"volume\" value: \"Тестовое количество томов\" }",
        "params_param[9]" : "{ name: \"part\" value: \"Тестовый том\" }",
        "params_param[10]" : "{ name: \"table_of_contents\" value: \"Тестовое оглавление\" }",
        "params_param[11]" : "{ name: \"language\" value: \"Тестовый язык\" }",
        "params_param[12]" : "{ name: \"Параметр 3\" unit: \"Юнит 3\" value: \"Значение 3\" }",
    },
]
)wrap");


TEST(WhiteYmlParser, BookAll) {
    const auto actual = RunWhiteYmlFeedParser<TYmlFeedParser>(
        INPUT_XML,
        [](const TQueueItem& item) {
            NSc::TValue result;

            result["name"] = item->GetOriginalSpecification().name().value();
            result["title_prefix"] = *item->TitlePrefixForSpecificTypes.Get();

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
                result["params_param[10]"] = ToString(item->GetOriginalSpecification().offer_params().param()[10]);
                result["params_param[11]"] = ToString(item->GetOriginalSpecification().offer_params().param()[11]);
                result["params_param[12]"] = ToString(item->GetOriginalSpecification().offer_params().param()[12]);
            }

            return result;
        },
        GetDefaultBlueFeedInfo(EFeedType::YML)
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
