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
      <offer id="409282" type="artist.title" available="true">
        <param name="Параметр 1" unit="Юнит 1">Значение 1</param>
        <artist>R. Astley</artist>
        <director>R. B. Weide</director>
        <media>blu-ray disc</media>
        <originalName>In Bruges</originalName>
        <starring>Р. Бетров, А. Поширов</starring>
        <name>Это не должно быть в name</name>
        <title>Это должно быть в name</title>
        <country>Великобритания</country>
        <barcode>12345</barcode>
        <year>2020</year>
        <age unit="year">6</age>
        <param name="Параметр 2">Значение 2</param>
      </offer>
    </offers>
  </shop>
</yml_catalog>)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "name" : "Это должно быть в name",
        "title_prefix" : "R. Astley",
        "params_param[0]" : "{ name: \"Параметр 1\" unit: \"Юнит 1\" value: \"Значение 1\" }",
        "params_param[1]" : "{ name: \"artist\" value: \"R. Astley\" }",
        "params_param[2]" : "{ name: \"director\" value: \"R. B. Weide\" }",
        "params_param[3]" : "{ name: \"media\" value: \"blu-ray disc\" }",
        "params_param[4]" : "{ name: \"originalName\" value: \"In Bruges\" }",
        "params_param[5]" : "{ name: \"starring\" value: \"Р. Бетров, А. Поширов\" }",
        "params_param[6]" : "{ name: \"country\" value: \"Великобритания\" }",
        "params_param[7]" : "{ name: \"year\" value: \"2020\" }",
        "params_param[8]" : "{ name: \"Параметр 2\" unit: \"\" value: \"Значение 2\" }",
    },
]
)wrap");


TEST(WhiteYmlParser, ArtistTitleAll) {
    const auto actual = RunWhiteYmlFeedParser<TYmlFeedParser>(
        INPUT_XML,
        [](const TQueueItem& item) {
            NSc::TValue result;

            result["name"] = item->GetOriginalSpecification().name().value();
            result["title_prefix"] = *item->TitlePrefixForSpecificTypes.Get();
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
            }

            return result;
        },
        GetDefaultBlueFeedInfo(EFeedType::YML)
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
