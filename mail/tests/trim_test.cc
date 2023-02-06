#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <internal/trim.h>

namespace  {

using namespace testing;
using namespace msg_body;

typedef Test TrimTest;

TEST(TrimTest, trimBigLetter_nullPointer_returnsFlase) {
    const unsigned treshold = 1;
    const bool res = trimBigLetter(NULL, treshold);
    ASSERT_FALSE(res);
}

TEST(TrimTest, trimBigLetter_zeroTreshold_returnsFlase) {
    const size_t len = 3;
    std::string s(len, 'a');
    const unsigned treshold = 0;
    const bool res = trimBigLetter(&s, treshold);
    ASSERT_FALSE(res);
}

TEST(TrimTest, trimBigLetter_contentLengthIsLessThanTreshold_returnsFlase) {
    const size_t len = 3;
    std::string s(len, 'a');
    const unsigned treshold = 10;
    const bool res = trimBigLetter(&s, treshold);
    ASSERT_FALSE(res);
}

TEST(TrimTest, trimBigLetter_contentWithNoSpacesNeedsTrimming_returnsTrueAndTrims) {
    const size_t len = 10;
    std::string s(len, 'a');
    const unsigned treshold = 5;
    const bool res = trimBigLetter(&s, treshold);
    ASSERT_TRUE(res);
    ASSERT_EQ(treshold, s.size());
}

TEST(TrimTest, trimBigLetter_contentWithSpacesNeesaTrimming_returnsTrueAndTrimsAndLastCharIsSpace) {
    std::string s("aaa aaaa");
    const unsigned treshold = 5;
    const bool res = trimBigLetter(&s, treshold);
    ASSERT_TRUE(res);
    ASSERT_TRUE(treshold >= s.size());
    ASSERT_EQ(' ', s[s.size()-1]);
}

} //unnamed namespace
