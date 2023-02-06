#include <web/util/get_header.h>

#include <gtest/gtest.h>

namespace {

using NMdb::NWeb::GetHeaderOptional;

TEST(TTestGetHeaderOptional, for_header_missing_must_return_empty_optional) {
    const auto header = std::string{"header"};
    const auto headerValue = std::string{"value"};
    const auto requestedHeader = std::string{"anotherHeader"};
    EXPECT_FALSE(GetHeaderOptional({{header, headerValue}}, requestedHeader));
}

TEST(TTestGetHeaderOptional, for_header_present_must_return_header) {
    const auto header = std::string{"header"};
    const auto headerValue = std::string{"value"};
    const auto result = GetHeaderOptional({{header, headerValue}}, header);
    EXPECT_TRUE(result);
    EXPECT_EQ(headerValue, *result);
}

}
