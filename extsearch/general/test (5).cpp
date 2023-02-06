#include <extsearch/images/kernel/protos/schemaorg_product.pb.h>

#include <extsearch/images/robot/parsers/microdata/schemaorg/scripts_parser/schemaorg_parser.h>
#include <extsearch/images/robot/parsers/library/ut_functions.h>

#include <google/protobuf/util/message_differencer.h>

#include <library/cpp/numerator/blob/numeratorevents.h>
#include <library/cpp/numerator/blob/numserializer.h>
#include <library/cpp/protobuf/json/json2proto.h>
#include <library/cpp/protobuf/json/proto2json.h>
#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

#include <yweb/structhtml/semweb/tree/output_json.h>

template <typename T>
bool IsEqualProtos(const T& proto1, const T& proto2) {
    google::protobuf::util::MessageDifferencer differencer;
    return differencer.Equals(proto1, proto2);
}

constexpr auto TEST_DATA_PATH = "htmls";

TString GetCanonicalFilePath(const TString& suffix) {
    TFsPath path(GetWorkPath());
    path /= TEST_DATA_PATH;
    path /= suffix;
    return path.GetPath();
}

void Test(const TString url, const TString htmlPath, const TString canonJsonFileName, ECharset charset) {
    auto buffer = NImages::ComposeNumeratorEvents(url, GetCanonicalFilePath(htmlPath), charset);
    TNumeratorEvents numeratorEvents(buffer);
    NImageLib::TSchemaOrgParser::TPtr imgHtmlParser = new NImageLib::TSchemaOrgParser(url);

    imgHtmlParser->SetPageEncoding(CODES_UTF8);

    TNumeratorHandlers numerators;
    numerators.AddHandler(imgHtmlParser.Get());

    try {
        numeratorEvents.Numerate(numerators);
    } catch (const yexception& e) {
        ythrow yexception() << "Failed to parse page " << url << ": " << e.what();
    }

    NImages::NSchemaOrg::TProducts parsedProducts = imgHtmlParser->GetProducts();
    NProtobufJson::Proto2Json(parsedProducts, Cout);

    TFileInput input(GetCanonicalFilePath(canonJsonFileName));
    NImages::NSchemaOrg::TProducts products = NProtobufJson::Json2Proto<NImages::NSchemaOrg::TProducts>(input);

    UNIT_ASSERT(IsEqualProtos(parsedProducts, products));
}

Y_UNIT_TEST_SUITE(TParseSchemaOrgFromScripts) {
    Y_UNIT_TEST(HM1) {
        const TString url = "https://www2.hm.com/ru_ru/productpage.0788696003.html";
        const TString htmlPath = "hm1.html";
        const TString canonizedResultPath = "hm1.json";

        Test(url, htmlPath, canonizedResultPath, CODES_WIN);
    }

    Y_UNIT_TEST(IKEA1) {
        const TString url = "https://www.ikea.com/ru/ru/p/kleppstad-garderob-2-dvernyy-belyy-50437235/";
        const TString htmlPath = "ikea1.html";
        const TString canonizedResultPath = "ikea1.json";

        Test(url, htmlPath, canonizedResultPath, CODES_WIN);
    }

    Y_UNIT_TEST(DETMIR1) {
        const TString url = "https://www.detmir.ru/product/index/id/3221728/";
        const TString htmlPath = "detmir1.html";
        const TString canonizedResultPath = "detmir1.json";

        Test(url, htmlPath, canonizedResultPath, CODES_WIN);
    }

    Y_UNIT_TEST(ZARA1) {
        const TString url = "https://www.zara.com/ru/ru/кожаные-сандалии-с-ремешками-p12740520.html?v1=51037153&v2=1546863";
        const TString htmlPath = "zara1.html";
        const TString canonizedResultPath = "zara1.json";

        Test(url, htmlPath, canonizedResultPath, CODES_WIN);
    }

    Y_UNIT_TEST(MASSIMODUTTI1) {
        const TString url = "https://www.massimodutti.com/ru/мужчины/новинки/новые-поступления/легкая-темно-синяя-тренчкот-c1607006p9004063.html?colorId=401&categoryId=1607006";
        const TString htmlPath = "massimodutti1.html";
        const TString canonizedResultPath = "massimodutti1.json";

        Test(url, htmlPath, canonizedResultPath, CODES_WIN);
    }

    Y_UNIT_TEST(ADIDAS1) {
        const TString url = "https://www.adidas.ru/krossovki-superstar/FV3284.html";
        const TString htmlPath = "adidas1.html";
        const TString canonizedResultPath = "adidas1.json";

        Test(url, htmlPath, canonizedResultPath, CODES_WIN);
    }

    Y_UNIT_TEST(SVYAZNOY1) {
        const TString url = "https://www.svyaznoy.ru/catalog/accessories/8936/4852784";
        const TString htmlPath = "svyaznoy1.html";
        const TString canonizedResultPath = "svyaznoy1.json";

        Test(url, htmlPath, canonizedResultPath, CODES_WIN);
    }

    Y_UNIT_TEST(CITILINK1) {
        const TString url = "https://www.citilink.ru/catalog/computers_and_notebooks/accums/415119/";
        const TString htmlPath = "citilink1.html";
        const TString canonizedResultPath = "citilink1.json";

        Test(url, htmlPath, canonizedResultPath, CODES_WIN);
    }

    Y_UNIT_TEST(BESTMEBELSHOP1) {
        const TString url = "https://www.bestmebelshop.ru/catalog/komod/komod-mikhail-lyuks-23/";
        const TString htmlPath = "bestmebelshop1.html";
        const TString canonizedResultPath = "bestmebelshop1.json";

        Test(url, htmlPath, canonizedResultPath, CODES_WIN);
    }

    Y_UNIT_TEST(IKEA2) {
        const TString url = "https://www.ikea.com/ru/ru/p/svensta-2-mestnyy-divan-krovat-chernyy-20446161/";
        const TString htmlPath = "ikea2.html";
        const TString canonizedResultPath = "ikea2.json";

        Test(url, htmlPath, canonizedResultPath, CODES_WIN);
    }

    Y_UNIT_TEST(SANTEHNIKAONLINE1) {
        const TString url = "santehnikaonline1.html";
        const TString htmlPath = "santehnikaonline1.html";
        const TString canonizedResultPath = "santehnikaonline1.json";

        Test(url, htmlPath, canonizedResultPath, CODES_WIN);
    }

    Y_UNIT_TEST(CALVINKLEIN1) {
        const TString url = "https://www.calvinklein.ru/джинсы-slim-j30j3155671by";
        const TString htmlPath = "calvinklein1.html";
        const TString canonizedResultPath = "calvinklein1.json";

        Test(url, htmlPath, canonizedResultPath, CODES_WIN);
    }

    Y_UNIT_TEST(NULLPRICE1) {
        const TString url = "https://orenburg.poshk.ru/catalog/specshina/standart_st-2000_premium_6_50-10_5_00_/";
        const TString htmlPath = "null_price1.html";
        const TString canonizedResultPath = "null_price1.json";

        Test(url, htmlPath, canonizedResultPath, CODES_WIN);
    }

    Y_UNIT_TEST(IMAGEOBJECT1) {
        const TString url = "https://www.cmlt.ru/ad-b6752448";
        const TString htmlPath = "image_object1.html";
        const TString canonizedResultPath = "image_object1.json";

        Test(url, htmlPath, canonizedResultPath, CODES_WIN);
    }

    Y_UNIT_TEST(AGGREGATEOFFER1) {
        const TString url = "https://russian.alibaba.com/product-detail/2018-trendy-men-s-stripe-jeans-light-blue-washed-skinny-distressed-jeans-trouser-60717413430.html";
        const TString htmlPath = "aggregate_offer1.html";
        const TString canonizedResultPath = "aggregate_offer1.json";

        Test(url, htmlPath, canonizedResultPath, CODES_WIN);
    }

    Y_UNIT_TEST(OSTIN1) {
        const TString url = "https://ostin.com/product/uzkie-dzhinsy-s-neobrabotannym-nizom/23488580299/";
        const TString htmlPath = "ostin1.html";
        const TString canonizedResultPath = "ostin1.json";

        Test(url, htmlPath, canonizedResultPath, CODES_WIN);
    }

    Y_UNIT_TEST(LORDMEBEL1) {
        const TString url = "https://lordmebel.com.ua/product/baston/";
        const TString htmlPath = "lordmebel1.html";
        const TString canonizedResultPath = "lordmebel1.json";

        Test(url, htmlPath, canonizedResultPath, CODES_WIN);
    }

    Y_UNIT_TEST(YANDEX_ADV1) {
        const TString url = "https://o.yandex.ru/tovari-dlya-doma/offer/45889320583159808/";
        const TString htmlPath = "yandex_ads1.html";
        const TString canonizedResultPath = "yandex_ads1.json";

        Test(url, htmlPath, canonizedResultPath, CODES_WIN);
    }

    Y_UNIT_TEST(YANDEX_ADV2) {
        const TString url = "https://o.yandex.ru/bitovaya-tehnika/offer/47254701861515264/";
        const TString htmlPath = "yandex_ads2.html";
        const TString canonizedResultPath = "yandex_ads2.json";

        Test(url, htmlPath, canonizedResultPath, CODES_WIN);
    }
}

