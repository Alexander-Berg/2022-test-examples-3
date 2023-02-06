#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/white/csv/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>


using namespace NMarket;


static const TString INPUT_CSV(R"wrap(type;id;shop-sku;price;artist;name;title;name;director;media;originalName;starring;country
    artist.title;csv-offer1;csv-offer1-shop-sku;500;R. Astley;Не должно быть в name 1;Должно быть в name;Не должно быть в name 2;R. B. Weide;blu-ray disc;In Bruges;Р. Бетров, А. Поширов;Великобритания
)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "name" : "Должно быть в name",
        "title_prefix" : "R. Astley",
        "params_param[00]" : "{ name: \"artist\" value: \"R. Astley\" }",
        "params_param[01]" : "{ name: \"country\" value: \"Великобритания\" }",
        "params_param[02]" : "{ name: \"director\" value: \"R. B. Weide\" }",
        "params_param[03]" : "{ name: \"media\" value: \"blu-ray disc\" }",
        "params_param[04]" : "{ name: \"originalname\" value: \"In Bruges\" }",
        "params_param[05]" : "{ name: \"starring\" value: \"Р. Бетров, А. Поширов\" }",
    },
]
)wrap");


TEST(WhiteCsvParser, ArtistTitleAll) {
const auto actual = RunFeedParserWithTrace<TCsvFeedParser>(
        INPUT_CSV,
        [](const TQueueItem& item) {
            NSc::TValue result;

            result["name"] = item->GetOriginalSpecification().name().value();
            result["title_prefix"] = *item->TitlePrefixForSpecificTypes.Get();
            if (item->GetOriginalSpecification().has_offer_params()) {
                result["params_param[00]"] = ToString(item->GetOriginalSpecification().offer_params().param()[0]);
                result["params_param[01]"] = ToString(item->GetOriginalSpecification().offer_params().param()[1]);
                result["params_param[02]"] = ToString(item->GetOriginalSpecification().offer_params().param()[2]);
                result["params_param[03]"] = ToString(item->GetOriginalSpecification().offer_params().param()[3]);
                result["params_param[04]"] = ToString(item->GetOriginalSpecification().offer_params().param()[4]);
                result["params_param[05]"] = ToString(item->GetOriginalSpecification().offer_params().param()[5]);
            }

            return result;
        },
        GetDefaultWhiteFeedInfo(EFeedType::CSV),
        "offers-trace.log"
);
const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
ASSERT_EQ(actual, expected);
}
