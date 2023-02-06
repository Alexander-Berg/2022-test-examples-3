#include <crypta/ext_fp/matcher/lib/matchers/intentai_matcher/intentai_response_parser.h>

#include <crypta/ext_fp/matcher/lib/matchers/intentai_matcher/intentai_match.pb.h>

#include <library/cpp/testing/gtest/gtest.h>

using namespace NCrypta::NExtFp::NMatcher;

namespace {
    TMatchResult CreateMatch(const TString& id) {
        return TMatchResult{.Status = TMatchResult::EStatus::Found, .ExtId = id};
    }

    const TConnection CONN_1 = {.Ip = "11.11.11.11", .Port = 1111, .Timestamp = 1611111111u};
    const TConnection CONN_2 = {.Ip = "22.22.22.22", .Port = 2222, .Timestamp = 1622222222u};
}

TEST(NIntentaiResponseParser, ParseEmpty) {
    EXPECT_EQ(0u, NIntentaiResponseParser::Parse("").size());
    EXPECT_EQ(0u, NIntentaiResponseParser::Parse("[]").size());
    EXPECT_EQ(0u, NIntentaiResponseParser::Parse("{}").size());
}

TEST(NIntentaiResponseParser, Invalid) {
    EXPECT_THROW(NIntentaiResponseParser::Parse("[1, 2, 3]"), yexception);
    EXPECT_THROW(NIntentaiResponseParser::Parse("[{}]"), yexception);
}

TEST(NIntentaiResponseParser, OneMatch) {
    const auto& oneMatchList = NIntentaiResponseParser::Parse(R"LIST([
            {"ip": "11.11.11.11", "port": 1111, "timestamp": 1611111111, "user_id": "intentai-111"}
    ])LIST");

    EXPECT_EQ(1u, oneMatchList.size());
    EXPECT_EQ(CreateMatch("intentai-111"), oneMatchList.at(CONN_1));
}

TEST(NIntentaiResponseParser, TwoMatches) {
    const auto& twoMatchesList = NIntentaiResponseParser::Parse(R"LIST([
            {"ip": "11.11.11.11", "port": 1111, "timestamp": 1611111111, "user_id": "intentai-111"},
            {"ip": "22.22.22.22", "port": 2222, "timestamp": 1622222222, "user_id": "intentai-222"}
    ])LIST");

    EXPECT_EQ(2u, twoMatchesList.size());
    EXPECT_EQ(CreateMatch("intentai-111"), twoMatchesList.at(CONN_1));
    EXPECT_EQ(CreateMatch("intentai-222"), twoMatchesList.at(CONN_2));
}

TEST(NIntentaiResponseParser, OneMatchOneNot) {
    const auto& matchesList = NIntentaiResponseParser::Parse(R"LIST([
            {"ip": "11.11.11.11", "port": 1111, "timestamp": 1611111111, "user_id": "intentai-111"},
            {"ip": "22.22.22.22", "port": 2222, "timestamp": 1622222222, "user_id": None}
    ])LIST");

    EXPECT_EQ(1u, matchesList.size());
    EXPECT_EQ(CreateMatch("intentai-111"), matchesList.at(CONN_1));
}
