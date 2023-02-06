#include <extsearch/images/robot/parsers/microdata/metaparser/parser/parse.h>
#include <extsearch/images/robot/parsers/microdata/metaparser/protos/metadata.pb.h>
#include <extsearch/images/robot/parsers/microdata/library/ut_functions.h>

#include <yweb/structhtml/semweb/tree/output_json.h>

#include <library/cpp/charset/doccodes.h>
#include <library/cpp/html/face/propface.h>
#include <library/cpp/html/html5/parse.h>
#include <library/cpp/html/zoneconf/ht_conf.h>
#include <library/cpp/langs/langs.h>
#include <library/cpp/numerator/blob/numeratorevents.h>
#include <library/cpp/numerator/blob/numserializer.h>
#include <library/cpp/protobuf/json/json2proto.h>
#include <library/cpp/protobuf/json/proto2json.h>
#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

#include <google/protobuf/util/message_differencer.h>

#include <util/generic/buffer.h>
#include <util/generic/string.h>
#include <util/stream/file.h>

#include <util/folder/path.h>
#include <util/system/env.h>
#include <util/system/fs.h>

#include <util/system/env.h>

template <typename T>
bool IsEqualProtos(const T& proto1, const T& proto2) {
    google::protobuf::util::MessageDifferencer differencer;
    return differencer.Equals(proto1, proto2);
}

constexpr auto TEST_DATA_PATH = "extsearch/images/robot/parsers/microdata/metaparser/ut/htmls";

TString GetCanonicalFilePath(const TString& suffix) {
    TFsPath path(ArcadiaSourceRoot());
    path /= TEST_DATA_PATH;
    path /= suffix;
    return path.GetPath();
}

void Test(const TString url, const TString htmlPath, const TString canonJsonFileName) {
    bool canonize = GetEnv("CANONIZE") == "Y";

    auto buffer = NImages::ComposeNumeratorEvents(url, GetCanonicalFilePath(htmlPath), CODES_UTF8);
    TNumeratorEvents events(buffer);

    const auto parsedPage = NImages::NMetaParser::ParseLinksFromMeta(events);

    if (canonize) {
        TUnbufferedFileOutput output(GetCanonicalFilePath(canonJsonFileName));

        NProtobufJson::Proto2Json(parsedPage, output);
    } else {
        TFileInput input(GetCanonicalFilePath(canonJsonFileName));
        NImages::NMetaParser::TLinksFromMeta canonizedPage = NProtobufJson::Json2Proto<NImages::NMetaParser::TLinksFromMeta>(input);

        UNIT_ASSERT(IsEqualProtos(parsedPage, canonizedPage));
    };
}

Y_UNIT_TEST_SUITE(TParseMetaTest) {

    Y_UNIT_TEST(Avito1) {
        const TString url = "https://www.avito.ru/moskva/odezhda_obuv_aksessuary/palto_na_izosofte_-_teploe_i_legkoe_1086972803";
        const TString htmlPath = "avito1.html";
        const TString canonizedResultPath = "avito1.json";

        Test(url, htmlPath, canonizedResultPath);
    }

    Y_UNIT_TEST(Avito2) {
        const TString url = "https://www.avito.ru/moskva/bytovaya_tehnika/chaynik_1215897335";
        const TString htmlPath = "avito2.html";
        const TString canonizedResultPath = "avito2.json";

        Test(url, htmlPath, canonizedResultPath);
    }

    Y_UNIT_TEST(Avito3) {
        const TString url = "https://www.avito.ru/moskva/gruzoviki_i_spetstehnika/traktor_dt-75_rrs2_vgtz_2002_goda_1087962387";
        const TString htmlPath = "avito3.html";
        const TString canonizedResultPath = "avito3.json";

        Test(url, htmlPath, canonizedResultPath);
    }

    Y_UNIT_TEST(Lamoda1) {
        const TString url = "https://www.lamoda.ru/p/je008ewxpi31/clothes-jennyfer-dzhinsy/";
        const TString htmlPath = "lamoda1.html";
        const TString canonizedResultPath = "lamoda1.json";

        Test(url, htmlPath, canonizedResultPath);
    }

    Y_UNIT_TEST(Youla1) {
        const TString url = "https://youla.io/moskva/muzhskaya-odezhda/obuv/anghliiskiie-botinki-drmartens-orighinal-581e57769a64a21d6cc91b32";
        const TString htmlPath = "youla1.html";
        const TString canonizedResultPath = "youla1.json";

        Test(url, htmlPath, canonizedResultPath);
    }

    Y_UNIT_TEST(Pinterest1) {
        const TString url = "https://ru.pinterest.com/pin/837388124435096044/";
        const TString htmlPath = "pinterest1.html";
        const TString canonizedResultPath = "pinterest1.json";

        Test(url, htmlPath, canonizedResultPath);
    }

    Y_UNIT_TEST(Instagram1) {
        const TString url = "https://www.instagram.com/p/Bcxhu2dhho-/?taken-by=anl.official";
        const TString htmlPath = "instagram1.html";
        const TString canonizedResultPath = "instagram1.json";

        Test(url, htmlPath, canonizedResultPath);
    }

}
