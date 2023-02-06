#include <extsearch/images/robot/parsers/html_parser/several_html/factory.h>

#include <extsearch/images/robot/parsers/library/ut_functions.h>

#include <google/protobuf/util/message_differencer.h>

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

#include <yweb/structhtml/semweb/tree/output_json.h>

#include <util/folder/path.h>
#include <util/generic/buffer.h>
#include <util/generic/string.h>
#include <util/stream/file.h>
#include <util/system/env.h>
#include <util/system/fs.h>

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
    bool canonize = GetEnv("CANONIZE") == "Y";

    auto buffer = NImages::ComposeNumeratorEvents(url, GetCanonicalFilePath(htmlPath), charset);
    TNumeratorEvents numeratorEvents(buffer);
    NImageLib::TImgSeveralHtmlParserPtr imgHtmlParser = NImageLib::CreateImgSeveralPagesExtractors(url);

    imgHtmlParser->SetPageEncoding(CODES_UTF8);

    TNumeratorHandlers numerators;
    numerators.AddHandler(imgHtmlParser.Get());

    try {
        numeratorEvents.Numerate(numerators);
    } catch (const yexception& e) {
        ythrow yexception() << "Failed to parse page " << url << ": " << e.what();
    }

    NImageLib::TSeveralPagesLinks severalPagesLinks = imgHtmlParser->GetPagesWithLinks();

    if (canonize) {
        TUnbufferedFileOutput output(GetCanonicalFilePath(canonJsonFileName));

        NProtobufJson::Proto2Json(severalPagesLinks, output);
    } else {
        TFileInput input(GetCanonicalFilePath(canonJsonFileName));
        NImageLib::TSeveralPagesLinks canonizedPage = NProtobufJson::Json2Proto<NImageLib::TSeveralPagesLinks>(input);

        UNIT_ASSERT(IsEqualProtos(severalPagesLinks, canonizedPage));
    };
}

Y_UNIT_TEST_SUITE(TParseMetaTest) {
    Y_UNIT_TEST(Vk1) {
        const TString url = "https://vk.com/warhammer_art_of_war";
        const TString htmlPath = "vk1.html";
        const TString canonizedResultPath = "vk1.json";

        Test(url, htmlPath, canonizedResultPath, CODES_WIN);
    }
    Y_UNIT_TEST(Fb1) {
        const TString url = "https://www.facebook.com/yandex/";
        const TString htmlPath = "fb1.html";
        const TString canonizedResultPath = "fb1.json";

        Test(url, htmlPath, canonizedResultPath, CODES_UTF8);
    }
    Y_UNIT_TEST(Tw1) {
        const TString url = "https://twitter.com/tass_agency";
        const TString htmlPath = "tw1.html";
        const TString canonizedResultPath = "tw1.json";

        Test(url, htmlPath, canonizedResultPath, CODES_UTF8);
    }
}
