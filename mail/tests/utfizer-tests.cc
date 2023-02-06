#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <butil/StrUtils/Iconv.h>
#include <mail_getter/UTFizer.h>

#include "recognizer_instance.h"

namespace {

    using namespace testing;

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
    UTFizerTest() : utfizer(getRecognizer()) {}
    UTFizer utfizer;
};

TEST_F(UTFizerTest, process_UtfToUtfConverted_ReturnsUtfString) {
    std::string src = source;
    utfizer.utfize("",src);
    EXPECT_EQ(source, src);
}

TEST_F(UTFizerTest, process_MAILSUPPORT_856_ReturnsUtfString) {
    std::string src = "ÇELİK Ahmet";
    utfizer.utfize("ýso-8859-9",src);
    EXPECT_EQ("ÇELİK Ahmet", src);
}

class UTFizerGenericTest : public TestWithParam<const char*> {
public:
    UTFizerGenericTest() : utfizer(getRecognizer()) {}
    UTFizer utfizer;
};

TEST_P(UTFizerGenericTest, process_ParamToUtf_ReturnsUtfString) {
    std::string src = convertTo(GetParam());
    utfizer.utfize("",src);
    EXPECT_EQ(source, src);
}

const std::initializer_list<const char*> successValues = {
    "cp1251", "koi8-r", "ibm866", "mac-cyrillic", "iso-8859-5",
    "utf-8", "cp1251", "cp1124", "cp1125", "koi8-t", "pt154",
    "rk1048", "utf-16"
#ifdef ARCADIA_BUILD
    , "cp1131"
#endif
};


INSTANTIATE_TEST_SUITE_P(UTFizerGenericTestInstance,
                        UTFizerGenericTest,
                        ValuesIn(successValues));

class UTFizerGenericFailTest : public TestWithParam<const char*> {
public:
    UTFizerGenericFailTest() : utfizer(getRecognizer()) {}
    UTFizer utfizer;
};

TEST_P(UTFizerGenericFailTest, process_ParamToUtfFail_ReturnsParamString) {
    std::string src = convertTo(GetParam());
    utfizer.utfize("",src);
    EXPECT_NE(source, src);
}

const std::initializer_list<const char*> failValues = {
    "cp1252", "cp1250", "iso-8859-2", "winwin", "koikoi", "ibm-855",
    "armscii-8", "georgian-academy", "georgian-ps", "iso-8859-3", "iso-8859-4",
    "iso-8859-6", "iso-8859-7", "iso-8859-8", "iso-8859-9", "iso-8859-13", "iso-8859-15",
    "iso-8859-16", "cp1253", "cp1254", "cp1255", "cp1256", "cp1257", "cp1046", "cp1129",
    "cp1133", "cp1161", "cp1162", "cp1163", "cp1258", "cp437", "cp737", "cp775", "cp850",
    "cp852", "cp853", "cp856", "cp857", "cp858", "cp860", "cp861", "cp862", "cp863", "cp864",
    "cp865", "cp869", "cp874", "cp922", "iso646-cn", "iso646-jp", "iso8859-10", "iso8859-11",
    "iso8859-14", "tcvn", "tis620", "viscii", "big5", "big5-hkscs", "cp932", "cp936", "cp949",
    "cp950", "euc-cn", "euc-jp", "euc-kr", "euc-tw", "gb18030", "gbk", "iso-2022-cn",
    "iso-2022-cn-ext", "iso-2022-jp", "iso-2022-jp-2", "iso-2022-kr", "johab", "shift-jis",
    "ks_c_5601-1987"
#ifndef ARCADIA_BUILD
    , "cp1131"
#endif
};

INSTANTIATE_TEST_SUITE_P(UTFizerGenericFailTestInstance,
                        UTFizerGenericFailTest,
                        ValuesIn(failValues));

}
