#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/blue/csv/feed_parser.h>

#include <library/cpp/string_utils/tskv_format/tskv_map.h>
#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

#include <util/stream/file.h>
#include <util/string/join.h>
#include <util/system/fs.h>

using namespace NMarket;


static const TString INPUT_CSV(R"wrap(shop-sku;price;oldprice;dimensions;weight;vat;disabled;vendor;name;count
    csv-tracelog-1;1;1;1.1/1.1/1.1;1.1;VAT_10;false;First Vendor;odin;1
    csv-tracelog-2;2;1;2.2/2.2/2.2;2.2;VAT_18;false;Second Vendor;dva;2
    csv-tracelog-3;;1;3.3/3.3/3.3;3.3;VAT_18;false;Third Vendor;tri;0
    csv-tracelog-4;4;1;4/4/4;4;VAT_18;false;Fourth Vendor;chetire;qwe
)wrap");


// csv-tracelog-3 - is disabled by error (offer without price)
static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "csv-tracelog-1",
        "Price": 1,
        "OldPrice": "1",
        "Dimensions": "11000,11000,11000",
        "Weight": 1100000,
        "Vat": "2",
        "IsDisabled": 0,
        "IsValid": 1,
        "Vendor": "First Vendor",
        "Name": "odin",
        "FeedStockCount": "1"
    },
    {
        "OfferId": "csv-tracelog-2",
        "Price": 2,
        "OldPrice": "1",
        "Dimensions": "22000,22000,22000",
        "Weight": 2200000,
        "Vat": "1",
        "IsValid": 1,
        "IsDisabled": 0,
        "Vendor": "Second Vendor",
        "Name": "dva",
        "FeedStockCount": "2"
    },
    {
        "OfferId": "csv-tracelog-3",
        "OldPrice": "1",
        "Dimensions": "33000,33000,33000",
        "Weight": 3300000,
        "Vat": "1",
        "IsValid": 1,
        "IsDisabled": 1,
        "Vendor": "Third Vendor",
        "Name": "tri",
        "FeedStockCount": "0"
    },
    {
        "OfferId": "csv-tracelog-4",
        "Price": 4,
        "OldPrice": "1",
        "Dimensions": "40000,40000,40000",
        "Weight": 4000000,
        "Vat": "1",
        "IsValid": 1,
        "IsDisabled": 0,
        "Vendor": "Fourth Vendor",
        "Name": "chetire",
        "FeedStockCount": "0"
    }
]
)wrap");


TEST(BlueCsvParser, Tracelog) {

    /*
     *  Тест проверяет trace-log файл на наличие всех ошибок, которые ожидаем поймать в тестовом фиде;
     *  Сначла запускаем парсер с включенным Tracelog и на вход даем тестовый фид;
     *  Проверяем, что результат парсинга верный;
     *  Проверяем, что на после работы парсера есть trace-log файл;
     *  Смотрим, чтобы данные внутри файла были ожидаемыми;
     */

    auto feedInfo = GetDefaultBlueFeedInfo(EFeedType::CSV);
    static constexpr char TRACELOG_FILENAME[] = "offers-tracelog.log";

    const auto actual = RunFeedParserWithTrace<NBlue::TCsvFeedParser>(
            INPUT_CSV,
            [](const TQueueItem& item) {
                NSc::TValue result;
                result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
                if (item->RawPrice) {
                    result["Price"] = *item->RawPrice;
                }
                result["OldPrice"] = ToString(item->RawOldPrice);

                if (item->DataCampOffer.content().partner().original().has_dimensions()) {
                    const auto& dimensions = item->DataCampOffer.content().partner().original().dimensions();
                    result["Dimensions"] = JoinSeq(
                        ",", {dimensions.length_mkm(), dimensions.width_mkm(), dimensions.height_mkm()});
                } else {
                    result["Dimensions"] = ToString(Nothing());
                }

                result["Weight"] = item->DataCampOffer.content().partner().original().weight().value_mg();
                result["Vat"] = ToString(item->DataCampOffer.price().basic().vat());
                result["IsDisabled"] = item->IsDisabled;
                result["Vendor"] = item->DataCampOffer.content().partner().original().vendor().value();
                result["Name"] = item->DataCampOffer.content().partner().original().name().value();
                result["IsValid"] = item->IsValid;
                if (item->IsValid) {
                    result["FeedStockCount"] = ToString(item->DataCampOffer.stock_info().partner_stocks().count());
                }

                return result;
            },
            feedInfo,
            TRACELOG_FILENAME
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);

    ASSERT_TRUE(NFs::Exists(TRACELOG_FILENAME));

    TFileInput in(TRACELOG_FILENAME);

    TVector<THashMap<TString, TString>> maps;
    maps.reserve(2);

    static TString FORMAT = "tskv";
    TString line;
    while (in.ReadLine(line)) {
        // Каждая строка в trace-логе начинается с "tskv" - противоречние tskv-формату
        // поэтому просто будем вырезать эту часть
        ASSERT_EQ(line.find_first_of(FORMAT), 0);
        THashMap<TString, TString> value;
        NTskvFormat::DeserializeMap(line.substr(FORMAT.size() + 1), value);
        maps.push_back(std::move(value));
    }

    ASSERT_EQ(maps.size(), 2);

    const auto first = std::find_if(std::begin(maps), std::end(maps), [](const auto& map) {
        return map.at("request_method") == NMarket::NOfferError::OE451_NO_PRICE.GetMessage();
    });

    ASSERT_NE(first, std::end(maps));
    ASSERT_EQ((*first)["http_code"], "200");
    ASSERT_EQ((*first)["error_code"], "400");
    ASSERT_EQ((*first)["kv.offer_id"], "csv-tracelog-3");
    ASSERT_EQ((*first)["kv.feed_id"], "1500");

    const auto second = std::find_if(std::begin(maps), std::end(maps), [](const auto& map) {
        return map.at("request_method") == NMarket::NOfferError::OW358_INVALID_VALUE_FOR_TAG.GetMessage();
    });

    ASSERT_NE(second, std::end(maps));
    ASSERT_EQ((*second)["http_code"], "404");
    ASSERT_EQ((*second)["error_code"], "");
    ASSERT_EQ((*second)["kv.offer_id"], "csv-tracelog-4");
    ASSERT_EQ((*second)["kv.feed_id"], "1500");
}
