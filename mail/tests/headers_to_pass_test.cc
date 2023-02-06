#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <internal/server/headers_to_pass.h>
#include <optional>

namespace http {
inline bool operator==(const headers& lhs, const headers& rhs) {
    return lhs.flatten() == rhs.flatten();
}

inline std::ostream& operator<<(std::ostream& stream, const headers& hdr) {
    stream << hdr.format();
    return stream;
}
}

namespace {

using namespace ::testing;
using msg_body::getHeadersToPass;

struct Config {
    std::vector<std::string> headersToPass;
};

struct RequestMock {
    MOCK_METHOD(std::optional<std::string>, getOptionalHeader, (const std::string&), (const));
};

struct HeadersToPassTest : public Test {
    const Config config {{"hdr1", "hdr2"}};
    StrictMock<RequestMock> request;
};

TEST_F(HeadersToPassTest, tryGetHeadersFromRequest_notFound_returnsEmpty) {
    http::headers expected;

    EXPECT_CALL(request, getOptionalHeader("hdr1")).WillOnce(Return(std::nullopt));
    EXPECT_CALL(request, getOptionalHeader("hdr2")).WillOnce(Return(std::nullopt));

    http::headers result = getHeadersToPass(config, request);

    ASSERT_EQ(result, expected);
}

TEST_F(HeadersToPassTest, tryGetHeadersFromRequest_returnsOnlyFound) {
    http::headers expected;
    expected.add("hdr1", "val1");

    EXPECT_CALL(request, getOptionalHeader("hdr1")).WillOnce(Return(std::optional<std::string>("val1")));
    EXPECT_CALL(request, getOptionalHeader("hdr2")).WillOnce(Return(std::nullopt));

    http::headers result = getHeadersToPass(config, request);

    ASSERT_EQ(result, expected);
}

TEST_F(HeadersToPassTest, tryGetHeadersFromRequest_returnsAllFound) {
    http::headers expected;
    expected.add("hdr1", "val1");
    expected.add("hdr2", "val2");

    EXPECT_CALL(request, getOptionalHeader("hdr1")).WillOnce(Return(std::optional<std::string>("val1")));
    EXPECT_CALL(request, getOptionalHeader("hdr2")).WillOnce(Return(std::optional<std::string>("val2")));

    http::headers result = getHeadersToPass(config, request);

    ASSERT_EQ(result, expected);
}

} // namespace
