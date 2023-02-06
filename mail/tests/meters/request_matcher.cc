#include <gtest/gtest.h>

#include <mail/unistat/cpp/include/meters/request_matcher.h>

using namespace ::testing;
using namespace ::unistat;

struct Case {
    std::string_view input;
    std::string_view expectation;
};

struct PathMatcherTests_PositiveCases : Test, WithParamInterface<Case> {};

TEST_P(PathMatcherTests_PositiveCases, testGetPath) {
    PathMatcher pm("path");
    const auto [input, expectation] = GetParam();
    ASSERT_EQ(pm.getPath(input), expectation);
}

// clang-format off
INSTANTIATE_TEST_SUITE_P(, PathMatcherTests_PositiveCases,
                         Values(Case{"path", "path"}, Case{"/path", "path"}, Case{"path/", "path"}, Case{"/path/", "path"}, Case{"//path//", "path"},
                                Case{"v2/path", "v2/path"}, Case{"/v2/path", "v2/path"}, Case{"v2/path/", "v2/path"}, Case{"/v2/path/", "v2/path"}, Case{"//v2/path//", "v2/path"},
                                Case{"path?", "path"}, Case{"path/?", "path"}, Case{"//path?a=b", "path"}, Case{"path//?", "path"},
                                Case{"path?a=b", "path"}, Case{"//path?a=b&c=d", "path"}, Case{"//path/?a=b&c=d", "path"}, Case{"//path//?a=b&c=d", "path"}
                         )
);
// clang-format on

struct PathMatcherTests_ExceptionalCases : Test, WithParamInterface<std::string_view> {};

TEST_P(PathMatcherTests_ExceptionalCases, testGetPathThrowsOnInvalidRequest) {
    PathMatcher pm("path");
    EXPECT_THROW(pm.getPath(GetParam()), std::invalid_argument);
}

INSTANTIATE_TEST_SUITE_P(, PathMatcherTests_ExceptionalCases,
                         Values("", "/", "//", "/?/?/?", "//??", "?", "/?", "?/", "?path/"));
