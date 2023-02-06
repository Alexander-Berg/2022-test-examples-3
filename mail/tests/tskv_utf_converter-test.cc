#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <yplatform/tskv/detail/utf_converter.h>

namespace {

using namespace testing;

namespace tskv = yplatform::tskv;

struct UtfConverterTest : public Test {
    std::ostringstream s;
    tskv::detail::utf_converter convert;
};

TEST_F(UtfConverterTest, escapeEmptyString_returnsEmptyString) {
    s.imbue(std::locale("en_US.UTF-8"));
    convert(s, std::string());
    EXPECT_TRUE(s.str().empty());
}

TEST_F(UtfConverterTest, escapeOneByteString_returnsAsIs) {
    s.imbue(std::locale("en_US.UTF-8"));
    convert(s, "abc");
    EXPECT_EQ(s.str(), "abc");
}

TEST_F(UtfConverterTest, escapeUnicodePrintableCharacters_returnsAsIs) {
    s.imbue(std::locale("en_US.UTF-8"));
    convert(s, "測試");
    EXPECT_EQ(s.str(), "測試");
}

TEST_F(UtfConverterTest, escapeUnicodePrintableCharacters_withCLocale_returnsEscaped) {
    s.imbue(std::locale("C"));
    convert(s, "測試");
    EXPECT_EQ(s.str(), R"(\u6e2c\u8a66)");
}

TEST_F(UtfConverterTest, escapeSpaceSymbols_returnsEscapedString) {
    s.imbue(std::locale("en_US.UTF-8"));
    convert(s, "\r\t\n");
    EXPECT_EQ(s.str(), "\\r\\t\\n");
}

TEST_F(UtfConverterTest, escapeOneByteNonPrintableCharacters_returnsEscapedString) {
    s.imbue(std::locale("en_US.UTF-8"));
    convert(s, "\x01\x02\x03");
    EXPECT_EQ(s.str(), "\\x01\\x02\\x03");
}

TEST_F(UtfConverterTest, escapeTwoBytesNonPrintableCharacters_returnsEscapedString) {
    s.imbue(std::locale("en_US.UTF-8"));
    convert(s, "\u4dbf");
    EXPECT_EQ(s.str(), "\\u4dbf");
}

TEST_F(UtfConverterTest, escapeFourBytesNonPrintableCharacters_returnsEscapedString) {
    s.imbue(std::locale("en_US.UTF-8"));
    convert(s, "\U000338af");
    EXPECT_EQ(s.str(), "\\U000338af");
}

TEST_F(UtfConverterTest, escapeMixedNonPrintableCharacters_returnsEscapedString) {
    s.imbue(std::locale("en_US.UTF-8"));
    convert(s, "\x01\u4dbf\U000338af");
    EXPECT_EQ(s.str(), "\\x01\\u4dbf\\U000338af");
}

TEST_F(UtfConverterTest, backslash_returnsEscapedBackslash) {
    s.imbue(std::locale("en_US.UTF-8"));
    convert(s, "\\");
    EXPECT_EQ(s.str(), "\\\\");
}

}
