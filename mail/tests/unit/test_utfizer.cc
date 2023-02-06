#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <butil/StrUtils/Iconv.h>

#include <internal/recognizer/UTFizer.h>

#include <library/cpp/testing/unittest/tests_data.h>

namespace {

using namespace testing;
using namespace Recognizer;
using namespace settings::utfizer;

const std::string source = "Съешь ещё этих мягких французских булок, да выпей [же] чаю. 1234567890.";

std::string convertTo ( const std::string &encoding ) {
    std::string dest;
    if (Iconv::recode("utf-8",encoding,source,dest,true)) {
        return dest;
    }
    return dest;
}

class UTFizerTest: public Test {
public:
    UTFizerTest()
        : languageDict(GetWorkPath() + "/queryrec.dict")
        , languageWeights(GetWorkPath() + "/queryrec.weights")
        , encodingDict(GetWorkPath() + "/dict.dict")
        , recognizer(languageDict.data(), languageWeights.data(), encodingDict.data())
        , utfizer(recognizer)
    {}
    const TString languageDict;
    const TString languageWeights;
    const TString encodingDict;
    TWebmailRecognizer recognizer;
    UTFizer utfizer;
};

TEST_F(UTFizerTest, process_UtfToUtfConverted_ReturnsUtfString) {
    std::string src = source;
    ASSERT_EQ(source, utfizer.utfize(src));
}

TEST_F(UTFizerTest, process_Cp1251ToUtf_ReturnsUtfString) {
    std::string src = convertTo("cp1251");
    ASSERT_EQ(source, utfizer.utfize(src));
}

TEST_F(UTFizerTest, process_koi8rToUtf_ReturnsUtfString) {
    std::string src = convertTo("koi8-r");
    ASSERT_EQ(source, utfizer.utfize(src));
}

TEST_F(UTFizerTest, process_ibm866ToUtf_ReturnsUtfString) {
    std::string src = convertTo("ibm866");
    ASSERT_EQ(source, utfizer.utfize(src));
}

TEST_F(UTFizerTest, process_macCyrToUtf_ReturnsUtfString) {
    std::string src = convertTo("mac-cyrillic");
    ASSERT_EQ(source, utfizer.utfize(src));
}

TEST_F(UTFizerTest, process_iso88595ToUtf_ReturnsUtfString) {
    std::string src = convertTo("iso-8859-5");
    ASSERT_EQ(source, utfizer.utfize(src));
}

TEST_F(UTFizerTest, for_turkish_signature_should_return_text_traits_contained_turkish_language) {
    ASSERT_EQ(
        (std::make_pair<std::int32_t, std::int32_t>(CODES_ASCII, LANG_TUR)),
        utfizer.recognize("Merhaba, benim")
    );
}

TEST_F(UTFizerTest, for_signature_with_unknown_language_should_return_text_traits_contained_unknown_language) {
    ASSERT_EQ(
        (std::make_pair<std::int32_t, std::int32_t>(CODES_UTF8, LANG_UNK)),
        utfizer.recognize("цwю")
    );
}

}
