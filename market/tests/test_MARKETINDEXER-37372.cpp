#include <market/idx/feeds/qparser/tests/test_utils.h>
#include <market/idx/feeds/qparser/tests/blue_yml_test_runner.h>
#include <market/idx/feeds/qparser/tests/white_yml_test_runner.h>

#include <market/idx/feeds/qparser/src/feed_parsers/white/yml/feed_parser.h>
#include <market/idx/feeds/qparser/src/feed_parsers/blue/yml/feed_parser.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

#include <util/generic/maybe.h>
#include <util/folder/path.h>
#include <util/stream/file.h>

#include <google/protobuf/text_format.h>

using namespace NMarket;


namespace {

const auto fname = JoinFsPaths(
    ArcadiaSourceRoot(),
    "market/idx/feeds/qparser/tests/data/MARKETINDEXER-37372-feed.xml"
);
auto fStream = TUnbufferedFileInput(fname);
const auto input_xml = fStream.ReadAll();

}

static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "0",
        "Name": "Пика RACO для акустической гитары.",
        "Description": "⭐️ Акварельные краски полностью сохранять все для зарядки с механическим повреждениям и может незначительно отличаться от природы и СНГ; Доставляем запчасти по Стеллаж 300 125 250 Вт. Характеристики: Материал нежный трикотаж Aloe Vera воздействует на сайте (например, оттенки неба. Smart Spring», созданный для чертёжных работ. Каждый видеоинструктаж включает в верхней частью. Товар имеет размер 4,4 тыс. циклов Эл. сопротивление изоляции, не могут НЕ НЕСУ, В комплекте: 1 шт. простыня на резинках . Процедура окрашивания уже отдали своё предпочтение определенной марке, добавить данный товар армируется стальными лентами. Элементы состава составляет не входят также можно на время года. Материал: полиэтилен Крепление проводится",
        "Picture": "glassmoon.ru/store/static/images/218f0bce454c9a00.jpg",
    },
]
)wrap");


TEST(WhiteYmlParser, MARKETINDEXER_37372) {
    const auto actual = RunWhiteYmlFeedParser<TYmlFeedParser>(
        input_xml,
        [](const TQueueItem& item) {
            if (item->DataCampOffer.identifiers().offer_id() != "0") {
                return TMaybe<NSc::TValue>{};
            }
            TString s;
            google::protobuf::TextFormat::PrintToString(item->DataCampOffer, &s);
            Cerr << "DataCampOffer: " << s << Endl;

            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            result["Name"] = item->DataCampOffer.content().partner().original().name().value();
            result["Description"] = item->DataCampOffer.content().partner().original().description().value();
            result["Picture"] = item->DataCampOffer.pictures().partner().original().source()[0].url();
            return TMaybe<NSc::TValue>{result};
        },
        GetDefaultWhiteFeedInfo(EFeedType::YML)
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}


TEST(BlueYmlParser, MARKETINDEXER_37372) {
    const auto actual = RunBlueYmlFeedParser<NBlue::TYmlFeedParser>(
        input_xml,
        [](const TQueueItem& item) {
            if (item->DataCampOffer.identifiers().offer_id() != "0") {
                return TMaybe<NSc::TValue>{};
            }
            TString s;
            google::protobuf::TextFormat::PrintToString(item->DataCampOffer, &s);
            Cerr << "DataCampOffer: " << s << Endl;

            NSc::TValue result;
            result["Name"] = item->DataCampOffer.content().partner().original().name().value();
            result["Description"] = item->DataCampOffer.content().partner().original().description().value();
            result["Picture"] = item->DataCampOffer.pictures().partner().original().source()[0].url();
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            return TMaybe<NSc::TValue>{result};
        },
        GetDefaultBlueFeedInfo(EFeedType::YML)
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
