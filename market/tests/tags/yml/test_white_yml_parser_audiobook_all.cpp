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
      <offer id="409282" type="audiobook" available="true">
        <param name="Параметр 1" unit="Юнит 1">Значение 1</param>
        <price>440</price>
        <currencyId>RUR</currencyId>
        <categoryId>9094</categoryId>
        <author>Автор</author>
        <name>Название</name>
        <publisher>Издатель</publisher>
        <year>1901</year>
        <barcode>8-800-555-35-35</barcode>
        <ISBN>978-5-8138-1382-5, 978-5-8138-1187-6, 978-5-88590-680-7</ISBN>
        <series>Серия</series>
        <volume>Количество томов</volume>
        <part>Номер тома</part>
        <table_of_contents>Оглавление</table_of_contents>
        <language>Язык</language>
        <format>Формат</format>
        <performed_by>Исполнитель</performed_by>
        <performance_type>Тип аудиокниги</performance_type>
        <storage>Носитель</storage>
        <recording_length>13.37</recording_length>
        <param name="Параметр 3" unit="Юнит 3">Значение 3</param>
      </offer>
    </offers>
  </shop>
</yml_catalog>)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "title_prefix" : "Автор",
        "barcode_value" : "8-800-555-35-35, 978-5-8138-1382-5, 978-5-8138-1187-6, 978-5-88590-680-7",
        "params_param[0]" : "{ name: \"Параметр 1\" unit: \"Юнит 1\" value: \"Значение 1\" }",
        "params_param[1]" : "{ name: \"author\" value: \"Автор\" }",
        "params_param[2]" : "{ name: \"publisher\" value: \"Издатель\" }",
        "params_param[3]" : "{ name: \"year\" value: \"1901\" }",
        "params_param[4]" : "{ name: \"series\" value: \"Серия\" }",
        "params_param[5]" : "{ name: \"volume\" value: \"Количество томов\" }",
        "params_param[6]" : "{ name: \"part\" value: \"Номер тома\" }",
        "params_param[7]" : "{ name: \"table_of_contents\" value: \"Оглавление\" }",
        "params_param[8]" : "{ name: \"language\" value: \"Язык\" }",
        "params_param[9]" : "{ name: \"format\" value: \"Формат\" }",
        "params_param[10]" : "{ name: \"performed_by\" value: \"Исполнитель\" }",
        "params_param[11]" : "{ name: \"performance_type\" value: \"Тип аудиокниги\" }",
        "params_param[12]" : "{ name: \"storage\" value: \"Носитель\" }",
        "params_param[13]" : "{ name: \"recording_length\" value: \"13.37\" }",
        "params_param[14]" : "{ name: \"Параметр 3\" unit: \"Юнит 3\" value: \"Значение 3\" }",
    },
]
)wrap");


TEST(WhiteYmlParser, AudiobookAll) {
    const auto actual = RunWhiteYmlFeedParser<TYmlFeedParser>(
        INPUT_XML,
        [](const TQueueItem& item) {
            NSc::TValue result;

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
                result["params_param[13]"] = ToString(item->GetOriginalSpecification().offer_params().param()[13]);
                result["params_param[14]"] = ToString(item->GetOriginalSpecification().offer_params().param()[14]);
            }

            return result;
        },
        GetDefaultBlueFeedInfo(EFeedType::YML)
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
