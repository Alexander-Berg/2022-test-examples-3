#include <butil/http/parse.h>
#include <gtest/gtest.h>
#include <gmock/gmock.h>

using namespace testing;

namespace  {

TEST(ParseWwwFormUrlEncode, shouldParseSuccessfully) {
    auto result = http::parse("a=1&b=2");

    EXPECT_THAT(result["a"], UnorderedElementsAre("1"));
    EXPECT_THAT(result["b"], UnorderedElementsAre("2"));
    EXPECT_EQ(result.size(), 2ul);
}

TEST(ParseWwwFormUrlEncode, shouldParseArgumentsWithSameName) {
    auto result = http::parse("a=1&a=2");

    EXPECT_THAT(result["a"], UnorderedElementsAre("1", "2"));
    EXPECT_EQ(result.size(), 1ul);
}

TEST(ParseWwwFormUrlEncode, shouldParseUrlEncodedArgsSuccessfully) {
    std::ostringstream out;
    out << "%D0%9F%D0%B0%D1%80%D0%B0%D0%BC%D0%B5%D1%82%D1%80"
        << "="
        << "%D0%97%D0%BD%D0%B0%D1%87%D0%B5%D0%BD%D0%B8%D0%B5";

    auto result = http::parse(out.str());

    EXPECT_THAT(result["Параметр"], UnorderedElementsAre("Значение"));
    EXPECT_EQ(result.size(), 1ul);
}

TEST(ParseWwwFormUrlEncode, shouldThrowAnExceptionWithMisformattedString) {
    EXPECT_THROW(http::parse("a=b=c"), std::runtime_error);
}

}
