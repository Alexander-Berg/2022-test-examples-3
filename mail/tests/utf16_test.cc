
#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <internal/utf16.h>


namespace {

using namespace testing;

TEST(Utf16Test, isValidUtf16_validUtf16_true) {
    std::string str;
    str.push_back(static_cast<char>(0xff));
    str.push_back(static_cast<char>(0xfe));
    str.push_back(0x00);
    str.push_back('a');
    EXPECT_TRUE(isValidUtf16(str));
}

TEST(Utf16Test, isValidUtf16_sizeLess2_false) {
    std::string str;
    str.push_back(static_cast<char>(0xff));
    EXPECT_FALSE(isValidUtf16(str));
}

TEST(Utf16Test, isValidUtf16_sizeNotDivisibleBy2_false) {
    std::string str;
    str.push_back(static_cast<char>(0xff));
    str.push_back(static_cast<char>(0xfe));
    str.push_back(0x00);
    str.push_back('a');
    str.push_back(0x00);
    EXPECT_FALSE(isValidUtf16(str));
}

TEST(Utf16Test, isValidUtf16_withoutBom_false) {
    const std::string str("abcd");
    EXPECT_FALSE(isValidUtf16(str));
}

TEST(Utf16Test, isValidUtf16_highSurrogateWithoutLow_false) {
    std::string str;
    str.push_back(static_cast<char>(0xff));
    str.push_back(static_cast<char>(0xfe));
    str.push_back(0x00);
    str.push_back('a');
    str.push_back(static_cast<char>(0xd8));
    str.push_back(0x00);
    str.push_back(0x00);
    str.push_back('b');
    str.push_back(0x00);
    str.push_back('c');
    EXPECT_FALSE(isValidUtf16(str));
}

TEST(Utf16Test, isValidUtf16_highSurrogateLast_false) {
    std::string str;
    str.push_back(static_cast<char>(0xff));
    str.push_back(static_cast<char>(0xfe));
    str.push_back(0x00);
    str.push_back('a');
    str.push_back(static_cast<char>(0xd8));
    str.push_back(0x00);
    EXPECT_FALSE(isValidUtf16(str));
}

TEST(Utf16Test, isValidUtf16_lowSurrogateWithoutHigh_false) {
    std::string str;
    str.push_back(static_cast<char>(0xff));
    str.push_back(static_cast<char>(0xfe));
    str.push_back(0x00);
    str.push_back('a');
    str.push_back(0x00);
    str.push_back('b');
    str.push_back(static_cast<char>(0xdc));
    str.push_back(0x00);
    str.push_back(0x00);
    str.push_back('c');
    EXPECT_FALSE(isValidUtf16(str));
}

TEST(Utf16Test, isValidUtf16_lowSurrogateAfterBom_false) {
    std::string str;
    str.push_back(static_cast<char>(0xff));
    str.push_back(static_cast<char>(0xfe));
    str.push_back(static_cast<char>(0xdc));
    str.push_back(0x00);
    str.push_back(0x00);
    str.push_back('c');
    EXPECT_FALSE(isValidUtf16(str));
}

TEST(Utf16Test, isValidUtf16_validUtf16ButJunkAfterString_true) {
    std::string str;
    str.reserve(8);
    str.push_back(static_cast<char>(0xff));
    str.push_back(static_cast<char>(0xfe));
    str.push_back(0x00);
    str.push_back('a');

    //Adding a junk after string's buffer - high surrogate.
    //This bytes should not affect validating.
    const_cast<char*>(str.data())[4] = static_cast<char>(0xd8);
    const_cast<char*>(str.data())[5] = 0x00;
    EXPECT_TRUE(isValidUtf16(str));
}

TEST(Utf16Test, isValidUtf16_noLowSurrogateInStringButInJunkAfter_false) {
    std::string str;
    str.reserve(8);
    str.push_back(static_cast<char>(0xff));
    str.push_back(static_cast<char>(0xfe));
    str.push_back(static_cast<char>(0xd8));
    str.push_back(0x00);

    //Adding a junk after string's buffer - low surrogate.
    //This bytes should not affect validating.
    const_cast<char*>(str.data())[4] = static_cast<char>(0xdc);
    const_cast<char*>(str.data())[5] = 0x00;
    EXPECT_FALSE(isValidUtf16(str));
}

}
