#include <gtest/gtest.h>
#include "../../src/processor/http_client.hpp"

namespace furita {

TEST(HttpClientTest, headersToString_singleHeader) {
    const ymod_httpclient::headers_dict headers{{"key1", "value1"}};
    EXPECT_EQ("key1: value1\n", headersToString(headers));
}

TEST(HttpClientTest, headersToString_manyHeaders) {
    const ymod_httpclient::headers_dict headers{{"key1", "value1"}, {"key2", "value2"}};
    EXPECT_EQ("key1: value1\nkey2: value2\n", headersToString(headers));
}

TEST(HttpClientTest, headersToString_empty) {
    const ymod_httpclient::headers_dict headers;
    EXPECT_EQ("", headersToString(headers));
}

TEST(HttpClientTest, headersToString_repeatedHeader) {
    const ymod_httpclient::headers_dict headers{{"key1", "value1"}, {"key1", "value2"}};
    EXPECT_EQ("key1: value1\nkey1: value2\n", headersToString(headers));
}

} // namespace furita
