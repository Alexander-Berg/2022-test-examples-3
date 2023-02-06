#include <crypta/ext_fp/matcher/lib/matchers/mts_matcher/mts_response_parser.h>

#include <crypta/ext_fp/matcher/lib/matchers/mts_matcher/mts_match.pb.h>

#include <library/cpp/testing/gtest/gtest.h>

using namespace NCrypta::NExtFp::NMatcher;

namespace {
    TMatchResult CreateMatch(const TString& id) {
        return TMatchResult{.Status = TMatchResult::EStatus::Found, .ExtId = id};
    }

    const TConnection CONN_1 = {.Ip = "11.11.11.11", .Port = 1111, .Timestamp = 1611111111u};
    const TConnection CONN_2 = {.Ip = "22.22.22.22", .Port = 2222, .Timestamp = 1622222222u};
}

TEST(NMtsResponseParser, ParseEmpty) {
    EXPECT_EQ(0u, NMtsResponseParser::Parse("").size());
    EXPECT_EQ(0u, NMtsResponseParser::Parse("[]").size());
    EXPECT_EQ(0u, NMtsResponseParser::Parse("{}").size());
}

TEST(NMtsResponseParser, Invalid) {
    EXPECT_THROW(NMtsResponseParser::Parse("[1, 2, 3]"), yexception);
    EXPECT_THROW(NMtsResponseParser::Parse("[{}]"), yexception);
}

TEST(NMtsResponseParser, OneMatch) {
    const auto& oneMatchList = NMtsResponseParser::Parse(R"LIST([
            {"ip": "11.11.11.11", "port": 1111, "ts": 1611111111000, "id": "mts-111"}
    ])LIST");

    EXPECT_EQ(1u, oneMatchList.size());
    EXPECT_EQ(CreateMatch("mts-111"), oneMatchList.at(CONN_1));
}

TEST(NMtsResponseParser, TwoMatches) {
    const auto& twoMatchesList = NMtsResponseParser::Parse(R"LIST([
            {"ip": "11.11.11.11", "port": 1111, "ts": 1611111111000, "id": "mts-111"},
            {"ip": "22.22.22.22", "port": 2222, "ts": 1622222222000, "id": "mts-222"}
    ])LIST");

    EXPECT_EQ(2u, twoMatchesList.size());
    EXPECT_EQ(CreateMatch("mts-111"), twoMatchesList.at(CONN_1));
    EXPECT_EQ(CreateMatch("mts-222"), twoMatchesList.at(CONN_2));
}
