#include <mail/ymod_smtpserver/src/utils.h>

#include <gtest/gtest.h>

namespace {

using namespace ymod_smtpserver;
using namespace testing;

TEST(FindPossiblyIncorrectCrlfSequence, for_empty_string_should_return_false) {
    std::string str;
    EXPECT_FALSE(NUtil::FindPossiblyIncorrectCrlfSequence(str.begin(), str.end()));
}

struct TFindPossiblyIncorrectCrlfSequenceTest : public TestWithParam<std::string> {};

TEST_P(TFindPossiblyIncorrectCrlfSequenceTest, for_string_with_crlf_inside_string_should_return_correct_position) {
    std::string str;
    str += "String with";
    std::size_t crlfBegin = str.size();
    str += GetParam();
    std::size_t crlfEnd = str.size();
    str += "crlf\r\n";
    auto res = NUtil::FindPossiblyIncorrectCrlfSequence(str.begin(), str.end());
    EXPECT_TRUE(res);
    EXPECT_EQ(res->begin(), str.begin() + crlfBegin);
    EXPECT_EQ(res->end(), str.begin() + crlfEnd);
}

TEST_P(TFindPossiblyIncorrectCrlfSequenceTest, for_string_with_crlf_in_the_beginning_of_string_should_return_correct_position) {
    std::string str;
    std::size_t crlfBegin = str.size();
    str += GetParam();
    std::size_t crlfEnd = str.size();
    str += "String with";
    str += "crlf\r\n";
    auto res = NUtil::FindPossiblyIncorrectCrlfSequence(str.begin(), str.end());
    EXPECT_TRUE(res);
    EXPECT_EQ(res->begin(), str.begin() + crlfBegin);
    EXPECT_EQ(res->end(), str.begin() + crlfEnd);
}

TEST_P(TFindPossiblyIncorrectCrlfSequenceTest, for_string_with_crlf_in_the_end_of_string_should_return_correct_position) {
    std::string str;
    str += "String with crlf and something more";
    std::size_t crlfBegin = str.size();
    str += GetParam();
    std::size_t crlfEnd = str.size();
    auto res = NUtil::FindPossiblyIncorrectCrlfSequence(str.begin(), str.end());
    EXPECT_TRUE(res);
    EXPECT_EQ(res->begin(), str.begin() + crlfBegin);
    EXPECT_EQ(res->end(), str.begin() + crlfEnd);
}

INSTANTIATE_TEST_SUITE_P(FindPossiblyIncorrectCrlfSequenceTests, TFindPossiblyIncorrectCrlfSequenceTest,
    Values(
        std::string("\r\n"),
        std::string("\r"),
        std::string("\n"),
        std::string("\r\r\n"),
        std::string("\r\r"),
        std::string("\r\r\r\r"),
        std::string("\r\r\r\n")
    )
);

struct TFixCrlfTest : public TestWithParam<std::tuple<std::string, std::string>> {};

TEST_P(TFixCrlfTest, for_potentially_broken_crlf_should_fix_crlf) {
    auto [before, after] = GetParam();

    auto msgPtr = NUtil::FixCrlf(before.begin(), before.end());

    EXPECT_EQ(*msgPtr, after);
}

INSTANTIATE_TEST_SUITE_P(FixCrlfTests, TFixCrlfTest,
    Values(
        std::make_tuple(std::string{"This message\r\r\nShould be fixed\r\r\r\n"}, std::string{"This message\r\nShould be fixed\r\n"}),
        std::make_tuple(std::string{"\r\rThis message should be fixed"}, std::string{"\r\nThis message should be fixed"}),
        std::make_tuple(std::string{"\r\rThis \nmessage\r\r\nShould be fixed\r\r\r\n"}, std::string{"\r\nThis \r\nmessage\r\nShould be fixed\r\n"}),
        std::make_tuple(std::string{"\r\rThis \nmessage\r\r\nShould be fixed"}, std::string{"\r\nThis \r\nmessage\r\nShould be fixed"}),
        std::make_tuple(std::string{"\r\rThis \nmessage\r\r\nShould be fixed\r"}, std::string{"\r\nThis \r\nmessage\r\nShould be fixed\r\n"}),
        std::make_tuple(std::string{"This \r\r\n\r\nmessage\r\r\nShould be fixed\r"}, std::string{"This \r\n\r\nmessage\r\nShould be fixed\r\n"}),
        std::make_tuple(std::string{"This message\n\nShould be fixed"}, std::string{"This message\r\n\r\nShould be fixed"}),
        std::make_tuple(std::string{"This message\r\n\r\nShould be fixed"}, std::string{"This message\r\n\r\nShould be fixed"}),
        std::make_tuple(std::string{"This message should not be fixed"}, std::string{"This message should not be fixed"})
    )
);

}
