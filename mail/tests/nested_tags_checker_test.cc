#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <internal/nested_tags_checker.h>
#include <internal/config.h>

using namespace testing;

namespace msg_body {

std::ostream& operator<< (std::ostream& stream, const NestedTagsCheckerResult checkerResult) {
    switch (checkerResult) {
        case NestedTagsCheckerResult::Ok:
            stream << "Ok";
            break;
        case NestedTagsCheckerResult::ParseError:
            stream << "ParseError";
            break;
        case NestedTagsCheckerResult::TagsDepthLimitExceeded:
            stream << "TagsDepthLimitExceeded";
            break;
    }
    return stream;
}

static LogPtr dummyLogger = std::make_shared<ContextLogger>(makeLoggerWithRequestId(""));

struct NestedTagsCheckerTest : Test {
    Configuration config;

    NestedTagsChecker createChecker(const unsigned divTagDepthLimit) {
        config.divTagDepthLimit = divTagDepthLimit;

        return NestedTagsChecker(config, dummyLogger);
    }
};

TEST_F(NestedTagsCheckerTest, ContentCheck_tagsLimitDidntExceeded) {
    const std::string content =
        "<div><div><div><div><div>"
        "<p>some data</p>"
        "</div></div></div></div></div>";

    auto checker = createChecker(10u);
    EXPECT_EQ(checker.check(content), NestedTagsCheckerResult::Ok);
}

TEST_F(NestedTagsCheckerTest, ContentCheck_tagsLimitExceeded) {
    const std::string content =
        "<div><div><div><div><div><div>"
        "<p>some data</p>"
        "</div></div></div></div></div></div>";

    auto checker = createChecker(5u);
    EXPECT_EQ(checker.check(content), NestedTagsCheckerResult::TagsDepthLimitExceeded);
}

TEST_F(NestedTagsCheckerTest, ContentCheck_tagsLimitExceeded_noClosingTags) {
    const std::string content =
        "<div><div><div><div><div><div>"
        "<p>some data</p>";

    auto checker = createChecker(5u);
    EXPECT_EQ(checker.check(content), NestedTagsCheckerResult::TagsDepthLimitExceeded);
}

TEST_F(NestedTagsCheckerTest, ContentCheck_invalidHtml) {
    const std::string content =
        "<div<div<div><div>div><div>"
        "<p>some data</p>";

    auto checker = createChecker(10u);
    EXPECT_EQ(checker.check(content), NestedTagsCheckerResult::ParseError);
}

TEST_F(NestedTagsCheckerTest, ContentCheck_emptyContent) {
    auto checker = createChecker(1u);
    EXPECT_EQ(checker.check(""), NestedTagsCheckerResult::Ok);
}

TEST_F(NestedTagsCheckerTest, ContentCheck_withoutDivTag) {
    const std::string content =
        "<tr><tr><tr>"
        "<p>some data</p>"
        "</tr></tr></tr>";

    auto checker = createChecker(1u);
    EXPECT_EQ(checker.check(content), NestedTagsCheckerResult::Ok);
}

}
