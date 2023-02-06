
#include <gtest/gtest.h>
#include <butil/butil.h>
#include <mimeparser/base64.h>

namespace {

template <class Range>
size_t test_length(const Range& range) {
    using namespace mail::utils::base64;
    typedef LengthCalculator<decltype(std::begin(range))> Calculator;
    Calculator calculator;
    for (auto it = std::begin(range); it != std::end(range); ++it) {
        calculator.push(it, it+1);
    }
    calculator.stop();
    return calculator.length();
}

TEST(Base64, length)
{
    using namespace mail::utils;
    typedef std::string::const_iterator Iterator;
    ASSERT_TRUE(1==base64::calculate_length<Iterator>(std::string("YQ==")));
    ASSERT_TRUE(2==base64::calculate_length<Iterator>(std::string("YWE=")));
    ASSERT_TRUE(3==base64::calculate_length<Iterator>(std::string("YWFh")));
}

TEST(Base64, a_length)
{
    using namespace mail::utils;
    ASSERT_TRUE(1==test_length("YQ=="));
    ASSERT_TRUE(2==test_length("YWE="));
    ASSERT_TRUE(3==test_length("YWFh"));
}

TEST(Base64, length_n)
{
    using namespace mail::utils;
    typedef std::string::const_iterator Iterator;
    std::string str;
    for (size_t i=0;i<100;++i) {
        std::string b64str=encode_base64(str);
        ASSERT_TRUE(str.size()==i);
        ASSERT_TRUE(base64::calculate_length<Iterator>(b64str)==i);
        str+="a";
    }
}

TEST(Base64, a_length_n)
{
    using namespace mail::utils;
    std::string str;
    for (size_t i=0;i<100;++i) {
        std::string b64str=encode_base64(str);
        ASSERT_TRUE(str.size()==i);
        ASSERT_EQ(test_length(b64str), i);
        str+="a";
    }
}

class DecodeBase64MergePartsTest : public ::testing::TestWithParam<std::pair<std::string, std::string>> {};
INSTANTIATE_TEST_SUITE_P(test_decode_base64_mergeparts_all, DecodeBase64MergePartsTest, ::testing::Values(
    std::make_pair("YQ==",                                      "a"),
    std::make_pair("YWI=",                                      "ab"),
    std::make_pair("YWJj",                                      "abc"),
    std::make_pair("\tYQ==\n",                                  "a"),
    std::make_pair("YQ== YQ==     YQ==          YQ==",          "aaaa"),
    std::make_pair("\tY Q =\n = \n",                            "a"),
    std::make_pair("YQ=",                                       "a"),
    std::make_pair("YQ",                                        "a"),
    std::make_pair("YWJhYg==",                                  "abab"),
    std::make_pair("YWI=YWI=",                                  "abab"),
    std::make_pair("YWI=\nYWI=\n",                              "abab"),
    std::make_pair("YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXo=",      "abcdefghijklmnopqrstuvwxyz"),
    std::make_pair("YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXo=\n",    "abcdefghijklmnopqrstuvwxyz" ),
    std::make_pair("AQIDBAUK3v8=",                              "\x01\x02\x03\x04\x05\x0A\xDE\xFF"),
    std::make_pair("",                                          std::string()),
    std::make_pair("\n",                                        std::string()),
    std::make_pair(" \t\n",                                     std::string()),
    std::make_pair("=",                                         std::string()),
    std::make_pair("*",                                         std::string()),
    std::make_pair("YWJ*Yg==",                                  "abX"),
    std::make_pair("YWJh*g==",                                  "aba"),
    std::make_pair("YWJhY*==",                                  "aba"),
    std::make_pair("YWJhYg*=",                                  "abab"),
    std::make_pair("YWJhYg=*",                                  "aba"),
    std::make_pair("AA==",                                      std::string("\x00", 1)),
    std::make_pair("AAA=",                                      std::string("\x00\x00", 2)),
    std::make_pair("AAAA",                                      std::string("\x00\x00\x00", 3))
));

TEST_P(DecodeBase64MergePartsTest, test_decode_base64_mergeparts)
{
    using namespace mail::utils;

    auto [input, result] = GetParam();

    EXPECT_EQ(decode_base64_mergeparts(input), result) << "input is '" << input << "'";
}

class DecodeBase64MergePartsSameResultTest : public ::testing::TestWithParam<std::string> {};
INSTANTIATE_TEST_SUITE_P(test_decode_base64_mergeparts_same_result_all, DecodeBase64MergePartsSameResultTest, ::testing::Values(
    std::string("YQ=="),
    std::string("YWI="),
    std::string("YWJj"),
    std::string("\tYQ==\n"),
    std::string("YQ="),
    std::string("YQ"),
    std::string("YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXo="),
    std::string("YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXo=\n"),
    std::string("AQIDBAUK3v8="),
    std::string(""),
    std::string("\n"),
    std::string(" \t\n"),
    std::string("="),
    std::string("*"),
    std::string("YWJ*Yg=="),
    std::string("YWJh*g=="),
    std::string("YWJhY*=="),
    std::string("YWJhYg*="),
    std::string("YWJhYg=*"),
    std::string("AA=="),
    std::string("AAA="),
    std::string("AAAA")
));

TEST_P(DecodeBase64MergePartsSameResultTest, test_decode_base64_mergeparts_same_result_all)
{
    using namespace mail::utils;

    auto input = GetParam();

    EXPECT_EQ(decode_base64_mergeparts(input), decode_base64(input));
}
}
