#include <crypta/ext_fp/matcher/lib/matchers/beeline_matcher/beeline_response_parser.h>

#include <crypta/ext_fp/matcher/lib/matchers/beeline_matcher/beeline_match.pb.h>

#include <library/cpp/testing/gtest/gtest.h>

using namespace NCrypta::NExtFp::NMatcher;

namespace {
    TMatchResult CreateMatch(const TString& id) {
        return TMatchResult{.Status = TMatchResult::EStatus::Found, .ExtId = id};
    }

    const TConnection CONN_1 = {.Ip = "11.11.11.11", .Port = 1111, .Timestamp = 1611111111u};
    const TConnection CONN_2 = {.Ip = "22.22.22.22", .Port = 2222, .Timestamp = 1622222222u};
}

TEST(NBeelineResponseParser, ParseEmpty) {
    EXPECT_EQ(0u, NBeelineResponseParser::Parse("").size());
    EXPECT_EQ(0u, NBeelineResponseParser::Parse("[]").size());
    EXPECT_EQ(0u, NBeelineResponseParser::Parse("{}").size());
}

TEST(NBeelineResponseParser, Invalid) {
    EXPECT_THROW(NBeelineResponseParser::Parse("[1, 2, 3]"), yexception);
    EXPECT_THROW(NBeelineResponseParser::Parse("[{}]"), yexception);
}

TEST(NBeelineResponseParser, OneMatch) {
    const auto& oneMatchList = NBeelineResponseParser::Parse(R"LIST([
            {"ip": "11.11.11.11", "port": 1111, "unixtime": 1611111111, "id": "beeline-111"}
    ])LIST");

    EXPECT_EQ(1u, oneMatchList.size());
    EXPECT_EQ(CreateMatch("beeline-111"), oneMatchList.at(CONN_1));
}

TEST(NBeelineResponseParser, TwoMatches) {
    const auto& twoMatchesList = NBeelineResponseParser::Parse(R"LIST([
            {"ip": "11.11.11.11", "port": 1111, "unixtime": 1611111111, "id": "beeline-111"},
            {"ip": "22.22.22.22", "port": 2222, "unixtime": 1622222222, "id": "beeline-222"}
    ])LIST");

    EXPECT_EQ(2u, twoMatchesList.size());
    EXPECT_EQ(CreateMatch("beeline-111"), twoMatchesList.at(CONN_1));
    EXPECT_EQ(CreateMatch("beeline-222"), twoMatchesList.at(CONN_2));
}
